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
package network.aika;

import network.aika.lattice.AndNode;
import network.aika.lattice.Node;
import network.aika.lattice.OrNode;
import network.aika.neuron.INeuron;
import network.aika.neuron.relation.Relation;
import network.aika.neuron.Synapse;

import java.util.*;

/**
 * Converts the synapse weights of a neuron into a boolean logic representation of this neuron.
 *
 * @author Lukas Molzberger
 */
public class Converter {

    public static int MAX_AND_NODE_SIZE = 6;


    public static Comparator<Synapse> SYNAPSE_COMP = (s1, s2) -> {
        int r = Boolean.compare(
                s2.linksAnyOutput() || s2.identity,
                s1.linksAnyOutput() || s1.identity
        );
        if (r != 0) return r;
        r = Double.compare(s2.weight, s1.weight);
        if (r != 0) return r;
        return Integer.compare(s1.id, s2.id);
    };

    private int threadId;
    private INeuron neuron;
    private Document doc;
    private OrNode outputNode;
    private Collection<Synapse> modifiedSynapses;


    public static boolean convert(int threadId, Document doc, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        return new Converter(threadId, doc, neuron, modifiedSynapses).convert();
    }


    private Converter(int threadId, Document doc, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        this.doc = doc;
        this.neuron = neuron;
        this.threadId = threadId;
        this.modifiedSynapses = modifiedSynapses;
    }


    private boolean convert() {
        outputNode = neuron.node.get();

        initInputNodesAndComputeWeightSums();
        initCreateBeginEndPositionFlags();

        if(neuron.biasSum + neuron.posDirSum + neuron.posRecSum <= 0.0) {
            neuron.requiredSum = neuron.posDirSum + neuron.posRecSum;
            outputNode.removeParents(threadId);
            return false;
        }

        List<Synapse> candidates = prepareCandidates();

        NodeContext nodeContext = null;
        boolean noFurtherRefinement = false;
        TreeSet<Synapse> reqSyns = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
        double sum = 0.0;
        neuron.requiredSum = 0.0;

        if(neuron.numDisjunctiveSynapses == 0) {
            double remainingSum = neuron.posDirSum;
            int i = 0;
            for (Synapse s : candidates) {
                double v = s.getMaxInputValue();
                final boolean isOptionalInput = sum + remainingSum - v + neuron.posRecSum + neuron.posPassiveSum + neuron.biasSum > 0.0;
                final boolean maxAndNodesReached = i >= MAX_AND_NODE_SIZE;
                if (isOptionalInput || maxAndNodesReached) {
                    break;
                }

                remainingSum -= v;
                neuron.requiredSum += v;
                reqSyns.add(s);

                NodeContext nlNodeContext = expandNode(nodeContext, s);
                if(nlNodeContext == null) {
                    break;
                }
                nodeContext = nlNodeContext;

                i++;

                sum += v;

                final boolean sumOfSynapseWeightsAboveThreshold = sum + neuron.posRecSum + neuron.posPassiveSum + neuron.biasSum > 0.0;
                if (sumOfSynapseWeightsAboveThreshold) {
                    noFurtherRefinement = true;
                    break;
                }

            }

            outputNode.removeParents(threadId);

            if (noFurtherRefinement || i == MAX_AND_NODE_SIZE) {
                outputNode.addInput(nodeContext.getSynapseIds(), threadId, nodeContext.node, true);
            } else {
                for (Synapse s : candidates) {
                    double v = s.getMaxInputValue();
                    boolean belowThreshold = sum + v + remainingSum + neuron.posRecSum + neuron.posPassiveSum + neuron.biasSum <= 0.0;
                    if (belowThreshold) {
                        break;
                    }

                    if (!reqSyns.contains(s)) {
                        NodeContext nlNodeContext = expandNode(nodeContext, s);
                        if(nlNodeContext != null) {
                            outputNode.addInput(nlNodeContext.getSynapseIds(), threadId, nlNodeContext.node, true);
                            remainingSum -= v;
                        }
                    }
                }
            }
        } else {
            for (Synapse s : modifiedSynapses) {
                if (s.isDisjunction && !s.isRecurrent) {
                    NodeContext nlNodeContext = expandNode(nodeContext, s);
                    outputNode.addInput(nlNodeContext.getSynapseIds(), threadId, nlNodeContext.node, false);
                }
            }
        }

        return true;
    }


    private void initCreateBeginEndPositionFlags() {
        modifiedSynapses.forEach(s -> {
            boolean[] linksOutput = s.linksOutput();
            if(linksOutput[0]) {
                neuron.createBeginPosition = false;
            }
            if(linksOutput[1]) {
                neuron.createEndPosition = false;
            }
        });
    }


    private List<Synapse> prepareCandidates() {
        Synapse syn = getBestSynapse(neuron.inputSynapses.values());

        TreeSet<Integer> alreadyCollected = new TreeSet<>();
        ArrayList<Synapse> selectedCandidates = new ArrayList<>();
        TreeMap<Integer, Synapse> relatedSyns = new TreeMap<>();
        while(syn != null && selectedCandidates.size() < MAX_AND_NODE_SIZE) {
            relatedSyns.remove(syn.id);
            selectedCandidates.add(syn);
            alreadyCollected.add(syn.id);
            for(Integer relId: syn.relations.keySet()) {
                if(!alreadyCollected.contains(relId)) {
                    Synapse rs = syn.output.getSynapseById(relId);
                    if(rs != null) {
                        relatedSyns.put(relId, rs);
                    }
                }
            }

            syn = getBestSynapse(relatedSyns.values());
        }

        return selectedCandidates;
    }


