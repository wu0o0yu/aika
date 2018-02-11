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
import org.aika.neuron.Activation.StateChange;
import org.aika.neuron.Activation.SynapseActivation;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.aika.corpus.SearchNode.Decision.SELECTED;
import static org.aika.corpus.SearchNode.Decision.EXCLUDED;
import static org.aika.corpus.SearchNode.Decision.UNKNOWN;
import static org.aika.neuron.Activation.ACTIVATION_ID_COMP;
import static org.aika.neuron.Activation.Mode.NEW;

import java.util.*;

/**
 * The {@code SearchNode} class represents a node in the binary search tree that is used to find the optimal
 * interpretation for a given document. Each search node possess a refinement (simply a set of interpretation nodes).
 * The two options that this search node examines are that the refinement will either part of the final interpretation or not.
 * During each search step the activation values in all the neuron activations adjusted such that they reflect the interpretation of the current search path.
 * When the search reaches the maximum depth of the search tree and no further refinements exists, a weight is computed evaluating the current search path.
 * The search path with the highest weight is used to determine the final interpretation.
 * <p>
 * <p> Before the search is started a set of initial refinements is generated from the conflicts within the document.
 * In other words, if there are no conflicts in a given document, then no search is needed. In this case the final interpretation
 * will simply be the set of all interpretation nodes. The initial refinements are then expanded, meaning all interpretation nodes that are consistent
 * with this refinement are added to the refinement. The initial refinements are then propagated along the search path as refinement candidates.
 *
 * @author Lukas Molzberger
 */
public class SearchNode implements Comparable<SearchNode> {

    private static final Logger log = LoggerFactory.getLogger(SearchNode.class);

    public static int MAX_SEARCH_STEPS = Integer.MAX_VALUE;
    public static boolean ENABLE_CACHING = true;
    public static boolean SEARCH_ITERATIVE = true;

    public int id;

    public SearchNode excludedParent;
    public SearchNode selectedParent;

    public long visited;
    Candidate candidate;
    int level;

    DebugState debugState;


    public enum Decision {
        SELECTED('S'),
        EXCLUDED('E'),
        UNKNOWN('U');

        char s;

        Decision(char s) {
            this.s = s;
        }
    }


    public enum DebugState {
        CACHED,
        LIMITED,
        EXPLORE
    }

    Weight weightDelta = Weight.ZERO;
    Weight accumulatedWeight = Weight.ZERO;

    public Map<Activation, StateChange> modifiedActs = new TreeMap<>(ACTIVATION_ID_COMP);



    private Step step = Step.INIT;
    private boolean alreadySelected;
    private boolean alreadyExcluded;
    private SearchNode selectedChild = null;
    private SearchNode excludedChild = null;
    private Weight selectedWeight = Weight.ZERO;
    private Weight excludedWeight = Weight.ZERO;
    private long processVisited;

    // Avoids having to search the same path twice.
    private Decision skip = UNKNOWN;

    private enum Step {
        INIT,
        PREPARE_SELECT,
        SELECT,
        POST_SELECT,
        PREPARE_EXCLUDE,
        EXCLUDE,
        POST_EXCLUDE,
        FINAL
    }


    public SearchNode(Document doc, SearchNode selParent, SearchNode exclParent, int level) {
        id = doc.searchNodeIdCounter++;
        this.level = level;
        visited = doc.visitedCounter++;
        selectedParent = selParent;
        excludedParent = exclParent;

        Candidate c = getParent() != null ? getParent().candidate : null;

        SearchNode csn = null;
        boolean modified = true;
        if (c != null) {
            c.currentSearchNode = this;

            csn = c.cachedSearchNode;

            if (csn == null || csn.getDecision() != getDecision()) {
                Activation act = c.refinement.activation;
                act.markDirty(visited);
                for (SynapseActivation sa : act.neuronOutputs) {
                    sa.output.markDirty(visited);
                }
            } else {
                modified = csn.isModified();

                if (modified) {
                    c.debugComputed[2]++;
                }
            }
        }

        if(modified) {
            weightDelta = doc.vQueue.process(this);
            markDirty();

            if(c != null) {
                c.cachedSearchNode = this;
            }

        } else {
            if(ENABLE_CACHING) {
                c.cachedSearchNode.changeState(NEW);
                weightDelta = c.cachedSearchNode.weightDelta;

                for(Activation act: c.cachedSearchNode.modifiedActs.keySet()) {
                    act.saveOldState(modifiedActs, doc.visitedCounter++);
                    act.saveNewState();
                }
            } else {
                weightDelta = doc.vQueue.process(this);
                if (Math.abs(weightDelta.w - csn.weightDelta.w) > 0.00001 || !compareNewState(csn)) {
                    log.error("Cached search node activation do not match the newly computed results.");
                    log.info("Computed results:");
                    dumpDebugState();
                    log.info("Cached results:");
                    csn.dumpDebugState();
                }
            }
        }

        if (c != null) {
            c.debugComputed[modified ? 1 : 0]++;
        }

        if (getParent() != null) {
            accumulatedWeight = weightDelta.add(getParent().accumulatedWeight);
        }
    }


