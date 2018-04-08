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
package org.aika;

import org.aika.lattice.AndNode;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.lattice.OrNode;
import org.aika.neuron.INeuron;
import org.aika.neuron.Relation;
import org.aika.neuron.Synapse;

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

    private Model model;
    private int threadId;
    private INeuron neuron;
    private Document doc;
    private OrNode outputNode;
    private Collection<Synapse> modifiedSynapses;


    public static boolean convert(Model m, int threadId, Document doc, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        return new Converter(m, threadId, doc, neuron, modifiedSynapses).convert();
    }


    private Converter(Model model, int threadId, Document doc, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        this.model = model;
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
            outputNode.removeParents(threadId, false);
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

            outputNode.removeParents(threadId, false);

            if (noFurtherRefinement || i == MAX_AND_NODE_SIZE) {
                outputNode.addInput(offset, threadId, requiredNode, false);
            } else {
                for (Synapse s : tmp) {
                    boolean belowThreshold = sum + s.weight + remainingSum + neuron.posRecSum + neuron.biasSum <= 0.0;
                    if (belowThreshold) {
                        break;
                    }

                    if (!reqSyns.contains(s)) {
                        NodeContext nln;
                        nln = expandNode(nodeContext, s);

                        Integer nOffset = Utils.nullSafeMin(s.key.relativeRid, offset);
                        outputNode.addInput(nOffset, threadId, nln, false);
                        remainingSum -= s.weight;
                    }
                }
            }
        } else {
            for (Synapse s : modifiedSynapses) {
                if (s.weight + neuron.posRecSum + neuron.biasSum > 0.0) {
                    Node nln = s.inputNode.get();
                    offset = s.key.relativeRid;
                    outputNode.addInput(offset, threadId, nln, false);
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
                if (s.inputNode == null) {
                    InputNode iNode = InputNode.add(model, s.key.createInputNodeKey(), s.input.get());
                    iNode.setModified();
                    iNode.setSynapse(s);
                    iNode.postCreate(doc);
                    s.inputNode = iNode.provider;
                }

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
            nln.node = s.inputNode.get();
            nln.offsets = new Synapse[] {s};
        } else {
            Relation[] relations = new Relation[nln.offsets.length];
            for(int i = 0; i < nc.offsets.length; i++) {
                Synapse linkedSynapse = nc.offsets[i];
                relations[i] = s.relations.get(linkedSynapse);
            }

            AndNode.Refinement ref = new AndNode.Refinement(relations, s.inputNode);
            nln.node = AndNode.createNextLevelNode(model, threadId, doc, nc.node, ref, null);

            nln.offsets = new Synapse[nc.offsets.length + 1];
            for(int i = 0; i < nc.offsets.length; i++) {
                nln.offsets[ref.offsets[i]] = nc.offsets[i];
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
    }
}
