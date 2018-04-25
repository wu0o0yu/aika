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
import network.aika.neuron.Relation;
import network.aika.neuron.Synapse;

import java.util.*;

/**
 * Converts the synapse weights of a neuron into a boolean logic representation of this neuron.
 *
 * @author Lukas Molzberger
 */
public class Converter {

    public static int MAX_AND_NODE_SIZE = 4;


    public static Comparator<Synapse> SYNAPSE_COMP = (s1, s2) -> {
        int r = Double.compare(s2.weight, s1.weight);
        if (r != 0) return r;
        return Synapse.INPUT_SYNAPSE_COMP.compare(s1, s2);
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

        if(neuron.biasSum + neuron.posDirSum + neuron.posRecSum <= 0.0) {
            neuron.requiredSum = neuron.posDirSum + neuron.posRecSum;
            outputNode.removeParents(threadId);
            return false;
        }

        TreeSet<Synapse> tmp = new TreeSet<>(SYNAPSE_COMP);
        for(Synapse s: neuron.inputSynapses.values()) {
            if(!s.isNegative() && !s.key.isRecurrent && !s.inactive) {
                tmp.add(s);
            }
        }

        NodeContext nodeContext = null;
        boolean noFurtherRefinement = false;
        TreeSet<Synapse> reqSyns = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
        double sum = 0.0;
        neuron.requiredSum = 0.0;

        if(neuron.numDisjunctiveSynapses == 0) {
            double remainingSum = neuron.posDirSum;
            int i = 0;
            for (Synapse s : tmp) {
                final boolean isOptionalInput = sum + remainingSum - s.weight + neuron.posRecSum + neuron.biasSum > 0.0;
                final boolean maxAndNodesReached = i >= MAX_AND_NODE_SIZE;
                if (isOptionalInput || maxAndNodesReached) {
                    break;
                }

                remainingSum -= s.weight;
                neuron.requiredSum += s.weight;
                reqSyns.add(s);

                nodeContext = expandNode(nodeContext, s);

                i++;

                sum += s.weight;

                final boolean sumOfSynapseWeightsAboveThreshold = sum + neuron.posRecSum + neuron.biasSum > 0.0;
                if (sumOfSynapseWeightsAboveThreshold) {
                    noFurtherRefinement = true;
                    break;
                }
            }

            outputNode.removeParents(threadId);

            if (noFurtherRefinement || i == MAX_AND_NODE_SIZE) {
                outputNode.addInput(nodeContext.getSynapseIds(), threadId, nodeContext.node);
            } else {
                for (Synapse s : tmp) {
                    boolean belowThreshold = sum + s.weight + remainingSum + neuron.posRecSum + neuron.biasSum <= 0.0;
                    if (belowThreshold) {
                        break;
                    }

                    if (!reqSyns.contains(s)) {
                        NodeContext nln = expandNode(nodeContext, s);
                        outputNode.addInput(nln.getSynapseIds(), threadId, nln.node);
                        remainingSum -= s.weight;
                    }
                }
            }
        } else {
            for (Synapse s : modifiedSynapses) {
                if (s.weight + neuron.posRecSum + neuron.biasSum > 0.0) {
                    NodeContext nln = expandNode(nodeContext, s);
                    outputNode.addInput(nln.getSynapseIds(), threadId, nln.node);
                }
            }
        }

        return true;
    }


    public static final int DIRECT = 0;
    public static final int RECURRENT = 1;
    public static final int POSITIVE = 0;
    public static final int NEGATIVE = 1;

    private void initInputNodesAndComputeWeightSums() {
        double[][] sumDelta = new double[2][2];

//        neuron.biasSum = 0.0;
        for (Synapse s : modifiedSynapses) {
            if(s.toBeDeleted) {
                s.update(doc, -s.weight, 0.0);
            }

            INeuron in = s.input.get();
            in.lock.acquireWriteLock();
            try {
                if (!s.inactive) {
                    sumDelta[s.key.isRecurrent ? RECURRENT : DIRECT][s.isNegative() ? NEGATIVE : POSITIVE] -= s.weight;
                    sumDelta[s.key.isRecurrent ? RECURRENT : DIRECT][s.getNewWeight() <= 0.0 ? NEGATIVE : POSITIVE] += s.getNewWeight();

                    if (s.isConjunction(false, true) && !s.isConjunction(true, true)) {
                        neuron.numDisjunctiveSynapses++;
                    }
                }

                s.weight += s.weightDelta;
                s.weightDelta = 0.0;

                s.bias += s.biasDelta;
                s.biasDelta = 0.0;

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

        neuron.biasSum = Math.min(neuron.biasSum, 0.0);

        assert Double.isFinite(neuron.biasSum);

        neuron.posDirSum += sumDelta[DIRECT][POSITIVE];
        neuron.negDirSum += sumDelta[DIRECT][NEGATIVE];
        neuron.negRecSum += sumDelta[RECURRENT][NEGATIVE];
        neuron.posRecSum += sumDelta[RECURRENT][POSITIVE];

        neuron.setModified();
    }


    private NodeContext expandNode(NodeContext nc, Synapse s) {
        NodeContext nln = new NodeContext();
        if (nc == null) {
            nln.node = s.input.get().outputNode.get();
            nln.offsets = new Synapse[] {s};
        } else {
            nln.offsets = new Synapse[nc.offsets.length + 1];

            Relation[] relations = new Relation[nc.offsets.length];
            for(int i = 0; i < nc.offsets.length; i++) {
                Synapse linkedSynapse = nc.offsets[i];
                relations[i] = s.relations.get(linkedSynapse.id);
            }

            AndNode.Refinement ref = new AndNode.Refinement(new AndNode.RelationsMap(relations), s.input.get().outputNode);
            AndNode.RefValue rv = nc.node.extend(threadId, doc, ref);
            nln.node = rv.child.get(doc);

            for(int i = 0; i < nc.offsets.length; i++) {
                nln.offsets[rv.offsets[i]] = nc.offsets[i];
            }
            for(int i = 0; i < nln.offsets.length; i++) {
                if(nln.offsets[i] == null) {
                    nln.offsets[i] = s;
                }
            }
        }
        return nln;
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
