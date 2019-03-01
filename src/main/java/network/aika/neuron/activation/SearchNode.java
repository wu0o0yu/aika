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
package network.aika.neuron.activation;


import network.aika.Document;
import network.aika.Utils;
import network.aika.neuron.activation.Activation.StateChange;
import network.aika.neuron.activation.Activation.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static network.aika.neuron.activation.SearchNode.Decision.SELECTED;
import static network.aika.neuron.activation.SearchNode.Decision.EXCLUDED;
import static network.aika.neuron.activation.SearchNode.Decision.UNKNOWN;

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
    public static boolean OPTIMIZE_SEARCH = true;
    public static boolean COMPUTE_SOFT_MAX = false;

    private int id;

    private SearchNode excludedParent;
    private SearchNode selectedParent;

    private long visited;
    private Candidate candidate;
    private int level;

    private DebugState debugState;


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

    private double weightDelta;
    private double accumulatedWeight = 0.0;

    private Map<Activation, Activation.StateChange> modifiedActs = new TreeMap<>(Activation.ACTIVATION_ID_COMP);

    private Step step = Step.INIT;
    private Decision currentDecision = UNKNOWN;
    private Decision preDecision;
    private SearchNode selectedChild = null;
    private SearchNode excludedChild = null;
    private double selectedWeight = 0.0;
    private double excludedWeight = 0.0;
    private double selectedWeightSum = 0.0;
    private double excludedWeightSum = 0.0;
    private long processVisited;
    private boolean bestPath;
    private int cachedCount = 1;
    private int cachedFactor = 1;

    private Option option;

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
        visited = doc.getNewVisitedId();
        selectedParent = selParent;
        excludedParent = exclParent;

        Candidate c = getParent() != null ? getParent().candidate : null;

        SearchNode csn = null;
        boolean modified = true;
        if (c != null) {
            c.currentSearchNode = this;

            csn = c.cachedSearchNode;

            if (csn == null || csn.getDecision() != getDecision()) {
                Activation act = c.activation;
                act.markDirty(visited);
                act.getOutputLinks().forEach(
                        l -> l.getOutput().markDirty(visited)
                );
            } else {
                modified = csn.isModified();

                if (modified) {
                    c.debugComputed[2]++;
                }
            }
        }

        if(modified) {
            weightDelta = doc.getValueQueue().process(doc, this);
            markDirty();

            if(c != null) {
                c.cachedSearchNode = this;
            }

        } else {
            if(ENABLE_CACHING) {
                c.cachedSearchNode.changeState(Activation.Mode.NEW);
                weightDelta = c.cachedSearchNode.weightDelta;

                for(Activation act: c.cachedSearchNode.modifiedActs.keySet()) {
                    act.saveOldState(modifiedActs, doc.getNewVisitedId());
                    act.saveNewState();
                }
            } else {
                weightDelta = doc.getValueQueue().process(doc, this);
                if (Math.abs(weightDelta - csn.weightDelta) > 0.00001 || !compareNewState(csn)) {
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
            SearchNode pn = getParent();

            accumulatedWeight = weightDelta + pn.accumulatedWeight;
        }
    }


    public int getId() {
        return id;
    }


    public Option getOption() {
        return option;
    }


    public Map<Activation, StateChange> getModifiedActivations() {
        return modifiedActs;
    }


    public double getAccumulatedWeight() {
        return accumulatedWeight;
    }


    public Activation getActivation() {
        if(getParent() != null && getParent().candidate != null) {
            return getParent().candidate.activation;
        }
        return null;
    }


    public Option getCurrentOption()  {
        switch(currentDecision) {
            case SELECTED:
                return selectedChild.option;
            case EXCLUDED:
                return excludedChild.option;
            default:
                return null;
        }
    }


    private boolean isModified() {
        for (StateChange sc : modifiedActs.values()) {
            if (sc.getActivation().markedDirty > visited || sc.newState != sc.getActivation().decision) {
                return true;
            }
            if(sc.newRounds.isActive()) {
                if(sc.getActivation()
                        .getOutputLinks()
                        .anyMatch(l -> l.getOutput().decision != UNKNOWN && l.getOutput().markedDirty > visited)
                        ) {
                    return true;
                }
            }
        }
        return false;
    }


    private void markDirty() {
        if(getParent() == null || getParent().candidate == null) return;

        SearchNode csn = getParent().candidate.cachedSearchNode;

        Set<Activation> acts = new TreeSet<>(Activation.ACTIVATION_ID_COMP);
        acts.addAll(modifiedActs.keySet());
        if(csn != null) {
            acts.addAll(csn.modifiedActs.keySet());
        }

        acts.forEach(act -> {
            StateChange sca = modifiedActs.get(act);
            StateChange scb = csn != null ? csn.modifiedActs.get(act) : null;

            if (sca == null || scb == null || !sca.newRounds.compare(scb.newRounds)) {
                act.getOutputLinks()
                        .forEach(l -> l.getOutput().markDirty(visited));
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


    public void dumpDebugState() {
        SearchNode n = this;
        String weights = "";
        Decision decision = UNKNOWN;
        while (n != null && n.level >= 0) {
            System.out.println(
                    n.level + " " +
                            n.debugState +
                            " DECISION:" + decision +
                            weights +
                            " " + (n.candidate != null ? n.candidate.toString() : "") +
                            " MOD-ACTS:" + n.modifiedActs.size()
            );

            decision = n.getDecision();
            weights = " AW:" + Utils.round(n.accumulatedWeight) +
                    " DW:" + Utils.round(n.weightDelta);

            n = n.getParent();
        }
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
    public static void search(Document doc, SearchNode root, long v, Long timeoutInMilliSeconds) throws TimeoutException {
        SearchNode sn = root;
        double returnWeight = 0.0;
        double returnWeightSum = 0.0;
        long startTime = System.currentTimeMillis();

        do {
            if (sn.processVisited != v) {
                sn.step = Step.INIT;
                sn.processVisited = v;
            }

            switch(sn.step) {
                case INIT:
                    if (sn.level >= doc.candidates.size()) {
                        checkTimeoutCondition(timeoutInMilliSeconds, startTime);

                        returnWeight = sn.processResult(doc);
                        returnWeightSum = returnWeight;

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
                    sn.currentDecision = SELECTED;
                    sn = sn.selectedChild;
                    break;
                case POST_SELECT:
                    sn.selectedWeight = returnWeight;
                    sn.selectedWeightSum = returnWeightSum;

                    if(COMPUTE_SOFT_MAX) {
                        sn.selectedChild.option.setWeight(returnWeightSum);
                    }

                    sn.postReturn(sn.selectedChild);
                    sn.step = Step.PREPARE_EXCLUDE;
                    break;
                case PREPARE_EXCLUDE:
                    sn.step = sn.prepareExcludeStep(doc) ? Step.EXCLUDE : Step.FINAL;
                    break;
                case EXCLUDE:
                    sn.step = Step.POST_EXCLUDE;
                    sn.currentDecision = EXCLUDED;
                    sn = sn.excludedChild;
                    break;
                case POST_EXCLUDE:
                    sn.excludedWeight = returnWeight;
                    sn.excludedWeightSum = returnWeightSum;

                    if(COMPUTE_SOFT_MAX) {
                        sn.excludedChild.option.setWeight(returnWeightSum);
                    }

                    sn.postReturn(sn.excludedChild);
                    sn.step = sn.candidate.repeat && OPTIMIZE_SEARCH ? Step.PREPARE_SELECT : Step.FINAL;
                    break;
                case FINAL:
                    returnWeight = sn.finalStep();
                    returnWeightSum = sn.getWeightSum();

                    sn.currentDecision = UNKNOWN;
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


    private static void checkTimeoutCondition(Long timeoutInMilliSeconds, long startTime) {
        if(timeoutInMilliSeconds != null && System.currentTimeMillis() > startTime + timeoutInMilliSeconds) {
            throw new TimeoutException("Interpretation search took too long: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }


    public double getWeightSum() {
        return selectedWeightSum + excludedWeightSum;
    }


    private void initStep(Document doc) {
        candidate = doc.candidates.get(level);

        boolean precondition = candidate.activation.isActiveable();

        preDecision = candidate.activation.inputDecision;
        if(preDecision == UNKNOWN && (!precondition || checkExcluded(candidate.activation))) {
            preDecision = EXCLUDED;
        }

        if(preDecision == UNKNOWN && !candidate.isConflicting() && !candidate.activation.hasUndecidedPositiveFeedbackLinks()) {
            preDecision = SELECTED;
        }
        if(preDecision == UNKNOWN && OPTIMIZE_SEARCH) {
            Decision cd = getCachedDecision();

            setWeightSum(cd, candidate.alternativeCachedWeightSum);

            if(COMPUTE_SOFT_MAX && cd != null && cd != UNKNOWN) {
                SearchNode asn = candidate.cachedSearchNode.getAlternative();
                if(asn != null) {
                    asn.cachedCount++;
                }
            }

            preDecision = cd;
        }

        if (doc.searchStepCounter > MAX_SEARCH_STEPS) {
            dumpDebugState();
            throw new RuntimeException("Max search step exceeded.");
        }

        doc.searchStepCounter++;

        storeDebugInfos();
    }


    public SearchNode getAlternative() {
        SearchNode pn = getParent();
        switch(getDecision()) {
            case SELECTED:
                return pn.excludedChild;
            case EXCLUDED:
                return pn.selectedChild;
        }
        return null;
    }


    private Decision getCachedDecision() {
        return preDecision != EXCLUDED ? candidate.cachedDecision : Decision.UNKNOWN;
    }


    private boolean prepareSelectStep(Document doc) {
        candidate.repeat = false;

        if(preDecision == EXCLUDED) {
            return false;
        }
        if(skip == SELECTED) {
            return false;
        }
        if(doc.getModel().getSkipSelectStep().evaluate(candidate.activation)) {
            return false;
        }

        candidate.activation.setDecision(SELECTED, visited);

        if (candidate.cachedDecision == UNKNOWN) {
            invalidateCachedDecisions();
        }

        selectedChild = new SearchNode(doc, this, excludedParent, level + 1);

        candidate.debugDecisionCounts[0]++;

        if(COMPUTE_SOFT_MAX) {
            selectedChild.option = candidate.activation.new Option(id, SELECTED);
        }

        return true;
    }


    private boolean prepareExcludeStep(Document doc) {
        if(preDecision == SELECTED) {
            return false;
        }
        if(skip == EXCLUDED) {
            return false;
        }
        if(preDecision != EXCLUDED && generatesUnsuppressedExcluded() && !candidate.activation.hasUndecidedPositiveFeedbackLinks()) {
            return false;
        }

        candidate.activation.setDecision(EXCLUDED, visited);

        excludedChild = new SearchNode(doc, selectedParent, this, level + 1);

        candidate.debugDecisionCounts[1]++;

        if(COMPUTE_SOFT_MAX) {
            excludedChild.option = candidate.activation.new Option(id, EXCLUDED);
        }

        return true;
    }


    private boolean generatesUnsuppressedExcluded() {
        x: for (Activation cAct : candidate.activation.getConflicts()) {
            if(cAct.decision == EXCLUDED && cAct.isActiveable()) {
                for (Activation icAct : cAct.getConflicts()) {
                    if (candidate.activation != icAct && icAct.decision != EXCLUDED) {
                        continue x;
                    }
                }
                return true;
            }
        }

        for (Activation cAct : candidate.activation.getConflicts()) {
            if (cAct.decision != EXCLUDED) return false;
        }

        return true;
    }


    private void postReturn(SearchNode child) {
        child.changeState(Activation.Mode.OLD);

        candidate.activation.setDecision(UNKNOWN, visited);
        candidate.activation.rounds.reset();
    }


    private double finalStep() {
        Decision d;
        Decision cd = getCachedDecision();
        if(cd == UNKNOWN) {
            d = preDecision != UNKNOWN ? preDecision : (selectedWeight >= excludedWeight ? SELECTED : EXCLUDED);

            if (preDecision != EXCLUDED) {
                candidate.cachedDecision = d;
                candidate.alternativeCachedWeightSum = getWeightSum(candidate.cachedDecision);
            }
        } else {
            d = cd;
        }

        SearchNode cn = d == SELECTED ? selectedChild : excludedChild;
        if(cn != null && cn.bestPath) {
            candidate.bestChildNode = cn;
            bestPath = true;
        }

        if(!COMPUTE_SOFT_MAX && (!bestPath || d != SELECTED)) {
            selectedChild = null;
        }

        if(!COMPUTE_SOFT_MAX && (!bestPath || d != EXCLUDED)) {
            excludedChild = null;
        }

        return d == SELECTED ? selectedWeight : excludedWeight;
    }


    private void invalidateCachedDecisions() {
        candidate.activation
                .getOutputLinks()
                .filter(l -> !l.getSynapse().isNegative())
                .forEach(l -> invalidateCachedDecision(l.getOutput()));
    }


    public static void invalidateCachedDecision(Activation act) {
        Candidate pos = act.candidate;
        if (pos != null) {
            if (pos.cachedDecision == Decision.EXCLUDED) {
                pos.cachedDecision = UNKNOWN;
                pos.repeat = true;
            }
        }

        for (Activation c : act.getConflicts()) {
            Candidate neg = c.candidate;
            if (neg != null) {
                if (neg.cachedDecision == Decision.SELECTED) {
                    neg.cachedDecision = UNKNOWN;
                }
            }
        }
    }


    private double processResult(Document doc) {
        double accNW = accumulatedWeight;

        if (level > doc.selectedSearchNode.level || accNW > getSelectedAccumulatedWeight(doc)) {
            doc.selectedSearchNode = this;
            storeFinalState(this);
            bestPath = true;
        } else {
            bestPath = false;
        }

        if(COMPUTE_SOFT_MAX) {
            dumpDebugState();
//            System.out.println(accumulatedWeight);
//            System.out.println();

        }

        return accumulatedWeight;
    }


    private static void storeFinalState(SearchNode sn) {
        while(sn != null) {
            if(sn.candidate != null) {
                Activation act = sn.candidate.activation;
                act.finalRounds = act.rounds.copy();
                act.finalDecision = act.decision;
            }
            sn = sn.getParent();
        }
    }


    public static void computeCachedFactor(SearchNode sn) {
        while(sn != null) {
            switch(sn.currentDecision) {
                case UNKNOWN:
                    sn.currentDecision = SELECTED;
                    if(sn.selectedChild != null) {
                        sn = sn.selectedChild;
                        sn.computeCacheFactor();
                    }
                    break;
                case SELECTED:
                    sn.currentDecision = EXCLUDED;
                    if(sn.excludedChild != null) {
                        sn = sn.excludedChild;
                        sn.computeCacheFactor();
                    }
                    break;
                case EXCLUDED:
                    sn = sn.getParent();
                    break;
            }
        }
    }


    private double getWeightSum(Decision d) {
        switch(d) {
            case SELECTED:
                return excludedWeightSum;
            case EXCLUDED:
                return selectedWeightSum;
            default:
                return 0.0;
        }
    }


    private void setWeightSum(Decision d, double weightSum) {
        switch (d) {
            case SELECTED:
                excludedWeightSum = weightSum;
                break;
            case EXCLUDED:
                selectedWeightSum = weightSum;
                break;
        }
    }



    private void computeCacheFactor() {
        SearchNode pn = getParent();
        cachedFactor = (pn != null ? pn.cachedFactor : 1) * cachedCount;

        option.setCacheFactor(cachedFactor);
    }


    private double getSelectedAccumulatedWeight(Document doc) {
        return doc.selectedSearchNode != null ? doc.selectedSearchNode.accumulatedWeight : -1.0;
    }


    private boolean checkExcluded(Activation ref) {
        for (Activation cn : ref.getConflicts()) {
            if (cn.decision == SELECTED) return true;
        }
        return false;
    }



    public String pathToString() {
        return (selectedParent != null ? selectedParent.pathToString() : "") + " - " + toString();
    }


    public String toString() {
        return "id:" + id + " actId:" + candidate.activation.getId() + " Decision:" + getDecision() + " curDec:" + currentDecision;
    }


    public void changeState(Activation.Mode m) {
        for (Activation.StateChange sc : modifiedActs.values()) {
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
        if (preDecision != UNKNOWN) {
            debugState = DebugState.LIMITED;
        } else if (getCachedDecision() != UNKNOWN) {
            debugState = DebugState.CACHED;
        } else {
            debugState = DebugState.EXPLORE;
        }

        candidate.debugCounts[debugState.ordinal()]++;
    }


    public static class TimeoutException extends RuntimeException {

        public TimeoutException(String message) {
            super(message);
        }
    }


    public interface SkipSelectStep {
        boolean evaluate(Activation act);
    }
}
