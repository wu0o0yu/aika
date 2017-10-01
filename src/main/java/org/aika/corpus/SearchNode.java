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


import org.aika.Utils;
import org.aika.neuron.Activation.Rounds;
import org.aika.neuron.Activation.SynapseActivation;
import org.aika.neuron.Activation;
import org.aika.corpus.Conflicts.Conflict;
import org.aika.neuron.INeuron.NormWeight;
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

    public static int MAX_SEARCH_STEPS = 100000;

    public int id;

    public SearchNode excludedParent;
    public SearchNode selectedParent;

    public int visited;
    List<InterprNode> refinement;
    Candidate candidate;
    int level;

    NormWeight[] weightDelta = new NormWeight[] {NormWeight.ZERO_WEIGHT, NormWeight.ZERO_WEIGHT};
    NormWeight[] accumulatedWeight = new NormWeight[2];

    public List<StateChange> modifiedActs = new ArrayList<>();


    public enum Coverage {
        SELECTED,
        UNKNOWN,
        EXCLUDED
    }


    public SearchNode(Document doc, SearchNode selParent, SearchNode exclParent, Candidate c, int level) {
        id = doc.searchNodeIdCounter++;
        this.level = level;
        visited = doc.visitedCounter++;
        selectedParent = selParent;
        excludedParent = exclParent;

        refinement = expandRefinement(Collections.singletonList(c != null ? c.refinement : doc.bottom), doc.visitedCounter++);;
        candidate = c;

        if(Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("Search Step: " + id + "  Candidate Weight Delta: " + weightDelta);
            log.info(doc.neuronActivationsToString(true, true, false) + "\n");
        }
    }


    private void collectResults(Collection<InterprNode> results) {
        results.addAll(refinement);
        if(selectedParent != null) selectedParent.collectResults(results);
    }


    public void computeBestInterpretation(Document doc) {
        ArrayList<InterprNode> results = new ArrayList<>();
        results.add(doc.bottom);

        doc.selectedSearchNode = null;
        int[] searchSteps = new int[1];

        List<InterprNode> rootRefs = expandRootRefinement(doc);
        refinement = expandRefinement(rootRefs, doc.visitedCounter++);

        markSelected(null, refinement);
        markExcluded(null, refinement);

        weightDelta = doc.vQueue.adjustWeight(this, rootRefs);
        accumulatedWeight = weightDelta;

        if(Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("Root SearchNode:" + toString());
        }

        doc.bottom.storeFinalWeight(doc.visitedCounter++);


        Candidate[] candidates = generateCandidates(doc);
        if(candidates.length > 0) {
            Candidate c = candidates[level + 1];

            if (c != null) {
                SearchNode child = new SearchNode(doc, this, null, c, level + 1);
                child.search(doc, searchSteps, candidates);
            }
        }

        if (doc.selectedSearchNode != null) {
            doc.selectedSearchNode.reconstructSelectedResult(doc);
            doc.selectedSearchNode.collectResults(results);
        }

        doc.bestInterpretation = results;

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
                doc.finallyActivatedNeurons.add(act.key.n.neuron.get());
            }
        }
    }


    private double search(Document doc, int[] searchSteps, Candidate[] candidates) {
        double selectedWeight = 0.0;
        double excludedWeight = 0.0;

        boolean alreadySelected = checkSelected(refinement);
        boolean alreadyExcluded = checkExcluded(refinement, doc.visitedCounter++);

        if(searchSteps[0] > MAX_SEARCH_STEPS) {
            doc.interrupted = true;
        }
        searchSteps[0]++;

        if(Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("Search Step: " + id);
            log.info(toString());
        }

        if(Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info(doc.neuronActivationsToString(true, true, false) + "\n");
        }

        if(selectedParent != null) {
            for (int i = 0; i < 2; i++) {
                accumulatedWeight[i] = selectedParent.accumulatedWeight[i];
            }
        }

        Boolean cd = !alreadyExcluded && !alreadySelected ? getCachedDecision() : null;

        if (!alreadyExcluded) {
            List<InterprNode> changed = new ArrayList<>();
            if(level == -1) {
                changed.add(doc.bottom);
            }

            markSelected(changed, refinement);
            markExcluded(changed, refinement);

            weightDelta = doc.vQueue.adjustWeight(this, changed);

            if(selectedParent != null) {
                for (int i = 0; i < 2; i++) {
                    accumulatedWeight[i] = weightDelta[i].add(accumulatedWeight[i]);
                }
            }

            if (candidates.length == level + 1) {
                selectedWeight = processResult(doc);
            } else {
                if (cd == null || cd) {
                    Candidate c = candidates[level + 1];
                    SearchNode child = new SearchNode(doc, this, excludedParent, c, level + 1);
                    selectedWeight = child.search(doc, searchSteps, candidates);
                }
            }
        }
        changeState(StateChange.Mode.OLD);
        if(doc.interrupted) {
            return 0.0;
        }

        if(!alreadySelected) {
            if (candidates.length == level + 1) {
                excludedWeight = processResult(doc);
            } else {
                if(cd == null || !cd) {
                    Candidate c = candidates[level + 1];
                    SearchNode child = new SearchNode(doc, selectedParent, this, c, level + 1);
                    excludedWeight = child.search(doc, searchSteps, candidates);
                }
            }
        }

        if(cd == null && !alreadyExcluded && !alreadySelected) {
            candidate.cache.put(this, selectedWeight >= excludedWeight);
        }
        return Math.max(selectedWeight, excludedWeight);
    }

    private double processResult(Document doc) {
        double accNW = accumulatedWeight[0].getNormWeight();
        double selectedAccNW = doc.selectedSearchNode != null ? doc.selectedSearchNode.accumulatedWeight[Activation.State.VALUE].getNormWeight() : 0.0;

        if (accNW > selectedAccNW) {
            doc.selectedSearchNode = this;
            doc.bottom.storeFinalWeight(doc.visitedCounter++);
        }

        return accNW;
    }


    public Candidate[] generateCandidates(Document doc) {
        TreeSet<Candidate> candidates = new TreeSet<>();
        int i = 0;
        for(InterprNode cn: collectConflicts(doc)) {
            candidates.add(new Candidate(cn, i++));
        }
        return candidates.toArray(new Candidate[candidates.size()]);
    }



    private boolean checkSelected(List<InterprNode> n) {
        for(InterprNode x: n) {
            if(!isCovered(x.markedSelected)) return false;
        }
        return true;
    }


    private boolean checkExcluded(List<InterprNode> n, int v) {
        for(InterprNode x: n) {
            if(checkExcluded(x, v)) return true;
        }
        return false;
    }


    private boolean checkExcluded(InterprNode ref, int v) {
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
        int v = doc.visitedCounter++;
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


    private List<InterprNode> expandRefinement(List<InterprNode> ref, int v) {
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


    private void markExpandRefinement(InterprNode n, int v) {
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


    private void expandRefinementRecursiveStep(Collection<InterprNode> results, InterprNode n, int v) {
        if(n.visitedExpandRefinementRecursiveStep == v) return;
        n.visitedExpandRefinementRecursiveStep = v;

        if (n.refByOrInterprNode != null) {
            for (InterprNode on : n.refByOrInterprNode) {
                if(on.markedExpandRefinement != v && !hasUncoveredConflicts(on) && !isCovered(on.markedSelected)) {
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
                if(cnp.visitedExpandRefinementRecursiveStep != v && !isCovered(cnp.markedSelected)) {
                    covered = false;
                    break;
                }
            }

            if(covered) {
                expandRefinementRecursiveStep(results, cn, v);
            }
        }
    }


    public Coverage getCoverage(InterprNode n) {
        if(isCovered(n.markedSelected)) return Coverage.SELECTED;
        if(isCovered(n.markedExcluded)) return Coverage.EXCLUDED;
        return Coverage.UNKNOWN;
    }


    public boolean isCovered(int g) {
        SearchNode n = this;
        do {
            if(g == n.visited) return true;
            else if(g > n.visited) return false;
            n = n.selectedParent;
        } while(n != null);
        return false;
    }


    private void markSelected(List<InterprNode> changed, List<InterprNode> n) {
        for(InterprNode x: n) {
            markSelected(changed, x);
        }
    }


    private boolean markSelected(List<InterprNode> changed, InterprNode n) {
        if(isCovered(n.markedSelected)) return false;

        n.markedSelected = visited;

        if(n.isBottom()) {
            return false;
        }

        for(InterprNode p: n.parents) {
            if(markSelected(changed, p)) return true;
        }

        for(InterprNode c: n.children) {
            if(isCovered(n.markedSelected) || !containedInSelectedBranch(c)) continue;

            if(c.isConflicting(n.doc.visitedCounter++)) return true;

            if(markSelected(changed, c)) return true;
        }

        if(changed != null) changed.add(n);

        return false;
    }


    private void markExcluded(List<InterprNode> changed, List<InterprNode> n) {
        for(InterprNode x: n) {
            markExcluded(changed, x);
        }
    }


    private void markExcluded(List<InterprNode> changed, InterprNode n) {
        List<InterprNode> conflicting = new ArrayList<>();
        Conflicts.collectAllConflicting(conflicting, n, n.doc.visitedCounter++);
        for(InterprNode cn: conflicting) {
            markExcludedRecursiveStep(changed, cn);
        }
    }


    private void markExcludedRecursiveStep(List<InterprNode> changed, InterprNode n) {
        if(isCovered(n.markedExcluded)) return;
        n.markedExcluded = visited;

        for(InterprNode c: n.children) {
            markExcludedRecursiveStep(changed, c);
        }

        // If the or option has two input options and one of them is already excluded, then when the other one is excluded we also have to exclude the or option.
        if(n.linkedByLCS != null) {
            for(InterprNode c: n.linkedByLCS) {
                if(checkOrNodeExcluded(c)) {
                    markExcludedRecursiveStep(changed, c);
                }
            }
        }

        if(changed != null) changed.add(n);

        return;
    }


    private boolean checkOrNodeExcluded(InterprNode n) {
        for(InterprNode on: n.orInterprNodes.values()) {
            if(!isCovered(on.markedExcluded)) {
                return false;
            }
        }
        return true;
    }


    public boolean containedInSelectedBranch(InterprNode n) {
        for(InterprNode p: n.parents) {
            if(!isCovered(p.markedSelected)) return false;
        }
        return true;
    }


    public String pathToString(Document doc) {
        return (selectedParent != null ? selectedParent.pathToString(doc) : "") + " - " + toString(doc);
    }


    public String toString(Document doc) {
        TreeSet<InterprNode> tmp = new TreeSet<>();
        for(InterprNode n: refinement) {
            n.collectPrimitiveNodes(tmp, doc.interprIdCounter++);
        }
        StringBuilder sb = new StringBuilder();
        for(InterprNode n: tmp) {
            sb.append(n.primId);
            sb.append(", ");
        }

        return sb.toString();
//        return id + " : " + Utils.round(computeAccumulatedWeight()[0].getNormWeight()) + ", " + Utils.round(computeAccumulatedWeight()[1].getNormWeight())  + " - " + Utils.round(computeAccumulatedWeight()[0].w) + ", " + Utils.round(computeAccumulatedWeight()[1].w);
    }


    public void changeState(StateChange.Mode m) {
        for(StateChange sc: modifiedActs) {
            sc.restoreState(m);
        }
    }


    @Override
    public int compareTo(SearchNode sn) {
        return Integer.compare(id, sn.id);
    }


    /**
     * The {@code StateChange} class is used to store the state change of an activation that occurs in each node of
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


    public SearchNode getParent() {
        return getDecision() ? selectedParent : excludedParent;
    }


    private boolean getDecision() {
        return excludedParent == null || selectedParent.id > excludedParent.id;
    }


    public Boolean getCachedDecision() {
        x: for(Map.Entry<SearchNode, Boolean> me: candidate.cache.entrySet()) {
            SearchNode n = this;
            SearchNode cn = me.getKey();
            do {
                if(n.getDecision() != cn.getDecision()) {
                    if(affectsUnknown(n.getParent())) {
                        continue x;
                    }
                }
                n = n.getParent();
                cn = cn.getParent();
            } while(n.selectedParent != null);

            return me.getValue();
        }

        return null;
    }


    public boolean affectsUnknown(SearchNode p) {
        for(InterprNode n: p.refinement) {
            if(n.act != null) {
                for(SynapseActivation sa: n.act.neuronOutputs) {
                    if(sa.s.key.isRecurrent && !sa.s.key.isNeg) {
                        if(getCoverage(sa.output.key.o) == Coverage.UNKNOWN) return true;
                    }
                }
            }
        }
        return false;
    }


    private static class Candidate implements Comparable<Candidate> {
        public TreeMap<SearchNode, Boolean> cache = new TreeMap<>();
        public InterprNode refinement;

        int id;
        Integer minBegin;
        Integer maxEnd;

        public Candidate(InterprNode refinement, int id) {
            this.refinement = refinement;
            minBegin = refinement.act.key.r.begin;
            maxEnd = refinement.act.key.r.end;
            this.id = id;
        }


        @Override
        public int compareTo(Candidate c) {
            int r = Integer.compare(minBegin, c.minBegin);
            if(r != 0) return r;
            r = Integer.compare(c.maxEnd - c.minBegin, maxEnd - minBegin);
            if(r != 0) return r;
            return Integer.compare(id, c.id);
        }
    }
}
