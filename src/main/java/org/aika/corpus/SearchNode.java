/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aika.corpus;


import org.aika.Activation;
import org.aika.Activation.Rounds;
import org.aika.Activation.SynapseActivation;
import org.aika.corpus.Conflicts.Conflict;
import org.aika.neuron.Neuron.NormWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The {@code SearchNode} class represents a node in the binary search tree that is used to find the optimal
 * interpretation for a given document. Each search node possess a refinement (simply a set of interpretation nodes).
 * The two options that this search node examines are that the refinement will either part of the final interpretation or not.
 * During each search step the activation values in all the neuron activations adjusted such that they reflect the interpretation of the current search path.
 * When the search reaches the maximum depth of the search tree and no further refinements exists, a weight is computed evaluating the current search path.
 * The search path with the highest weight is used to determine the final interpretation.
 *
 * <p> Before the search is started a set of initial refinements is generated from the conflicts within the document.
 * In other words, if there are no conflicts in a given document, then no search is needed. In this case the final interpretation
 * will simply be the set of all interpretation nodes. The initial refinements are then expanded, meaning all interpretation nodes that are consistent
 * with this refinement are added to the refinement. The initial refinements are then propagated along the search path as refinement candidates.
 *
 * @author Lukas Molzberger
 */
public class SearchNode implements Comparable<SearchNode> {

    private static final Logger log = LoggerFactory.getLogger(SearchNode.class);

    /**
     * This optimization may miss some cases and will not always return the best interpretation.
     */
    public static boolean INCOMPLETE_OPTIMIZATION = true;

    public static int MAX_SEARCH_STEPS = 100000;

    public int id;

    SearchNode excludedParent;
    SearchNode selectedParent;
    SearchNode parent;

    long visited;
    List<InterprNode> refinement;
    RefMarker marker;

    NormWeight weightDelta = NormWeight.ZERO_WEIGHT;

    public List<StateChange> modifiedActs = new ArrayList<>();

    public TreeSet<SearchNode> candidates = new TreeSet<>();


    public SearchNode(SearchNode parent) {
        this.parent = parent;
    }


    private void collectResults(Collection<InterprNode> results) {
        results.addAll(refinement);
        if(selectedParent != null) selectedParent.collectResults(results);
    }


    private void markSelected(long v) {
        if(marker != null) {
            marker.markedSelected = v;
        }
        if(selectedParent != null) selectedParent.markSelected(v);
    }


    public static SearchNode createInitialExpandNode(Document doc) {
        List<InterprNode> changed = new ArrayList<>();
        changed.add(doc.bottom);
        return createCandidate(doc, changed, null, null, null, Arrays.asList(doc.bottom), null);
    }


    public void computeSelectedOption(Document doc) {
        ArrayList<InterprNode> results = new ArrayList<>();
        results.add(doc.bottom);

        doc.selectedSearchNode = null;
        int[] searchSteps = new int[1];

        List<InterprNode> rootRefs = expandRootRefinement(doc);
        refinement = expandRefinement(rootRefs, InterprNode.visitedCounter++);

        markCovered(null, visited, refinement);
        markExcluded(null, visited, refinement);

        weightDelta = doc.vQueue.adjustWeight(this, rootRefs);

        if(Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("Root ExpandNode:" + toString());
        }

        doc.interrupted = false;
        doc.selectedSearchNode = doc.root;
        doc.selectedMark = InterprNode.visitedCounter++;
        markSelected(doc.selectedMark);

        doc.bottom.storeFinalWeight(InterprNode.visitedCounter++);


        generateInitialCandidates(doc);

        SearchNode child = doc.root.selectCandidate();

        if(child != null) {
            child.search(doc, doc.root, null, searchSteps);
        }

        if (doc.selectedSearchNode != null) {
            doc.selectedSearchNode.reconstructSelectedResult(doc);
            doc.selectedSearchNode.collectResults(results);

            log.info("Selected SearchNode ID: " + doc.selectedSearchNode.id);
        }

        doc.selectedInterprNode = results;

        if(doc.interrupted) {
            log.warn("The search for the best interpretation has been interrupted. Too many search steps!");
        }
    }


    private void reconstructSelectedResult(Document doc) {
        if(selectedParent != null) selectedParent.reconstructSelectedResult(doc);

        changeState(StateChange.Mode.NEW);

        for(StateChange sc : modifiedActs) {
            Activation act = sc.act;
            if(act.finalState != null && act.finalState.value > 0.0) {
                doc.finallyActivatedNeurons.add(act.key.n.neuron);
            }
        }
    }


