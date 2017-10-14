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

    private TreeSet<Entry> queue = new TreeSet<>(new Comparator<Entry>() {
        @Override
        public int compare(Entry rsk1, Entry rsk2) {
            if (rsk1.node == null && rsk2.node != null) return -1;
            else if (rsk1.node != null && rsk2.node == null) return 1;
            else if (rsk1.node == null && rsk2.node == null) return 0;

            int r = Integer.compare(rsk2.node.get().level, rsk1.node.get().level) * -1;
            if (r != 0) return r;
            r = rsk1.node.compareTo(rsk2.node);
            if (r != 0) return r;
            return Utils.compareInteger(rsk1.offset, rsk2.offset);
        }
    });

    private List<Entry> outputs = new ArrayList<>();
    private List<Entry> cleanup = new ArrayList<>();

    private Model m;
    private int threadId;
    private INeuron neuron;
    private OrNode outputNode;
    private Collection<Synapse> modifiedSynapses;

    private int numAboveTolerance = 0;
    private double sumBelowTolerance = 0.0;


    public Converter(Model m, int threadId, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        this.m = m;
        this.neuron = neuron;
        this.threadId = threadId;
        this.modifiedSynapses = modifiedSynapses;
    }


    /**
     * Translates the synapse weights of a neuron into logic nodes.
     *
     * @return
     */
    public boolean convert() {
        long v = Node.visitedCounter++;
        OrNode outputNode = neuron.node.get();

        if (modifiedSynapses.isEmpty()) return false;


        neuron.maxRecurrentSum = 0.0;
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

            if (!s.isNegative() && !s.key.isRecurrent) {
                if (s.w >= -neuron.bias * TOLERANCE) {
                    numAboveTolerance++;
                } else {
                    sumBelowTolerance += s.w;
                }
            }
        }
        assert numAboveTolerance >= 1;


        if (queue.isEmpty()) {
            queue.add(new Entry(null, null));
        }

        while (!queue.isEmpty()) {
            Entry rsk = queue.pollFirst();

            computeRefinements(rsk, v);
        }

        if (outputs.isEmpty()) return false;

        for (Entry e : outputs) {
            Node n = e.node.get();
            outputNode.addInput(e.offset, n, false);
        }

        for (Entry e : cleanup) {
            Node n = e.node.get();
            outputNode.removeInput(e.offset, n, false);
            outputNode.removeInput(e.offset, n, true);

            e.node.get().cleanup(m);
        }

        return true;
    }


    private void computeRefinements(Entry rsk, long v) {
        neuron.lock.acquireWriteLock();

        Node pa = rsk.node != null ? rsk.node.get() : null;
        double sum = neuron.posRecSum - (neuron.negDirSum + neuron.negRecSum);
        double x = sum + ((pa != null ? pa.level : 0) + 1 == numAboveTolerance ? sumBelowTolerance : 0.0);

        Collection<Synapse> tmp;
        if (pa == null) {
            tmp = modifiedSynapses;
        } else {
            sum += pa.computeSynapseWeightSum(rsk.offset, neuron);
            tmp = neuron.inputSynapses.values();
        }

        for (Synapse s : tmp) {
            if (s.w >= -neuron.bias * TOLERANCE && !s.isNegative() && !s.key.isRecurrent && sum + Math.abs(s.w) + s.maxLowerWeightsSum > 0.0) {
                Node nln = rsk.node == null ?
                        s.inputNode.get() :
                        AndNode.createNextLevelNode(m, threadId, pa, new AndNode.Refinement(s.key.relativeRid, rsk.offset, s.inputNode), false);

                if (nln != null) {
                    processEntry(nln, v, Utils.nullSafeMin(s.key.relativeRid, rsk.offset), x);
                }
            }
        }
        neuron.lock.releaseWriteLock();
    }


    void processEntry(Node node, long v, Integer offset, double x) {
        Entry e = new Entry(node.provider, offset);
        try {
            Node.RidVisited nv = node.getThreadState(threadId, true).lookupVisited(offset);

            if (node.computeSynapseWeightSum(offset, neuron) + x > 0 || !node.isExpandable(false)) {
                if (nv.outputNode != v) {
                    nv.outputNode = v;
                    if (node.isCovered(threadId, offset, v)) {
                        cleanup.add(e);
                    } else {
                        outputs.add(e);
                    }
                }
            } else {
                if (nv.adjust != v) {
                    nv.adjust = v;
                    queue.add(e);

                    outputNode.addInput(e.offset, e.node.get(), true);
                }
            }
        } catch (Node.ThreadState.RidOutOfRange e) {
        }
    }


    private static class Entry implements Comparable<Entry> {
        Provider<? extends Node> node;
        Integer offset;

        public Entry(Provider<? extends Node> pa, Integer offset) {
            this.node = pa;
            this.offset = offset;
        }


        public String toString() {
            return "Offset:" + offset + " Node:" + node.get().logicToString();
        }

        @Override
        public int compareTo(Entry rs) {
            int r = node.compareTo(rs.node);
            if (r != 0) return r;
            return Utils.compareInteger(offset, rs.offset);
        }
    }
}
