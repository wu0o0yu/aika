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
    public static boolean OPTIMIZE_SEARCH = true;
    public static boolean COMPUTE_SOFT_MAX = false;

    private int id;

    private SearchNode excludedParent;
    private SearchNode selectedParent;

    private Activation act;
    private int level;

    private DebugState debugState;


    public enum DebugState {
        CACHED,
        LIMITED,
        EXPLORE
    }


    public static class Branch {
        boolean searched;
        double weight = 0.0;
        double weightSum = 0.0;

        SearchNode child = null;

        private boolean prepareStep(Document doc, SearchNode c) {
            child = c;

            child.updateActivations(doc);

            if (!child.followPath()) {
                return true;
            }

            searched = true;
            return false;
        }


        public void postStep(double returnWeight, double returnWeightSum) {
            weight = returnWeight;
            weightSum = returnWeightSum;

            child.setWeight(returnWeightSum);
            child.changeState(Activation.Mode.OLD);
        }
    }

    public Branch selected = new Branch();
    public Branch excluded = new Branch();


    private double weightDelta;
    private double accumulatedWeight = 0.0;

    private Map<Activation, Option> modifiedActs = new TreeMap<>(Activation.ACTIVATION_ID_COMP);

    private Step step = Step.INIT;
    private Decision currentDecision = UNKNOWN;


    private long processVisited;
    private boolean bestPath;
    private int cachedCount = 1;
    private int cachedFactor = 1;

    // Avoids having to search the same path twice.
    private Decision skip = UNKNOWN;

    private enum Step {
        INIT,
        SELECT,
        POST_SELECT,
        EXCLUDE,
        POST_EXCLUDE,
        FINAL
    }


    public SearchNode(Document doc, SearchNode selParent, SearchNode exclParent, int level) {
        id = doc.searchNodeIdCounter++;
        this.level = level;
        selectedParent = selParent;
        excludedParent = exclParent;
    }


    public Branch getBranch(Decision d) {
        switch(d) {
            case SELECTED:
                return selected;
            case EXCLUDED:
                return excluded;
        }
        return null;
    }


    public SearchNode getAlternative() {
        return getParent().getBranch(getDecision().getInverted()).child;
    }


    public void updateActivations(Document doc) throws OscillatingActivationsException {
        Activation parentAct = getParent() != null ? getParent().act : null;

        if (parentAct != null) {
            parentAct.currentSearchNode = this;
        }

        weightDelta = doc.getValueQueue().process(this);

        if (parentAct != null && followPath()) {
            parentAct.cachedSearchNode = this;
        }

        if (getParent() != null) {
            SearchNode pn = getParent();

            accumulatedWeight = weightDelta + pn.accumulatedWeight;
        }
    }


    public boolean followPath() {
        return getActivation().currentOption.searchNode == this && getDecision() == getActivation().currentOption.getState().getPreferredDecision();
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
        if (getParent() != null && getParent().act != null) {
            return getParent().act;
        }
        return null;
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
     * <p>
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

            switch (sn.step) {
                case INIT:
                    if (sn.level >= doc.candidates.size()) {
                        checkTimeoutCondition(timeoutInMilliSeconds, startTime);

                        returnWeight = sn.processResult(doc);
                        returnWeightSum = returnWeight;

                        sn.step = Step.FINAL;
                        sn = sn.getParent();
                    } else {
                        sn.initStep(doc);
                        sn.step = Step.SELECT;
                    }
                    break;
                case SELECT:
                    sn.act.repeat = false;

                    if (sn.prepareStep(doc, SELECTED)) {
                        sn.step = Step.POST_SELECT;
                        sn = sn.selected.child; // Recursive Step
                    } else {
                        sn.step = Step.EXCLUDE;
                    }

                    break;
                case POST_SELECT:
                    sn.selected.postStep(returnWeight, returnWeightSum);

                    sn.step = Step.EXCLUDE;
                    break;
                case EXCLUDE:
                    if (sn.prepareStep(doc, EXCLUDED)) {
                        sn.step = Step.POST_EXCLUDE;
                        sn = sn.excluded.child; // Recursive Step
                    } else {
                        sn.step = Step.FINAL;
                    }

                    break;
                case POST_EXCLUDE:
                    sn.excluded.postStep(returnWeight, returnWeightSum);

                    sn.step = Step.FINAL;
                    break;
                case FINAL:
                    if (sn.act.repeat && OPTIMIZE_SEARCH) {
                        sn.step = Step.SELECT;
                        sn.selected.searched = false;
                    } else {
                        returnWeight = sn.finalStep();
                        returnWeightSum = sn.getWeightSum();

                        sn.currentDecision = UNKNOWN;
                        SearchNode pn = sn.getParent();
                        if (pn != null) {
                            pn.skip = sn.getDecision();
                        }
                        sn = pn;
                    }
                    break;
                default:
            }
        } while (sn != null);
    }



    public void setWeight(double w) {
        for (Option sc : modifiedActs.values()) {
            sc.setWeight(w);
        }
    }


    private static void checkTimeoutCondition(Long timeoutInMilliSeconds, long startTime) throws TimeoutException {
        if (timeoutInMilliSeconds != null && System.currentTimeMillis() > startTime + timeoutInMilliSeconds) {
            throw new TimeoutException("Interpretation search took too long: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }


    public double getWeightSum() {
        return selected.weightSum + excluded.weightSum;
    }


    private void initStep(Document doc) throws RecursiveDepthExceededException {
        act = doc.candidates.get(level);

        if (OPTIMIZE_SEARCH) {
            Decision cd = getCachedDecision();

            if(cd != null && cd != UNKNOWN) {
                getBranch(cd).weightSum = act.alternativeCachedWeightSum;

                if (COMPUTE_SOFT_MAX) {
                    SearchNode asn = act.cachedSearchNode.getAlternative();
                    if (asn != null) {
                        asn.cachedCount++;
                    }
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


    private boolean prepareStep(Document doc, Decision d) throws OscillatingActivationsException {
        if (OPTIMIZE_SEARCH && getCachedDecision() == d.getInverted()) {
            return false;
        }
        if (skip == d) {
            return false;
        }
        if (doc.getModel().getSkipSelectStep().evaluate(act)) {
            return false;
        }

        SearchNode child = new SearchNode(
                doc, d == SELECTED ? this : selectedParent,
                d == SELECTED ? excludedParent : this,
                level + 1
        );

        if (getBranch(d).prepareStep(doc, child)) return false;

        if (d == SELECTED && act.cachedDecision == UNKNOWN) {
            invalidateCachedDecisions();
        }

        act.debugDecisionCounts[d.ordinal()]++;

        currentDecision = d;

        return true;
    }


    private double finalStep() {
        Decision d = selected.weight > excluded.weight ? SELECTED : EXCLUDED;

        if (selected.searched && excluded.searched) {
            act.cachedDecision = d;
            act.alternativeCachedWeightSum = getBranch(act.cachedDecision).weightSum;
        }

        SearchNode cn = d == SELECTED ? selected.child : excluded.child;
        if (cn != null && cn.bestPath) {
            act.bestChildNode = cn;
            bestPath = true;
        }

        if(!COMPUTE_SOFT_MAX) {
            if (!bestPath || d != SELECTED) {
                selected.child = null;
            }

            if (!bestPath || d != EXCLUDED) {
                excluded.child = null;
            }
        }

        return d == SELECTED ? selected.weight : excluded.weight;
    }


    private void invalidateCachedDecisions() {
        act.getOutputLinks()
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

        act.getInputLinks()
                .filter(l -> l.isRecurrent() && l.isNegative(CURRENT))
                .map(l -> l.getInput())
                .filter(c -> c.cachedDecision == SELECTED)
                .forEach(c -> c.cachedDecision = UNKNOWN);
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
        while (sn != null) {
            switch (sn.currentDecision) {
                case UNKNOWN:
                    sn.currentDecision = SELECTED;
                    if (sn.selected.child != null) {
                        sn = sn.selected.child;
                        sn.computeCacheFactor();
                    }
                    break;
                case SELECTED:
                    sn.currentDecision = EXCLUDED;
                    if (sn.excluded.child != null) {
                        sn = sn.excluded.child;
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

        for (Option sc : modifiedActs.values()) {
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
        if (!selected.searched || !excluded.searched) {
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
