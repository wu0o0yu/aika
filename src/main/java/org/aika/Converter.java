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

import org.aika.corpus.Document;
import org.aika.lattice.AndNode;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.lattice.OrNode;
import org.aika.neuron.INeuron;
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

        double conjSum = 0.0;
        TreeSet<Synapse> tmp = new TreeSet<>(SYNAPSE_COMP);
        for(Synapse s: neuron.inputSynapses.values()) {
            if(!s.isNegative() && !s.key.isRecurrent) {
                conjSum += s.weight;
                tmp.add(s);
            }
        }

        Integer offset = null;
        Node requiredNode = null;
        boolean noFurtherRefinement = false;
        TreeSet<Synapse> reqSyns = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
        double sum = 0.0;
        neuron.requiredSum = 0.0;

        if(conjSum + neuron.posRecSum + neuron.biasSum > 0.0) {
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

                requiredNode = getNextLevelNode(offset, requiredNode, s);
                offset = Utils.nullSafeMin(s.key.relativeRid, offset);

                i++;

                sum += s.weight;

                final boolean sumOfSynapseWeightsAboveThreshold = sum + neuron.posRecSum + neuron.biasSum > 0.0;
                if (sumOfSynapseWeightsAboveThreshold) {
                    noFurtherRefinement = true;
                    break;
                }
            }

            outputNode.removeParents(threadId, false);
            if (requiredNode != outputNode.requiredNode) {
                outputNode.requiredNode = requiredNode;
            }

            if (noFurtherRefinement || i == MAX_AND_NODE_SIZE) {
                outputNode.addInput(offset, threadId, requiredNode, false);
            } else {
                for (Synapse s : tmp) {
                    boolean belowThreshold = sum + s.weight + remainingSum + neuron.posRecSum + neuron.biasSum <= 0.0;
                    if (belowThreshold) {
                        break;
                    }

                    if (!reqSyns.contains(s)) {
                        Node nln;
                        nln = getNextLevelNode(offset, requiredNode, s);

                        Integer nOffset = Utils.nullSafeMin(s.key.relativeRid, offset);
                        outputNode.addInput(nOffset, threadId, nln, false);
                        remainingSum -= s.weight;
                    }
                }
            }
        } else {
            System.out.println();
        }

        for (Synapse s : modifiedSynapses) {
            if(s.weight + neuron.posRecSum + neuron.biasSum > 0.0) {
                Node nln = s.inputNode.get();
                offset = s.key.relativeRid;
                outputNode.addInput(offset, threadId, nln, false);
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
        double maxRecurrentSumDelta = 0.0;

//        neuron.biasSum = 0.0;
        for (Synapse s : modifiedSynapses) {
            if(s.toBeDeleted) {
                s.update(doc, -s.weight, 0.0);
            }

            INeuron in = s.input.get();
            in.lock.acquireWriteLock();

            if (s.inputNode == null) {
                InputNode iNode = InputNode.add(model, s.key.createInputNodeKey(), s.input.get());
                iNode.setModified();
                iNode.setSynapse(s);
                iNode.postCreate(doc);
                s.inputNode = iNode.provider;
            }

            if(!s.inactive) {
                if (s.key.isRecurrent) {
                    maxRecurrentSumDelta += Math.abs(s.getNewWeight()) - Math.abs(s.weight);
                }

                sumDelta[s.key.isRecurrent ? RECURRENT : DIRECT][s.isNegative() ? NEGATIVE : POSITIVE] -= s.weight;
                sumDelta[s.key.isRecurrent ? RECURRENT : DIRECT][s.getNewWeight() <= 0.0 ? NEGATIVE : POSITIVE] += s.getNewWeight();
            }

            s.weight += s.weightDelta;
            s.weightDelta = 0.0;

            s.bias += s.biasDelta;
            s.biasDelta = 0.0;

            if(doc != null) {
                s.committedInDoc = doc.id;
            }

            in.lock.releaseWriteLock();

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

        neuron.maxRecurrentSum += maxRecurrentSumDelta;
        neuron.posDirSum += sumDelta[DIRECT][POSITIVE];
        neuron.negDirSum += sumDelta[DIRECT][NEGATIVE];
        neuron.negRecSum += sumDelta[RECURRENT][NEGATIVE];
        neuron.posRecSum += sumDelta[RECURRENT][POSITIVE];

        neuron.setModified();
    }


    private Node getNextLevelNode(Integer offset, Node requiredNode, Synapse s) {
        Node nln;
        if (requiredNode == null) {
            nln = s.inputNode.get();
        } else {
            nln = AndNode.createNextLevelNode(model, threadId, doc, requiredNode, new AndNode.Refinement(s.key.relativeRid, offset, s.inputNode), null);
        }
        return nln;
    }
}
