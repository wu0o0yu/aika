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

    /**
     * Synapses with a weight smaller than the tolerance relative to the bias are not translated into logic nodes. Otherwise
     * too many irrelevant would be generated if there are a lot of synapses with small weights.
     */
    public static double TOLERANCE = 0.1;

//    private TreeSet<Entry> queue = new TreeSet<>(new Comparator<Entry>() {
//        @Override
//        public int compare(Entry rsk1, Entry rsk2) {
//            if (rsk1.node == null && rsk2.node != null) return -1;
//            else if (rsk1.node != null && rsk2.node == null) return 1;
//            else if (rsk1.node == null && rsk2.node == null) return 0;
//
//            int r = Integer.compare(rsk2.node.get().level, rsk1.node.get().level) * -1;
//            if (r != 0) return r;
//            r = rsk1.node.compareTo(rsk2.node);
//            if (r != 0) return r;
//            return Utils.compareInteger(rsk1.offset, rsk2.offset);
//        }
//    });


    public static Comparator<Synapse> SYNAPSE_COMP = new Comparator<Synapse>() {
        @Override
        public int compare(Synapse s1, Synapse s2) {
            int r = Double.compare(s2.w, s1.w);
            if(r != 0) return r;
            return Synapse.INPUT_SYNAPSE_COMP.compare(s1, s2);
        }
    };

//    private List<Entry> outputs = new ArrayList<>();
//    private List<Entry> cleanup = new ArrayList<>();

    private Model m;
    private int threadId;
    private INeuron neuron;
    private OrNode outputNode;
    private Collection<Synapse> modifiedSynapses;