    private boolean isModified() {
        for (StateChange sc : modifiedActs.values()) {
            if (sc.getActivation().markedDirty > visited || sc.newState != sc.getActivation().key.interpretation.state) {
                return true;
            }
            if(sc.newRounds.isActive()) {
                for (SynapseActivation sa : sc.getActivation().neuronOutputs) {
                    if (sa.output.key.interpretation.state != UNKNOWN &&
                            sa.output.markedDirty > visited) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private void markDirty() {
        if(getParent() == null || getParent().candidate == null) return;

        SearchNode csn = getParent().candidate.cachedSearchNode;

        Set<Activation> acts = new TreeSet<>(ACTIVATION_ID_COMP);
        acts.addAll(modifiedActs.keySet());
        if(csn != null) {
            acts.addAll(csn.modifiedActs.keySet());
        }

        acts.forEach(act -> {
            StateChange sca = modifiedActs.get(act);
            StateChange scb = csn != null ? csn.modifiedActs.get(act) : null;

            if (sca == null || scb == null || !sca.newRounds.compare(scb.newRounds)) {
                for (Activation.SynapseActivation sa : act.neuronOutputs) {
                    sa.output.markDirty(visited);
                }
            }
        });
    }


    public boolean compareNewState(SearchNode cachedNode) {
        if (modifiedActs == null && cachedNode.modifiedActs == null) return true;
        if (modifiedActs == null || cachedNode.modifiedActs == null) return false;

        if (modifiedActs.size() != cachedNode.modifiedActs.size()) {
            return false;
        }
        for (Map.Entry<Activation, StateChange> me: modifiedActs.entrySet()) {
            StateChange sca = me.getValue();
            StateChange scb = cachedNode.modifiedActs.get(me.getKey());

            if (!sca.newRounds.compare(scb.newRounds)) {
                return false;
            }
        }

        return true;
    }


    public void collectResults(Collection<InterpretationNode> results) {
        SearchNode sn = this;
        do {
            if (sn.candidate != null) {
                results.add(sn.candidate.refinement);
            }
            sn = sn.selectedParent;
        } while(sn != null);
    }


    public void reconstructSelectedResult(Document doc) {
        LinkedList<SearchNode> tmp = new LinkedList<>();
        SearchNode snt = this;
        do {
            tmp.addFirst(snt);
            snt = snt.getParent();
        } while(snt != null);

        for(SearchNode sn: tmp) {
            sn.changeState(NEW);

            SearchNode pn = sn.getParent();
            if (pn != null && pn.candidate != null) {
                pn.candidate.refinement.setState(sn.getDecision(), sn.visited);
            }

            for (StateChange sc : sn.modifiedActs.values()) {
                Activation act = sc.getActivation();
                if (act.isFinalActivation()) {
                    doc.finallyActivatedNeurons.add(act.getINeuron());
                }
            }
        }
    }


    public void dumpDebugState() {
        SearchNode n = this;
        String weights = "";
        Decision decision = UNKNOWN;
        while (n != null && n.level >= 0) {
            log.info(
                    n.level + " " +
                            n.debugState +
                            " DECISION:" + decision +
                            " " + (n.candidate != null ? n.candidate.toString() : "") +
                            " MOD-ACTS:" + n.modifiedActs.size() +
                            weights
            );

            decision = n.getDecision();
            weights = " AW:" + Utils.round(n.accumulatedWeight.w) +
                    " AN:" + Utils.round(n.accumulatedWeight.n) +
                    " DW:" + Utils.round(n.weightDelta.w) +
                    " DN:" + Utils.round(n.weightDelta.n);

            n = n.getParent();
        }
    }


    /**
     * This algorithm is the recursive version of the interpretation search.
     * Its perhaps easier to read than the iterative version.
     */
    public Weight searchRecursive(Document doc) {
        if (level >= doc.candidates.size()) {
            return processResult(doc);
        }

        initStep(doc);

        if (prepareSelectStep(doc)) {
            selectedWeight = selectedChild.searchRecursive(doc);

            postReturn(selectedChild);
        }

        if (prepareExcludeStep(doc)) {
            excludedWeight = excludedChild.searchRecursive(doc);

            postReturn(excludedChild);
        }

        return finalStep();
    }


    /**
     * Searches for the best interpretation for the given document.
     *
     * This implementation of the algorithm is iterative to prevent stack overflow errors from happening.
     * Depending on the document the search tree might be getting very deep.
     *
     * @param doc
     * @param root
     */
    public static void searchIterative(Document doc, SearchNode root, long v) {
        SearchNode sn = root;
        Weight returnWeight = null;

        do {
            if (sn.processVisited != v) {
                sn.step = Step.INIT;
                sn.processVisited = v;
            }

            switch(sn.step) {
                case INIT:
                    if (sn.level >= doc.candidates.size()) {
                        returnWeight = sn.processResult(doc);
                        sn.step = Step.FINAL;
                        sn = sn.getParent();
                    } else {
                        sn.initStep(doc);
                        sn.step = Step.PREPARE_SELECT;
                    }
                    break;
                case PREPARE_SELECT:
                    sn.step = sn.prepareSelectStep(doc) ? Step.SELECT : Step.PREPARE_EXCLUDE;
                    break;
                case SELECT:
                    sn.step = Step.POST_SELECT;
                    sn = sn.selectedChild;
                    break;
                case POST_SELECT:
                    sn.selectedWeight = returnWeight;

                    sn.postReturn(sn.selectedChild);
                    sn.step = Step.PREPARE_EXCLUDE;
                    break;
                case PREPARE_EXCLUDE:
                    sn.step = sn.prepareExcludeStep(doc) ? Step.EXCLUDE : Step.FINAL;
                    break;
                case EXCLUDE:
                    sn.step = Step.POST_EXCLUDE;
                    sn = sn.excludedChild;
                    break;
                case POST_EXCLUDE:
                    sn.excludedWeight = returnWeight;

                    sn.postReturn(sn.excludedChild);
                    sn.step = Step.FINAL;
                    break;
                case FINAL:
                    returnWeight = sn.finalStep();
                    SearchNode pn = sn.getParent();
                    if(pn != null) {
                        pn.skip = sn.getDecision();
                    }
                    sn = pn;
                    break;
                default:
            }
        } while(sn != null);
    }


    private void initStep(Document doc) {
        candidate = doc.candidates.get(level);

        boolean precondition = checkPrecondition();

        alreadySelected = precondition && !candidate.isConflicting();
        alreadyExcluded = !precondition || checkExcluded(candidate.refinement, doc.visitedCounter++);

        if (doc.searchStepCounter > MAX_SEARCH_STEPS) {
            dumpDebugState();
            throw new RuntimeException("Max search step exceeded.");
        }

        doc.searchStepCounter++;

        storeDebugInfos();
    }


    private Decision getCachedDecision() {
        return !alreadyExcluded ? candidate.cachedDecision : Decision.UNKNOWN;
    }


    private boolean prepareSelectStep(Document doc) {
        if(alreadyExcluded || skip == SELECTED || getCachedDecision() == Decision.EXCLUDED) return false;

        candidate.refinement.setState(SELECTED, visited);

        if (candidate.cachedDecision == UNKNOWN) {
            invalidateCachedDecisions(doc.visitedCounter++);
        }

        selectedChild = new SearchNode(doc, this, excludedParent, level + 1);

        candidate.debugDecisionCounts[0]++;

        return true;
    }


    private boolean prepareExcludeStep(Document doc) {
        if(alreadySelected || skip == EXCLUDED || getCachedDecision() == Decision.SELECTED) return false;

        candidate.refinement.setState(EXCLUDED, visited);

        excludedChild = new SearchNode(doc, selectedParent, this, level + 1);

        candidate.debugDecisionCounts[1]++;

        return true;
    }


    private void postReturn(SearchNode child) {
        child.changeState(Activation.Mode.OLD);

        candidate.refinement.setState(UNKNOWN, visited);
        candidate.refinement.activation.rounds.reset();
    }


    private Weight finalStep() {
        selectedChild = null;
        excludedChild = null;
        switch (getCachedDecision()) {
            case UNKNOWN:
                boolean dir = selectedWeight.getNormWeight() >= excludedWeight.getNormWeight();
                dir = alreadySelected || (!alreadyExcluded && dir);

                if (!alreadyExcluded) {
                    candidate.cachedDecision = dir ? Decision.SELECTED : Decision.EXCLUDED;
                }

                return dir ? selectedWeight : excludedWeight;
            case SELECTED:
                return selectedWeight;
            case EXCLUDED:
                return excludedWeight;
            default:
                throw new IllegalStateException();
        }
    }



    private boolean checkPrecondition() {
        Set soin = candidate.refinement.selectedOrInterpretationNodes;
        return soin != null && !soin.isEmpty();
    }


    private void invalidateCachedDecisions(long v) {
        for (SynapseActivation sa : candidate.refinement.activation.neuronOutputs) {
            if (!sa.synapse.isNegative()) {
                Candidate pos = sa.output.key.interpretation.candidate;
                if (pos != null) {
                    if (pos.cachedDecision == Decision.EXCLUDED && candidate.id > pos.id) {
                        pos.cachedDecision = UNKNOWN;
                    }
                }

                for (InterpretationNode c : Conflicts.getConflicting(sa.output.key.interpretation, v)) {
                    Candidate neg = c.candidate;
                    if (neg != null) {
                        if (neg.cachedDecision == Decision.SELECTED && candidate.id > neg.id) {
                            neg.cachedDecision = UNKNOWN;
                        }
                    }
                }
            }
        }
    }


    private Weight processResult(Document doc) {
        double accNW = accumulatedWeight.getNormWeight();

        if (level > doc.selectedSearchNode.level || accNW > getSelectedAccumulatedWeight(doc)) {
            doc.selectedSearchNode = this;
        }

        return accumulatedWeight;
    }


    private double getSelectedAccumulatedWeight(Document doc) {
        return doc.selectedSearchNode != null ? doc.selectedSearchNode.accumulatedWeight.getNormWeight() : -1.0;
    }


    private boolean checkExcluded(InterpretationNode ref, long v) {
        for (InterpretationNode cn : Conflicts.getConflicting(ref, v)) {
            if (cn.state == SELECTED) return true;
        }
        return false;
    }



    public String pathToString(Document doc) {
        return (selectedParent != null ? selectedParent.pathToString(doc) : "") + " - " + toString(doc);
    }


    public String toString(Document doc) {
        TreeSet<InterpretationNode> tmp = new TreeSet<>();
        candidate.refinement.collectPrimitiveNodes(tmp, doc.interpretationIdCounter++);
        StringBuilder sb = new StringBuilder();
        for (InterpretationNode n : tmp) {
            sb.append(n.primId);
            sb.append(" Decision:" + getDecision());
            sb.append(", ");
        }

        return sb.toString();
    }


    public void changeState(Activation.Mode m) {
        for (StateChange sc : modifiedActs.values()) {
            sc.restoreState(m);
        }
    }


    @Override
    public int compareTo(SearchNode sn) {
        return Integer.compare(id, sn.id);
    }


    public SearchNode getParent() {
        return getDecision() == SELECTED ? selectedParent : excludedParent;
    }


    public Decision getDecision() {
        return excludedParent == null || (selectedParent != null && selectedParent.id > excludedParent.id) ? SELECTED : EXCLUDED;
    }


    private void storeDebugInfos() {
        if (alreadyExcluded || alreadySelected) {
            debugState = DebugState.LIMITED;
        } else if (getCachedDecision() != UNKNOWN) {
            debugState = DebugState.CACHED;
        } else {
            debugState = DebugState.EXPLORE;
        }

        candidate.debugCounts[debugState.ordinal()]++;
    }


    public static class Weight {
        public final static Weight ZERO = new Weight(0.0, 0.0);

        public final double w;
        public final double n;

        private Weight(double w, double n) {
            this.w = w;
            this.n = n;
        }

        public static Weight create(double w, double n) {
            assert w >= 0.0 && n >= 0.0;
            if (w == 0.0 && n == 0.0) return ZERO;
            return new Weight(w, n);
        }

        public Weight add(Weight nw) {
            if (nw == null || nw == ZERO) return this;
            return new Weight(w + nw.w, n + nw.n);
        }

        public Weight sub(Weight nw) {
            if (nw == null || nw == ZERO) return this;
            return new Weight(w - nw.w, n - nw.n);
        }

        public double getNormWeight() {
            return n > 0 ? w / n : 0.0;
        }


        public boolean equals(Weight nw) {
            return (Math.abs(w - nw.w) <= INeuron.WEIGHT_TOLERANCE && Math.abs(n - nw.n) <= INeuron.WEIGHT_TOLERANCE);
        }

        public String toString() {
            return "W:" + Utils.round(w) + " N:" + Utils.round(n);
        }
    }

}