    private void search(Document doc, SearchNode selectedParent, SearchNode excludedParent, int[] searchSteps) {
        if(searchSteps[0] > MAX_SEARCH_STEPS) {
            doc.interrupted = true;
        }
        searchSteps[0]++;

        markCovered(null, visited, refinement);
        markExcluded(null, visited, refinement);

        if(Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("Search Step: " + id);
            log.info(toString());
        }

        boolean f = doc.selectedMark == -1 || marker.markedSelected != doc.selectedMark;

        changeState(StateChange.Mode.NEW);

        if(Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info(doc.networkStateToString(true, true) + "\n");
        }

        double accNW = computeAccumulatedWeight().getNormWeight();
        double selectedAccNW = doc.selectedSearchNode != null ? doc.selectedSearchNode.computeAccumulatedWeight().getNormWeight() : 0.0;

        generateNextLevelCandidates(doc, selectedParent, excludedParent);

        if(candidates.size() == 0) {
            SearchNode en = this;
            while(en != null) {
                if(en.marker != null && !en.marker.complete) {
                    en.marker.complete = !hasUnsatisfiedPositiveFeedbackLink(en.refinement);
                }
                en = en.selectedParent;
            }

            if (accNW > selectedAccNW) {
                doc.selectedSearchNode = this;
                doc.selectedMark = InterprNode.visitedCounter++;
                markSelected(doc.selectedMark);

                doc.bottom.storeFinalWeight(InterprNode.visitedCounter++);
            }
        }

        SearchNode child = selectCandidate();
        if(child != null) {
            child.search(doc, this, excludedParent, searchSteps);
        }
        changeState(StateChange.Mode.OLD);

        if(doc.interrupted) {
            return;
        }

        do {
            child = selectedParent.selectCandidate();
        } while(child != null && !f && INCOMPLETE_OPTIMIZATION && child.marker.complete);

        if(child != null) {
            child.search(doc, selectedParent, this, searchSteps);
        }
    }


    private boolean hasUnsatisfiedPositiveFeedbackLink(List<InterprNode> n) {
        for(InterprNode x: n) {
            if(hasUnsatisfiedPositiveFeedbackLink(x)) return true;
        }
        return false;
    }


    private boolean hasUnsatisfiedPositiveFeedbackLink(InterprNode n) {
        if(n.hasUnsatisfiedPosFeedbackLinksCache != null) return n.hasUnsatisfiedPosFeedbackLinksCache;

        for(Activation act: n.getNeuronActivations()) {
            for(SynapseActivation sa: act.neuronOutputs) {
                if(sa.s.key.isRecurrent && sa.s.w > 0.0 && !isCovered(sa.output.key.o.markedCovered)) {
                    n.hasUnsatisfiedPosFeedbackLinksCache = true;
                    return true;
                }
            }
        }

        for(InterprNode pn: n.parents) {
            if(hasUnsatisfiedPositiveFeedbackLink(pn)) {
                n.hasUnsatisfiedPosFeedbackLinksCache = true;
                return true;
            }
        }

        n.hasUnsatisfiedPosFeedbackLinksCache = false;
        return false;
    }


    private SearchNode selectCandidate() {
        if(candidates.isEmpty()) return null;
        return candidates.pollFirst();
    }


    public void generateInitialCandidates(Document doc) {
        candidates = new TreeSet<>();
        for(InterprNode cn: collectConflicts(doc)) {
            List<InterprNode> changed = new ArrayList<>();
            SearchNode c = createCandidate(doc, changed, this, this, null, Arrays.asList(cn), new RefMarker());

            c.weightDelta = doc.vQueue.adjustWeight(c, changed);
            if(Document.OPTIMIZE_DEBUG_OUTPUT) {
                log.info("Search Step: " + c.id + "  Candidate Weight Delta: " + c.weightDelta);
                log.info(doc.networkStateToString(true, true) + "\n");
            }

            c.changeState(StateChange.Mode.OLD);

            candidates.add(c);
        }
    }


    public void generateNextLevelCandidates(Document doc, SearchNode selectedParent, SearchNode excludedParent) {
        candidates = new TreeSet<>();

        for(SearchNode pc: selectedParent.candidates) {
            if(!checkCovered(pc.refinement) && !checkExcluded(pc.refinement, InterprNode.visitedCounter++)) {
                List<InterprNode> changed = new ArrayList<>();
                SearchNode c = createCandidate(doc, changed, this, this, excludedParent, pc.refinement, pc.marker);

                c.weightDelta = doc.vQueue.adjustWeight(c, changed);
                c.changeState(StateChange.Mode.OLD);

                candidates.add(c);
            }
        }
    }


    private boolean checkCovered(List<InterprNode> n) {
        for(InterprNode x: n) {
            if(!isCovered(x.markedCovered)) return false;
        }
        return true;
    }


