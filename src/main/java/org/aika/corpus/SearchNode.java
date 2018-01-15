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


import org.aika.neuron.Activation.StateChange;
import org.aika.neuron.Activation.SynapseActivation;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron.NormWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.aika.corpus.InterprNode.State.SELECTED;
import static org.aika.corpus.InterprNode.State.EXCLUDED;
import static org.aika.corpus.InterprNode.State.UNKNOWN;

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

    public static int MAX_SEARCH_STEPS = 1000000;

    public int id;

    public SearchNode excludedParent;
    public SearchNode selectedParent;

    public long visited;
    Candidate candidate;
    int level;

    DebugState debugState;

    public enum DebugState {
        CACHED,
        LIMITED,
        EXPLORE
    }

    NormWeight weightDelta = NormWeight.ZERO_WEIGHT;
    NormWeight accumulatedWeight;

    public List<StateChange> modifiedActs = new ArrayList<>();


    public SearchNode(Document doc, SearchNode selParent, SearchNode exclParent, Candidate c, int level, Collection<InterprNode> changed, boolean cached) {
        id = doc.searchNodeIdCounter++;
        this.level = level;
        visited = doc.visitedCounter++;
        selectedParent = selParent;
        excludedParent = exclParent;

        SearchNode lsn = null;
        boolean modified = true;
        if (c != null) {
            candidate = c;

            lsn = candidate.cachedSearchNode;
            if (lsn != null && cached) {
                modified = lsn.checkInputActModified();
            }


/*            if(cached) {
                candidate.cachedSearchNode.changeState(StateChange.Mode.NEW);
                weightDelta = candidate.cachedSearchNode.weightDelta;

                if (getParent() != null) {
                    accumulatedWeight = weightDelta.add(getParent().accumulatedWeight);
                }
                return;

            }
            */
//            candidate.cachedSearchNode = this;
        }
        boolean cachedSim = cached && !modified && lsn != null;

        weightDelta = doc.vQueue.adjustWeight(this, changed, cachedSim ? lsn.visited : visited);

/*        if (cachedSim) {
            if (Utils.round(weightDelta.w) != Utils.round(lsn.weightDelta.w)) {
                System.out.println();
            }
            if (!compareNewState(lsn)) {
                System.out.println();
            }
        }
*/
        SearchNode pn = getParent();
        if (pn != null && pn.candidate != null) {
            pn.candidate.debugComputed[cachedSim ? 0 : 1]++;
        }

        if (getParent() != null) {
            accumulatedWeight = weightDelta.add(getParent().accumulatedWeight);
        }
        if (Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("Search Step: " + id + "  Candidate Weight Delta: " + weightDelta);
            log.info(doc.neuronActivationsToString(true, true, false) + "\n");
        }
    }


    public void collectResults(Collection<InterprNode> results) {
        if (candidate != null) {
            results.add(candidate.refinement);
        }
        if (selectedParent != null) selectedParent.collectResults(results);
    }


    public void reconstructSelectedResult(Document doc) {
        if (getParent() != null) getParent().reconstructSelectedResult(doc);

        changeState(Activation.Mode.NEW);

        SearchNode pn = getParent();
        if (pn != null && pn.candidate != null) {
            pn.candidate.refinement.setState(getDecision() ? SELECTED : EXCLUDED, visited);
        }

        for (StateChange sc : modifiedActs) {
            Activation act = sc.getActivation();
            if (act.isFinalActivation()) {
                doc.finallyActivatedNeurons.add(act.key.node.neuron.get(doc));
            }
        }
    }


    public void dumpDebugState() {
        SearchNode n = this;
        while (n != null && n.level >= 0) {
            System.out.println(
                    n.level + " " +
                            n.debugState +
                            " DECISION:" + n.getDecision() +
                            " " + n.candidate != null ? n.candidate.toString() : "" +
                            " MOD-ACTS:" + n.modifiedActs.size()
            );

            n = n.getParent();
        }
    }


    public NormWeight search(Document doc) {
        if (candidate == null) {
            return processResult(doc);
        }

        NormWeight selectedWeight = NormWeight.ZERO_WEIGHT;
        NormWeight excludedWeight = NormWeight.ZERO_WEIGHT;

        boolean precondition = checkPrecondition();

        boolean alreadySelected = precondition && !candidate.isConflicting();
        boolean alreadyExcluded = !precondition || checkExcluded(candidate.refinement, doc.visitedCounter++);
        Boolean cachedDecision = !alreadyExcluded ? candidate.cachedDecision : null;

        if (doc.searchStepCounter > MAX_SEARCH_STEPS) {
            doc.interrupted = true;

            dumpDebugState();
        }
        doc.searchStepCounter++;

        if (Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("Search Step: " + id);
            log.info(toString());
        }

        if (Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info(doc.neuronActivationsToString(true, true, false) + "\n");
        }

        if (alreadyExcluded || alreadySelected) {
            debugState = DebugState.LIMITED;
        } else if (cachedDecision != null) {
            debugState = DebugState.CACHED;
        } else {
            debugState = DebugState.EXPLORE;
        }

        candidate.debugCounts[debugState.ordinal()]++;

        SearchNode selectedChild = null;
        SearchNode excludedChild = null;

        if (!alreadyExcluded) {
            candidate.refinement.setState(SELECTED, visited);

            if (cachedDecision == null || cachedDecision) {
                if (candidate.cachedDecision == null) {
                    invalidateCachedDecisions(doc.visitedCounter++);
                }

                Candidate c = doc.candidates.size() > level + 1 ? doc.candidates.get(level + 1) : null;
                selectedChild = new SearchNode(doc, this, excludedParent, c, level + 1, Collections.singleton(candidate.refinement), candidate.cachedSNDecision == Boolean.TRUE);

                candidate.debugDecisionCounts[0]++;
                selectedWeight = selectedChild.search(doc);
                selectedChild.changeState(Activation.Mode.OLD);
            }

            candidate.refinement.setState(UNKNOWN, visited);
            candidate.refinement.activation.rounds.reset();
        }
        if (doc.interrupted) {
            return NormWeight.ZERO_WEIGHT;
        }

        if (!alreadySelected) {
            candidate.refinement.setState(EXCLUDED, visited);

            if (cachedDecision == null || !cachedDecision) {
                Candidate c = doc.candidates.size() > level + 1 ? doc.candidates.get(level + 1) : null;
                excludedChild = new SearchNode(doc, selectedParent, this, c, level + 1, Collections.singleton(candidate.refinement), candidate.cachedSNDecision == Boolean.FALSE);

                candidate.debugDecisionCounts[1]++;
                excludedWeight = excludedChild.search(doc);
                excludedChild.changeState(Activation.Mode.OLD);
            }

            candidate.refinement.setState(UNKNOWN, visited);
            candidate.refinement.activation.rounds.reset();
        }


        if (cachedDecision == null) {
            boolean dir = selectedWeight.getNormWeight() >= excludedWeight.getNormWeight();
            dir = alreadySelected || (!alreadyExcluded && dir);

            if (!alreadyExcluded) {
                candidate.cachedDecision = dir;
            }

            candidate.cachedSNDecision = dir;
            SearchNode csn = dir ? selectedChild : excludedChild;
            if (csn.candidate != null) {
                csn.candidate.cachedSearchNode = csn;
            }
            return dir ? selectedWeight : excludedWeight;
        } else {
            return cachedDecision ? selectedWeight : excludedWeight;
        }
    }


    private boolean checkPrecondition() {
        Set soin = candidate.refinement.selectedOrInterprNodes;
        return soin != null && !soin.isEmpty();
    }


    private boolean checkInputActModified() {
        for (StateChange sc : modifiedActs) {
            if (checkInputActModified(sc.getActivation())) {
                return true;
            }
        }
        return false;
    }


    private boolean checkInputActModified(Activation act) {
        if (act.rounds.modified > visited) {
            return true;
        }
        for (SynapseActivation sa : act.neuronInputs) {
            if (sa.input.rounds.modified > visited) {
                return true;
            }
        }
        return false;
    }


    private void invalidateCachedDecisions(long v) {
        for (Activation act : candidate.refinement.neuronActivations) {
            for (SynapseActivation sa : act.neuronOutputs) {
                if (!sa.synapse.isNegative()) {
                    Candidate posCand = sa.output.key.interpretation.candidate;
                    if (posCand != null) {
                        if (posCand.cachedDecision == Boolean.FALSE && candidate.id < posCand.id) {
                            posCand.cachedDecision = null;
                            posCand.cachedSNDecision = null;
                        }
                    }

                    ArrayList<InterprNode> conflicting = new ArrayList<>();
                    Conflicts.collectConflicting(conflicting, sa.output.key.interpretation, v);
                    for (InterprNode c : conflicting) {
                        Candidate negCand = c.candidate;
                        if (negCand != null) {
                            if (negCand.cachedDecision == Boolean.TRUE && candidate.id < negCand.id) {
                                negCand.cachedDecision = null;
                                negCand.cachedSNDecision = null;
                            }
                        }
                    }
                }
            }
        }
    }


    private NormWeight processResult(Document doc) {
        double accNW = accumulatedWeight.getNormWeight();

        if (accNW > getSelectedAccumulatedWeight(doc)) {
            doc.selectedSearchNode = this;
        }

        return accumulatedWeight;
    }


    private double getSelectedAccumulatedWeight(Document doc) {
        return doc.selectedSearchNode != null ? doc.selectedSearchNode.accumulatedWeight.getNormWeight() : -1.0;
    }


    private boolean checkExcluded(InterprNode ref, long v) {
        ArrayList<InterprNode> conflicts = new ArrayList<>();
        Conflicts.collectConflicting(conflicts, ref, v);
        for (InterprNode cn : conflicts) {
            if (cn.state == SELECTED) return true;
        }
        return false;
    }



    public String pathToString(Document doc) {
        return (selectedParent != null ? selectedParent.pathToString(doc) : "") + " - " + toString(doc);
    }


    public String toString(Document doc) {
        TreeSet<InterprNode> tmp = new TreeSet<>();
        candidate.refinement.collectPrimitiveNodes(tmp, doc.interpretationIdCounter++);
        StringBuilder sb = new StringBuilder();
        for (InterprNode n : tmp) {
            sb.append(n.primId);
            sb.append(", ");
        }

        return sb.toString();
    }


    public void changeState(Activation.Mode m) {
        for (StateChange sc : modifiedActs) {
            sc.restoreState(m);
        }
    }


    @Override
    public int compareTo(SearchNode sn) {
        return Integer.compare(id, sn.id);
    }


    public boolean compareNewState(SearchNode cachedNode) {
        if (modifiedActs == null && cachedNode.modifiedActs == null) return true;
        if (modifiedActs == null || cachedNode.modifiedActs == null) return false;

        if (modifiedActs.size() != cachedNode.modifiedActs.size()) {
            return false;
        }
        for (int i = 0; i < modifiedActs.size(); i++) {
            StateChange sca = modifiedActs.get(i);
            StateChange scb = cachedNode.modifiedActs.get(i);

            if (!sca.newRounds.compare(scb.newRounds)) {
                return false;
            }
        }

        return true;
    }


    public SearchNode getParent() {
        return getDecision() ? selectedParent : excludedParent;
    }


    private boolean getDecision() {
        return excludedParent == null || selectedParent.id > excludedParent.id;
    }
}