//    private int numAboveTolerance = 0;
//    private double sumBelowTolerance = 0.0;


    public Converter(Model m, int threadId, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        this.m = m;
        this.neuron = neuron;
        this.threadId = threadId;
        this.modifiedSynapses = modifiedSynapses;
    }



    public boolean convert() {
        outputNode = neuron.node.get();

        for (Synapse s : modifiedSynapses) {
            INeuron in = s.input.get();
            in.lock.acquireWriteLock();

            if (s.inputNode == null) {
                InputNode iNode = InputNode.add(m, s.key.createInputNodeKey(), s.input.get());
                iNode.provider.setModified();
                iNode.isBlocked = in.isBlocked;
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
                final boolean isOptionalInput = sum + remainingSum - s.w - neuron.negRecSum + neuron.negDirSum + neuron.posRecSum + neuron.bias > 0.0;
                final boolean maxAndNodesReached = i >= AndNode.MAX_POS_NODES;
                if (isOptionalInput || maxAndNodesReached) {
                    break;
                }

                remainingSum -= s.w;
                reqSyns.add(s);

                requiredNode = getNextLevelNode(offset, requiredNode, s);
                offset = Utils.nullSafeMin(s.key.relativeRid, offset);

                i++;

                sum += s.w;

                final boolean sumOfSynapseWeightsAboveThreshold = sum - neuron.negRecSum + neuron.negDirSum + neuron.posRecSum + neuron.bias > 0.0;
                if (sumOfSynapseWeightsAboveThreshold) {
                    noFurtherRefinement = true;
                    break;
                }
            }

            if (requiredNode != outputNode.requiredNode) {
                outputNode.removeParents(threadId, false);
                outputNode.requiredNode = requiredNode;
            }

            if (noFurtherRefinement || i == AndNode.MAX_POS_NODES) {
                outputNode.addInput(offset, threadId, requiredNode, false);
            } else {
                for (Synapse s : tmp) {
                    boolean belowThreshold = sum + s.w + remainingSum - neuron.negRecSum + neuron.negDirSum + neuron.posRecSum + neuron.bias <= 0.0;
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


//    /**
//     * Translates the synapse weights of a neuron into logic nodes.
//     *
//     * @return
//     */
//    public boolean convertOld() {
//        long v = Node.visitedCounter++;
//        outputNode = neuron.node.get();
//
//        if (modifiedSynapses.isEmpty()) return false;
//
//
//        neuron.maxRecurrentSum = 0.0;
//        for (Synapse s : modifiedSynapses) {
//            INeuron in = s.input.get();
//            in.lock.acquireWriteLock();
//
//            if (s.inputNode == null) {
//                InputNode iNode = InputNode.add(m, s.key.createInputNodeKey(), s.input.get());
//                iNode.provider.setModified();
//                iNode.isBlocked = in.isBlocked;
//                iNode.setSynapse(s);
//                s.inputNode = iNode.provider;
//            }
//
//            if (s.key.isRecurrent) {
//                neuron.maxRecurrentSum += Math.abs(s.w);
//                neuron.provider.setModified();
//            }
//            in.lock.releaseWriteLock();
//
//            if (!s.isNegative() && !s.key.isRecurrent) {
//                if (s.w >= -neuron.bias * TOLERANCE) {
//                    numAboveTolerance++;
//                } else {
//                    sumBelowTolerance += s.w;
//                }
//            }
//        }
//        assert numAboveTolerance >= 1;
//
//
//        if (queue.isEmpty()) {
//            queue.add(new Entry(null, null));
//        }
//
//        while (!queue.isEmpty()) {
//            Entry e = queue.pollFirst();
//            computeRefinements(e, v);
//        }
//
//        if (outputs.isEmpty()) return false;
//
//        for (Entry e : outputs) {
//            Node n = e.node.get();
//            outputNode.addInput(e.offset, n, false);
//            outputNode.addInput(e.offset, e.node.get(), true);
//        }
//
//        for (Entry e : cleanup) {
//            Node n = e.node.get();
//            outputNode.removeInput(e.offset, n, false);
//            outputNode.removeInput(e.offset, n, true);
//
//            e.node.get().cleanup(m);
//        }
//
//        return true;
//    }


//    private void computeRefinements(Entry e, long v) {
//        neuron.lock.acquireWriteLock();
//
//        Node pa = e.node != null ? e.node.get() : null;
//        double sum = neuron.posRecSum - (neuron.negDirSum + neuron.negRecSum);
//        double x = sum + ((pa != null ? pa.level : 0) + 1 == numAboveTolerance ? sumBelowTolerance : 0.0);
//
//        Collection<Synapse> tmp;
//        if (pa == null) {
//            sum += neuron.bias;
//            tmp = modifiedSynapses;
//        } else {
//            sum += pa.computeSynapseWeightSum(e.offset, neuron);
//            tmp = neuron.inputSynapses.values();
//        }
//
//        for (Synapse s : tmp) {
//            if (!s.key.isRecurrent) {
//                Integer offset = Utils.nullSafeMin(s.key.relativeRid, e.offset);
//
//                if (isCandidate(sum, s)) {
//                    Node nln = getNextLevelNode(e, pa, s, true);
//
//                    if (nln != null) {
//                        processEntry(nln, v, offset, x);
//                    }
//                } else {
//                    Node nln = getNextLevelNode(e, pa, s, false);
//
//                    if(nln != null && outputNode.hasParent(offset, nln, true)) {
//                        processEntry(nln, v, offset, x);
//                    }
//                }
//            }
//        }
//        neuron.lock.releaseWriteLock();
//    }
//
//
//    private Node getNextLevelNode(Entry rsk, Node pa, Synapse s, boolean create) {
//        if(rsk.node == null) {
//            return s.inputNode.get();
//        } else if(create) {
//            return AndNode.createNextLevelNode(m, threadId, pa, new AndNode.Refinement(s.key.relativeRid, rsk.offset, s.inputNode), false);
//        } else {
//            Provider<AndNode> p = pa.getAndChild(new AndNode.Refinement(s.key.relativeRid, rsk.offset, s.inputNode));
//            if(p != null) {
//                return p.get();
//            }
//        }
//        return null;
//    }
//
//
//    private boolean isCandidate(double sum, Synapse s) {
//        return s.w >= -neuron.bias * TOLERANCE && !s.isNegative() && sum + Math.abs(s.w) + s.maxLowerWeightsSum > 0.0;
//    }
//
//
//    void processEntry(Node node, long v, Integer offset, double x) {
//        Entry e = new Entry(node.provider, offset);
//        try {
//            Node.RidVisited nv = node.getThreadState(threadId, true).lookupVisited(offset);
//
//            if (node.computeSynapseWeightSum(offset, neuron) + x > 0 || !node.isExpandable(false)) {
//                if (nv.outputNode != v) {
//                    nv.outputNode = v;
//                    if (node.isCovered(threadId, offset, v)) {
//                        cleanup.add(e);
//                    } else {
//                        outputs.add(e);
//                    }
//
//                    if(outputNode.hasParent(offset, node, true)) {
//                        queue.add(e);
//                    }
//                }
//            } else {
//                if (nv.adjust != v) {
//                    nv.adjust = v;
//                    queue.add(e);
//
//                    outputNode.addInput(e.offset, e.node.get(), true);
//                }
//            }
//        } catch (Node.ThreadState.RidOutOfRange ex) {
//        }
//    }
//
//
//    private static class Entry implements Comparable<Entry> {
//        Provider<? extends Node> node;
//        Integer offset;
//
//        public Entry(Provider<? extends Node> pa, Integer offset) {
//            this.node = pa;
//            this.offset = offset;
//        }
//
//
//        public String toString() {
//            return "Offset:" + offset + " Node:" + node.get().logicToString();
//        }
//
//        @Override
//        public int compareTo(Entry rs) {
//            int r = node.compareTo(rs.node);
//            if (r != 0) return r;
//            return Utils.compareInteger(offset, rs.offset);
//        }
//    }
}