    private boolean checkExcluded(List<InterprNode> n, long v) {
        for(InterprNode x: n) {
            if(checkExcluded(x, v)) return true;
        }
        return false;
    }


    private boolean checkExcluded(InterprNode ref, long v) {
        if(ref.visitedCheckExcluded == v) return false;
        ref.visitedCheckExcluded = v;

        if(isCovered(ref.markedExcluded)) return true;

        for(InterprNode pn: ref.parents) {
            if(checkExcluded(pn, v)) return true;
        }

        return false;
    }


    public static List<InterprNode> collectConflicts(Document doc) {
        List<InterprNode> results = new ArrayList<>();
        long v = InterprNode.visitedCounter++;
        for(InterprNode n: doc.bottom.children) {
            for(Conflict c: n.conflicts.primary.values()) {
                if(c.secondary.visitedCollectConflicts != v) {
                    c.secondary.visitedCollectConflicts = v;
                    results.add(c.secondary);
                }
            }
        }
        return results;
    }


    private static List<InterprNode> expandRootRefinement(Document doc) {
        ArrayList<InterprNode> tmp = new ArrayList<>();
        tmp.add(doc.bottom);
        for(InterprNode pn: doc.bottom.children) {
            if((pn.orInterprNodes == null || pn.orInterprNodes.isEmpty()) && pn.conflicts.primary.isEmpty() && pn.conflicts.secondary.isEmpty()) {
                tmp.add(pn);
            }
        }
        return tmp;
    }


    private List<InterprNode> expandRefinement(List<InterprNode> ref, long v) {
        ArrayList<InterprNode> tmp = new ArrayList<>();
        for(InterprNode n: ref) {
            markExpandRefinement(n, v);
            tmp.add(n);
        }

        for(InterprNode n: ref) {
            expandRefinementRecursiveStep(tmp, n, v);
        }

        if(ref.size() == tmp.size()) return tmp;
        else return expandRefinement(tmp, v);
    }


    private void markExpandRefinement(InterprNode n, long v) {
        if(n.markedExpandRefinement == v) return;
        n.markedExpandRefinement = v;

        for(InterprNode pn: n.parents) {
            markExpandRefinement(pn, v);
        }
    }


    private boolean hasUncoveredConflicts(InterprNode n) {
        if(!n.conflicts.hasConflicts()) return false;

        ArrayList<InterprNode> conflicts = new ArrayList<>();
        Conflicts.collectDirectConflicting(conflicts, n);
        for(InterprNode cn: conflicts) {
            if(!isCovered(cn.markedExcluded)) return true;
        }
        return false;
    }


    private void expandRefinementRecursiveStep(Collection<InterprNode> results, InterprNode n, long v) {
        if(n.visitedExpandRefinementRecursiveStep == v) return;
        n.visitedExpandRefinementRecursiveStep = v;

        if (n.refByOrInterprNode != null) {
            for (InterprNode on : n.refByOrInterprNode) {
                if(on.markedExpandRefinement != v && !hasUncoveredConflicts(on) && !isCovered(on.markedCovered)) {
                    markExpandRefinement(on, v);
                    results.add(on);
                }
            }
        }
        for(InterprNode pn: n.parents) {
            if(!pn.isBottom()) {
                expandRefinementRecursiveStep(results, pn, v);
            }
        }

        if(n.isBottom()) return;

        // Expand options that are partially covered by this refinement and partially by an earlier expand node.
        for(InterprNode cn: n.children) {
            if(cn.visitedExpandRefinementRecursiveStep == v) break;

            // Check if all parents are either contained in this refinement or an earlier refinement.
            boolean covered = true;
            for(InterprNode cnp: cn.parents) {
                if(cnp.visitedExpandRefinementRecursiveStep != v && !isCovered(cnp.markedCovered)) {
                    covered = false;
                    break;
                }
            }

            if(covered) {
                expandRefinementRecursiveStep(results, cn, v);
            }
        }
    }


    public NormWeight computeAccumulatedWeight() {
        return (selectedParent != null ? weightDelta.add(selectedParent.computeAccumulatedWeight()) : weightDelta);
    }


    public boolean isCovered(long g) {
        return g == visited || (selectedParent != null && g < visited && selectedParent.isCovered(g));
    }


    private void markCovered(List<InterprNode> changed, long v, List<InterprNode> n) {
        for(InterprNode x: n) {
            markCovered(changed, v, x);
        }
    }


