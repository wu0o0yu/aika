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
import network.aika.neuron.activation.Activation.RecursiveDepthExceededException;
import network.aika.neuron.activation.Activation.OscillatingActivationsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.Decision.EXCLUDED;
import static network.aika.neuron.activation.Decision.SELECTED;
import static network.aika.neuron.activation.Decision.UNKNOWN;


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
    public static boolean ENABLE_CACHING_COMPARISON = true;
    public static boolean OPTIMIZE_SEARCH = true;
    public static boolean COMPUTE_SOFT_MAX = false;

    private int id;

    private SearchNode excludedParent;
    private SearchNode selectedParent;

    private long visited;
    private Activation act;
    private int level;

    private DebugState debugState;


    public SearchNode getChild(Decision d) {
        return d.getChild(this);
    }


    public SearchNode getAlternative() {
        return getDecision().getInvertedChild(getParent());
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


    public enum DebugState {
        CACHED,
        LIMITED,
        EXPLORE
    }

    private double weightDelta;
    private double accumulatedWeight = 0.0;

    private Map<Activation, Option> modifiedActs = new TreeMap<>(Activation.ACTIVATION_ID_COMP);

    private Step step = Step.INIT;
    private Decision currentDecision = UNKNOWN;
    private boolean selectBranchSearched;
    private boolean excludeBranchSearched;
    SearchNode selectedChild = null;
    SearchNode excludedChild = null;
    private double selectedWeight = 0.0;
    private double excludedWeight = 0.0;
    private double selectedWeightSum = 0.0;
    private double excludedWeightSum = 0.0;
    private long processVisited;
    private boolean bestPath;
    private int cachedCount = 1;
    private int cachedFactor = 1;

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
    }


    public void updateActivations(Document doc) throws OscillatingActivationsException {
        Activation parentAct = getParent() != null ? getParent().act : null;

        SearchNode csn = null;
        boolean modified = true;
        if (parentAct != null) {
            parentAct.currentSearchNode = this;

            csn = parentAct.cachedSearchNode;

            if (csn == null || csn.getDecision() != getDecision()) {
                parentAct.markDirty(visited);
                parentAct.getOutputLinks().forEach(
                        l -> l.getOutput().markDirty(visited)
                );
            } else {
                modified = csn.isModified();

                if (modified) {
                    parentAct.debugComputed[2]++;
                }
            }
        }

        if(modified) {
            weightDelta = doc.getValueQueue().process(this);
            markDirty();

            if(parentAct != null && followPath()) {
                parentAct.cachedSearchNode = this;
            }
        } else {
            if(ENABLE_CACHING) {
                parentAct.cachedSearchNode.changeState(Activation.Mode.NEW);
                weightDelta = parentAct.cachedSearchNode.weightDelta;

                for(Activation mAct: parentAct.cachedSearchNode.modifiedActs.keySet()) {
                    mAct.saveState(this);
                }
            } else {
                weightDelta = doc.getValueQueue().process(this);
                if (ENABLE_CACHING_COMPARISON && (Math.abs(weightDelta - csn.weightDelta) > 0.00001 || !compareNewState(csn))) {
                    log.error("Cached search node activation do not match the newly computed results.");
                    log.info("Computed results (" + weightDelta + "):");
                    dumpDebugState();
                    log.info("Cached results (" + csn.weightDelta + "):");
                    csn.dumpDebugState();

                    throw new RuntimeException("Comparison between cached and computed search node failed!");
                }
            }
        }

        if (parentAct != null) {
            parentAct.debugComputed[modified ? 1 : 0]++;
        }

        if (getParent() != null) {
            SearchNode pn = getParent();

            accumulatedWeight = weightDelta + pn.accumulatedWeight;
        }
    }


    private boolean followPath() {
        return getActivation().currentOption.searchNode == this || getDecision() == getPreferedDecision(getActivation().currentOption);
    }


    private static Decision getPreferedDecision(Option o) {
        return o.getState().value > 0.0 ? SELECTED : EXCLUDED;
    }


    public int getId() {
        return id;
    }


    public Map<Activation, Option> getModifiedActivations() {
        return modifiedActs;
    }


    public double getAccumulatedWeight() {
        return accumulatedWeight;
    }


    public Activation getActivation() {
        if(getParent() != null && getParent().act != null) {
            return getParent().act;
        }
        return null;
    }


    private boolean isModified() {
        for (Option sc : modifiedActs.values()) {
            if (sc.act.markedDirty > visited || sc.decision != sc.act.getDecision()) {
                return true;
            }
            if(sc.isActive()) {
                if(sc.act
                        .getOutputLinks()
                        .anyMatch(l -> l.getOutput().getDecision() != UNKNOWN && l.getOutput().markedDirty > visited)
                        ) {
                    return true;
                }
            }
        }
        return false;
    }


    private void markDirty() {
        if(getParent() == null || getParent().act == null) return;

        SearchNode csn = getParent().act.cachedSearchNode;

        Set<Activation> acts = new TreeSet<>(Activation.ACTIVATION_ID_COMP);
        acts.addAll(modifiedActs.keySet());
        if(csn != null) {
            acts.addAll(csn.modifiedActs.keySet());
        }

        acts.forEach(act -> {
            Option sca = modifiedActs.get(act);
            Option scb = csn != null ? csn.modifiedActs.get(act) : null;

            if (sca == null || scb == null || !sca.compare(scb)) {
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
        for (Map.Entry<Activation, Option> me: modifiedActs.entrySet()) {
            Option sca = me.getValue();
            Option scb = cachedNode.modifiedActs.get(me.getKey());

            if (!sca.compare(scb)) {
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
            log.info(
                    n.level + " " +
                            n.debugState +
                            " DECISION:" + decision +
                            weights +
                            " " + (n.act != null ? n.act.toString() : "") +
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
    public static void search(Document doc, SearchNode root, long v, Long timeoutInMilliSeconds) throws TimeoutException, RecursiveDepthExceededException, OscillatingActivationsException {
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

                    sn.selectedChild.setWeight(returnWeightSum);

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

                    sn.excludedChild.setWeight(returnWeightSum);

                    sn.postReturn(sn.excludedChild);

                    if(sn.act.repeat && OPTIMIZE_SEARCH) {
                        sn.step = Step.PREPARE_SELECT;
                        sn.selectBranchSearched = false;
                    } else {
                        sn.step = Step.FINAL;
                    }

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


    public void setWeight(double w) {
        for(Option sc: modifiedActs.values()) {
            sc.setWeight(w);
        }
    }


    private static void checkTimeoutCondition(Long timeoutInMilliSeconds, long startTime) throws TimeoutException {
        if(timeoutInMilliSeconds != null && System.currentTimeMillis() > startTime + timeoutInMilliSeconds) {
            throw new TimeoutException("Interpretation search took too long: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }


    public double getWeightSum() {
        return selectedWeightSum + excludedWeightSum;
    }


    private void initStep(Document doc) throws RecursiveDepthExceededException {
        act = doc.candidates.get(level);

        if(OPTIMIZE_SEARCH) {
            Decision cd = getCachedDecision();

            setWeightSum(cd, act.alternativeCachedWeightSum);

            if(COMPUTE_SOFT_MAX && cd != null && cd != UNKNOWN) {
                SearchNode asn = act.cachedSearchNode.getAlternative();
                if(asn != null) {
                    asn.cachedCount++;
                }
            }
        }

        if (doc.searchStepCounter > MAX_SEARCH_STEPS) {
            dumpDebugState();
            throw new RuntimeException("Max search step exceeded.");
        }

        doc.searchStepCounter++;

        storeDebugInfos();
    }


    private Decision getCachedDecision() {
        return act.cachedDecision;
    }


    private boolean prepareSelectStep(Document doc) throws OscillatingActivationsException {
        act.repeat = false;

        if(OPTIMIZE_SEARCH && getCachedDecision() == EXCLUDED) {
            return false;
        }
        if(skip == SELECTED) {
            return false;
        }
        if(doc.getModel().getSkipSelectStep().evaluate(act)) {
            return false;
        }

        selectedChild = new SearchNode(doc, this, excludedParent, level + 1);

        selectedChild.updateActivations(doc);

        if(!selectedChild.followPath()) {
            return false;
        }

        if (act.cachedDecision == UNKNOWN) {
            invalidateCachedDecisions();
        }

        selectBranchSearched = true;

        act.debugDecisionCounts[0]++;

        return true;
    }


    private boolean prepareExcludeStep(Document doc) throws RecursiveDepthExceededException, OscillatingActivationsException {
        if(OPTIMIZE_SEARCH && getCachedDecision() == SELECTED) {
            return false;
        }
        if(skip == EXCLUDED) {
            return false;
        }

        excludedChild = new SearchNode(doc, selectedParent, this, level + 1);

        excludedChild.updateActivations(doc);

        if(!excludedChild.followPath()) {
            return false;
        }

        excludeBranchSearched = true;

        act.debugDecisionCounts[1]++;

        return true;
    }


    private void postReturn(SearchNode child) {
        child.changeState(Activation.Mode.OLD);
    }


    private double finalStep() {
        Decision d = selectedWeight >= excludedWeight ? SELECTED : EXCLUDED;

        if (selectBranchSearched && excludeBranchSearched) {
            act.cachedDecision = d;
            act.alternativeCachedWeightSum = getWeightSum(act.cachedDecision);
        }

        SearchNode cn = d == SELECTED ? selectedChild : excludedChild;
        if(cn != null && cn.bestPath) {
            act.bestChildNode = cn;
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
        act
                .getOutputLinks()
                .filter(l -> !l.isNegative(CURRENT))
                .forEach(l -> invalidateCachedDecision(l.getOutput()));
    }


    public static void invalidateCachedDecision(Activation act) {
        if (act != null) {
            if (act.cachedDecision == EXCLUDED) {
                act.cachedDecision = UNKNOWN;
                act.repeat = true;
            }
        }

        for (Activation c : act.getConflicts()) {
            if (c != null) {
                if (c.cachedDecision == SELECTED) {
                    c.cachedDecision = UNKNOWN;
                }
            }
        }
    }


    private double processResult(Document doc) {
        double accNW = accumulatedWeight;

        if (level > doc.selectedSearchNode.level || accNW > getSelectedAccumulatedWeight(doc)) {
            doc.selectedSearchNode = this;
            doc.storeFinalState();
            bestPath = true;
        } else {
            bestPath = false;
        }
/*
        if(COMPUTE_SOFT_MAX) {
            dumpDebugState();
            System.out.println(accumulatedWeight);
            System.out.println();
        }
*/
        return accumulatedWeight;
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


    private void computeCacheFactor() {
        SearchNode pn = getParent();
        cachedFactor = (pn != null ? pn.cachedFactor : 1) * cachedCount;

        for(Option sc: modifiedActs.values()) {
            sc.setCacheFactor(cachedFactor);
        }
    }


    private double getSelectedAccumulatedWeight(Document doc) {
        return doc.selectedSearchNode != null ? doc.selectedSearchNode.accumulatedWeight : -1.0;
    }


    public String pathToString() {
        return (selectedParent != null ? selectedParent.pathToString() : "") + " - " + toString();
    }


    public String toString() {
        return "id:" + id + " actId:" + (act != null ? act.getId() : "-") + " Decision:" + getDecision() + " curDec:" + currentDecision;
    }


    public void changeState(Activation.Mode m) {
        for (Option sc : modifiedActs.values()) {
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
        if (!selectBranchSearched || !excludeBranchSearched) {
            debugState = DebugState.LIMITED;
        } else if (getCachedDecision() != UNKNOWN) {
            debugState = DebugState.CACHED;
        } else {
            debugState = DebugState.EXPLORE;
        }

        act.debugCounts[debugState.ordinal()]++;
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