    private Synapse getBestSynapse(Collection<Synapse> synapses) {
        Synapse maxSyn = null;
        for(Synapse s: synapses) {
            if(!s.isNegative() && !s.isRecurrent && !s.inactive && !s.input.get().isPassiveInputNeuron()) {
                if(maxSyn == null || SYNAPSE_COMP.compare(maxSyn, s) > 0) {
                    maxSyn = s;
                }
            }
        }
        return maxSyn;
    }


    public static final int DIRECT = 0;
    public static final int RECURRENT = 1;
    public static final int POSITIVE = 0;
    public static final int NEGATIVE = 1;


    private void initInputNodesAndComputeWeightSums() {
        double[][] sumDelta = new double[2][2];

        double posPassiveSumDelta = 0.0;
        for (Synapse s : modifiedSynapses) {
            if(s.toBeDeleted) {
                s.update(doc, -s.weight, 0.0, s.limit);
            }

            INeuron in = s.input.get();
            in.lock.acquireWriteLock();
            try {
                if (!s.inactive) {
                    sumDelta[s.isRecurrent ? RECURRENT : DIRECT][s.isNegative() ? NEGATIVE : POSITIVE] -= s.limit * s.weight;
                    sumDelta[s.isRecurrent ? RECURRENT : DIRECT][s.getNewWeight() <= 0.0 ? NEGATIVE : POSITIVE] += (s.limit + s.limitDelta) * s.getNewWeight();

                    if(in.isPassiveInputNeuron() && !s.isNegative()) {
                        posPassiveSumDelta -= !s.isNegative() ? (s.limit * s.weight) : 0.0;
                        posPassiveSumDelta += s.getNewWeight() > 0.0 ? ((s.limit + s.limitDelta) * s.getNewWeight()) : 0.0;
                    }

                    if(!s.isRecurrent) {
                        if (!s.isDisjunction(Synapse.State.OLD) && s.isDisjunction(Synapse.State.NEW)) {
                            neuron.numDisjunctiveSynapses++;
                        } else if (s.isDisjunction(Synapse.State.OLD) && !s.isDisjunction(Synapse.State.NEW)) {
                            neuron.numDisjunctiveSynapses--;
                        }
                    }
                }

                s.weight += s.weightDelta;
                s.weightDelta = 0.0;

                s.bias += s.biasDelta;
                s.biasDelta = 0.0;

                s.limit += s.limitDelta;
                s.limitDelta = 0.0;

                if (doc != null) {
                    s.committedInDoc = doc.id;
                }
            } finally {
                in.lock.releaseWriteLock();
            }

            if(s.toBeDeleted) {
                s.unlink();
            }
        }

        neuron.bias += neuron.biasDelta;
        neuron.biasDelta = 0.0;

        neuron.biasSum += neuron.biasSumDelta;
        neuron.biasSumDelta = 0.0;

        assert Double.isFinite(neuron.biasSum);

        neuron.posDirSum += sumDelta[DIRECT][POSITIVE];
        neuron.negDirSum += sumDelta[DIRECT][NEGATIVE];
        neuron.negRecSum += sumDelta[RECURRENT][NEGATIVE];
        neuron.posRecSum += sumDelta[RECURRENT][POSITIVE];
        neuron.posPassiveSum += posPassiveSumDelta;

        neuron.setModified();
    }


    private NodeContext expandNode(NodeContext nc, Synapse s) {
        if (nc == null) {
            NodeContext nln = new NodeContext();
            nln.node = s.input.get().outputNode.get();
            nln.offsets = new Synapse[] {s};
            return nln;
        } else {
            Relation[] relations = new Relation[nc.offsets.length];
            for(int i = 0; i < nc.offsets.length; i++) {
                Synapse linkedSynapse = nc.offsets[i];
                Set<Relation> relSet = s.getRelationById(linkedSynapse.id);
                if (relSet != null) {
                    assert relSet.size() == 1;
                    relations[i] = relSet.iterator().next();
                }
            }

            NodeContext nln = new NodeContext();
            nln.offsets = new Synapse[nc.offsets.length + 1];
            AndNode.Refinement ref = new AndNode.Refinement(new AndNode.RelationsMap(relations), s.input.get().outputNode);
            AndNode.RefValue rv = nc.node.extend(threadId, doc, ref, null);
            if(rv == null) {
                return null;
            }

            nln.node = rv.child.get(doc);

            for (int i = 0; i < nc.offsets.length; i++) {
                nln.offsets[rv.offsets[i]] = nc.offsets[i];
            }
            for (int i = 0; i < nln.offsets.length; i++) {
                if (nln.offsets[i] == null) {
                    nln.offsets[i] = s;
                }
            }
            return nln;
        }
    }


    private class NodeContext {
        Node node;

        Synapse[] offsets;

        int[] getSynapseIds() {
            int[] result = new int[offsets.length];
            for(int i = 0; i < result.length; i++) {
                result[i] = offsets[i].id;
            }
            return result;
        }
    }
}
