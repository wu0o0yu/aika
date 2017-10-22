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
import org.aika.neuron.Synapse;

import java.util.*;

/**
 * Converts the synapse weights of a neuron into a boolean logic representation of this neuron.
 *
 * @author Lukas Molzberger
 */
public class Converter {


    public static Comparator<Synapse> SYNAPSE_COMP = new Comparator<Synapse>() {
        @Override
        public int compare(Synapse s1, Synapse s2) {
            int r = Double.compare(s2.w, s1.w);
            if(r != 0) return r;
            return Synapse.INPUT_SYNAPSE_COMP.compare(s1, s2);
        }
    };

    private Model m;
    private int threadId;
    private INeuron neuron;
    private OrNode outputNode;
    private Collection<Synapse> modifiedSynapses;


    public static boolean convert(Model m, int threadId, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        return new Converter(m, threadId, neuron, modifiedSynapses).convert();
    }

    private Converter(Model m, int threadId, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        this.m = m;
        this.neuron = neuron;
        this.threadId = threadId;
        this.modifiedSynapses = modifiedSynapses;
    }


    private boolean convert() {
        outputNode = neuron.node.get();

        for (Synapse s : modifiedSynapses) {
            INeuron in = s.input.get();
            in.lock.acquireWriteLock();

            if (s.inputNode == null) {
                InputNode iNode = InputNode.add(m, s.key.createInputNodeKey(), s.input.get());
                iNode.provider.setModified();
                iNode.setSynapse(s);
                s.inputNode = iNode.provider;
            }

            if (s.key.isRecurrent) {
                neuron.maxRecurrentSum += Math.abs(s.w);
                neuron.provider.setModified();
            }
            in.lock.releaseWriteLock();
        }


        double remainingSum = 0.0;
        double numAboveThreshold = 0;
        TreeSet<Synapse> tmp = new TreeSet<>(SYNAPSE_COMP);
        for(Synapse s: neuron.inputSynapses.values()) {
            if(!s.isNegative() && !s.key.isRecurrent) {
                if (s.w + neuron.bias > 0.0) {
                    if (numAboveThreshold > 0 && neuron.inputSynapses.size() != modifiedSynapses.size()) break;
                    numAboveThreshold++;
                }
                remainingSum += s.w;
                tmp.add(s);
            }
        }

        Integer offset = null;
        Node requiredNode = null;
        boolean noFurtherRefinement = false;
        TreeSet<Synapse> reqSyns = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
        double sum = 0.0;
        if(numAboveThreshold == 0 || neuron.inputSynapses.size() == modifiedSynapses.size()) {
            int i = 0;
            for (Synapse s : tmp) {
                final boolean isOptionalInput = sum + remainingSum - s.w - neuron.negRecSum - neuron.negDirSum + neuron.posRecSum + neuron.bias > 0.0;
                final boolean maxAndNodesReached = i >= AndNode.MAX_AND_NODE_SIZE;
                if (isOptionalInput || maxAndNodesReached) {
                    break;
                }

                remainingSum -= s.w;
                reqSyns.add(s);

                requiredNode = getNextLevelNode(offset, requiredNode, s);
                offset = Utils.nullSafeMin(s.key.relativeRid, offset);

                i++;

                sum += s.w;

                final boolean sumOfSynapseWeightsAboveThreshold = sum - neuron.negRecSum - neuron.negDirSum + neuron.posRecSum + neuron.bias > 0.0;
                if (sumOfSynapseWeightsAboveThreshold) {
                    noFurtherRefinement = true;
                    break;
                }
            }

            outputNode.removeParents(threadId, false);
            if (requiredNode != outputNode.requiredNode) {
                outputNode.requiredNode = requiredNode;
            }

            if (noFurtherRefinement || i == AndNode.MAX_AND_NODE_SIZE) {
                outputNode.addInput(offset, threadId, requiredNode, false);
            } else {
                for (Synapse s : tmp) {
                    boolean belowThreshold = sum + s.w + remainingSum - neuron.negRecSum - neuron.negDirSum + neuron.posRecSum + neuron.bias <= 0.0;
                    if (belowThreshold) {
                        break;
                    }

                    if (!reqSyns.contains(s)) {
                        Node nln;
                        nln = getNextLevelNode(offset, requiredNode, s);

                        Integer nOffset = Utils.nullSafeMin(s.key.relativeRid, offset);
                        outputNode.addInput(nOffset, threadId, nln, false);
                        remainingSum -= s.w;
                    }
                }
            }
        } else {
            for (Synapse s : modifiedSynapses) {
                Node nln = s.inputNode.get();
                offset = s.key.relativeRid;
                outputNode.addInput(offset, threadId, nln, false);
            }
        }
        return true;
    }


    private Node getNextLevelNode(Integer offset, Node requiredNode, Synapse s) {
        Node nln;
        if (requiredNode == null) {
            nln = s.inputNode.get();
        } else {
            nln = AndNode.createNextLevelNode(m, threadId, requiredNode, new AndNode.Refinement(s.key.relativeRid, offset, s.inputNode), false);
        }
        return nln;
    }
}