    private boolean markCovered(List<InterprNode> changed, long v, InterprNode n) {
        if(n.visitedMarkCovered == v) return false;
        n.visitedMarkCovered = v;

        if(isCovered(n.markedCovered)) return false;

        n.markedCovered = v;

        if(n.isBottom()) {
            return false;
        }

        for(InterprNode p: n.parents) {
            if(markCovered(changed, v, p)) return true;
        }

        for(InterprNode c: n.children) {
            if(c.visitedMarkCovered == v) continue;

            if(!containedInSelectedBranch(v, c)) continue;

            if(c.isConflicting(InterprNode.visitedCounter++)) return true;

            c.markedCovered = v;

            if(markCovered(changed, v, c)) return true;
        }

        if(changed != null) changed.add(n);

        return false;
    }


    private void markExcluded(List<InterprNode> changed, long v, List<InterprNode> n) {
        for(InterprNode x: n) {
            markExcluded(changed, v, x);
        }
    }


    private void markExcluded(List<InterprNode> changed, long v, InterprNode n) {
        List<InterprNode> conflicting = new ArrayList<>();
        Conflicts.collectAllConflicting(conflicting, n, InterprNode.visitedCounter++);
        for(InterprNode cn: conflicting) {
            markExcludedRecursiveStep(changed, v, cn);
        }
    }


    private void markExcludedRecursiveStep(List<InterprNode> changed, long v, InterprNode n) {
        if(n.markedExcluded == v || isCovered(n.markedExcluded)) return;
        n.markedExcluded = v;

        for(InterprNode c: n.children) {
            markExcludedRecursiveStep(changed, v, c);
        }

        // If the or option has two input options and one of them is already excluded, then when the other one is excluded we also have to exclude the or option.
        if(n.linkedByLCS != null) {
            for(InterprNode c: n.linkedByLCS) {
                markExcludedRecursiveStep(changed, v, c);
            }
        }

        if(changed != null) changed.add(n);

        return;
    }


    public boolean containedInSelectedBranch(long v, InterprNode n) {
        for(InterprNode p: n.parents) {
            if(p.markedCovered != v && !isCovered(p.markedCovered)) return false;
        }
        return true;
    }


    public String toString() {
        return refinement.toString() + " : " + computeAccumulatedWeight();
    }


    public static SearchNode createCandidate(Document doc, List<InterprNode> changed, SearchNode parent, SearchNode selectedParent, SearchNode excludedParent, List<InterprNode> ref, RefMarker marker) {
        SearchNode cand = new SearchNode(parent);
        cand.id = doc.searchNodeIdCounter++;
        cand.visited = InterprNode.visitedCounter++;
        cand.selectedParent = selectedParent;
        cand.excludedParent = excludedParent;
        cand.refinement = cand.expandRefinement(ref, InterprNode.visitedCounter++);
        cand.markCovered(changed, cand.visited, cand.refinement);
        cand.markExcluded(changed, cand.visited, cand.refinement);
        cand.marker = marker;

        if(Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("\n \n Generate Candidate: " + cand.refinement.toString());
        }

        return cand;
    }


    public void changeState(StateChange.Mode m) {
        for(StateChange sc: modifiedActs) {
            sc.restoreState(m);
        }
    }


    @Override
    public int compareTo(SearchNode c) {
        int r = Double.compare(c.computeAccumulatedWeight().getNormWeight(), computeAccumulatedWeight().getNormWeight());
        if(r != 0) return r;
        return Integer.compare(id, c.id);
    }


    /**
     * The <code>StateChange</code> class is used to store the state change of an activation that occurs in each node of
     * the binary search tree. When a candidate refinement is selected during the search, then the activation values of
     * all affected activation objects are adjusted. The changes to the activation values are also propagated through
     * the network. The old state needs to be stored here in order for the search to be able to restore the old network
     * state before following the alternative search branch.
     *
     */
    public static class StateChange {
        public Activation act;

        public Rounds oldRounds;
        public Rounds newRounds;

        public enum Mode { OLD, NEW }

        public static void saveOldState(List<StateChange> changes, Activation act, long v) {
            StateChange sc = act.currentStateChange;
            if(sc == null || act.currentStateV != v) {
                sc = new StateChange();
                sc.oldRounds = act.rounds.copy();
                act.currentStateChange = sc;
                act.currentStateV = v;
                sc.act = act;
                if(changes != null) {
                    changes.add(sc);
                }
            }
        }

        public static void saveNewState(Activation act) {
            StateChange sc = act.currentStateChange;

            sc.newRounds = act.rounds;
        }

        public void restoreState(Mode m) {
            act.rounds = (m == Mode.OLD ? oldRounds : newRounds).copy();
        }
    }


    private static class RefMarker {
        public long markedSelected;
        public boolean complete;
    }
}
