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
import org.aika.lattice.Node;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.RangeSignal;
import org.aika.neuron.Synapse.RangeVisibility;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;


/**
 *
 * @author Lukas Molzberger
 */
public class Model implements Writable {

    public int numberOfThreads = 1;

    public int[] lastCleanup;
    public int[] iterationCounter;

    public Document[] docs;

    public Map<String, Neuron> labeledNeurons = Collections.synchronizedMap(new LinkedHashMap<>());
    public Map<Integer, Neuron> neurons = Collections.synchronizedMap(new TreeMap<>());

    public Map<Integer, Node> initialNodes;
    public Set<Node> allNodes[];

    public Statistic stat = new Statistic();

    private Document dummyDoc;

    public Set<AndNode> numberOfPositionsQueue = Collections.synchronizedSet(new TreeSet<>(new Comparator<AndNode>() {
        @Override
        public int compare(AndNode n1, AndNode n2) {
            int r = Integer.compare(n1.numberOfPositionsNotify, n2.numberOfPositionsNotify);
            if(r != 0) return r;
            return n1.compareTo(n2);
        }
    }));

    public volatile int numberOfPositions;


    public Model() {
        this(1);
    }


    public Model(int numberOfThreads) {
        assert numberOfThreads >= 1;
        this.numberOfThreads = numberOfThreads;

        lastCleanup = new int[numberOfThreads];
        iterationCounter = new int[numberOfThreads];
        allNodes = new Set[numberOfThreads];
        docs = new Document[numberOfThreads];

        for(int i = 0; i < numberOfThreads; i++) {
            allNodes[i] = new TreeSet<>();
        }

        dummyDoc = createDocument(null, 0);
    }


    public Document createDocument(String txt) {
        return createDocument(txt, 0);
    }


    public Document createDocument(String txt, int threadId) {
        Document doc = new Document(txt, this, threadId, iterationCounter[threadId]++);

        if(txt != null) {
            doc.changeNumberOfPositions(doc.length());

            if(docs[threadId] != null) {
                throw new RuntimeException("Two documents are using the same thread. Call clearActivations() first.");
            }
            docs[threadId] = doc;
        }

        return doc;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Network Weights:\n");
        sb.append(networkWeightsToString(false));
        sb.append("\n\n\n");

        return sb.toString();
    }


    public void reset() {
        labeledNeurons.clear();
        neurons.clear();
    }


    public void resetFrequency() {
        for(int t = 0; t < numberOfThreads; t++) {
            for(Node n: allNodes[t]) {
                n.frequency = 0;
            }
        }
    }


    public String networkWeightsToString(boolean all) {
        StringBuilder sb = new StringBuilder();
        for(Neuron n: neurons.values()) {
            if(all || n.node.frequency > 0) {
                sb.append(n.toStringWithSynapses());
                sb.append("\n");
            }
        }
        return sb.toString();
    }




    public InputNeuron createOrLookupInputSignal(String label) {
        return createOrLookupInputSignal(label, false);
    }


    public InputNeuron createOrLookupInputSignal(String label, boolean isBlocked) {
        InputNeuron n = (InputNeuron) labeledNeurons.get(label);
        if(n == null) {
            n = InputNeuron.create(dummyDoc, new InputNeuron(label, isBlocked));
            labeledNeurons.put(label, n);
        }
        return n;
    }


    public Neuron createAndNeuron(Neuron n, double threshold, Input... inputs) {
        return createAndNeuron(n, threshold, new TreeSet<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron representing a conjunction of its inputs.
     *
     * @param n
     * @param threshold
     * @param inputs
     * @return
     */
    public Neuron createAndNeuron(Neuron n, double threshold, Collection<Input> inputs) {
        n.m = this;
        if(n.node != null) throw new RuntimeException("This neuron has already been initialized!");

        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        double bias = 0.0;
        double negDirSum = 0.0;
        double negRecSum = 0.0;
        double posRecSum = 0.0;
        double minWeight = Double.MAX_VALUE;
        for(Input ni: inputs) {
            Synapse s = new Synapse(ni.neuron, new Synapse.Key(ni.weight < 0.0, ni.recurrent, ni.relativeRid, ni.absoluteRid, ni.matchRange, Synapse.RangeSignal.START, ni.startVisibility, Synapse.RangeSignal.END, ni.endVisibility));
            s.w = ni.weight;
            s.maxLowerWeightsSum = ni.maxLowerWeightsSum;

            if(ni.weight < 0.0) {
                if(!ni.recurrent) {
                    negDirSum += ni.weight;
                } else {
                    negRecSum += ni.weight;
                }
            } else if(ni.recurrent) {
                posRecSum += ni.weight;
            }

            if(!ni.optional) {
                bias -= Math.abs(ni.weight) * (ni.weight >= 0.0 ? ni.minInput : 1.0);
                if(ni.weight >= 0.0) {
                    minWeight = Math.min(minWeight, ni.weight * ni.minInput);
                }
            }
            is.add(s);
        }
        bias += minWeight * threshold;

        return Neuron.create(dummyDoc, n, bias, negDirSum, negRecSum, posRecSum, is);
    }


    public Neuron createNeuron(Neuron n, double bias, Input... inputs) {
        return createNeuron(n, bias, new TreeSet<>(Arrays.asList(inputs)));
    }


    public Neuron createNeuron(Neuron n, double bias, Collection<Input> inputs) {
        n.m = this;
        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        double negDirSum = 0.0;
        double negRecSum = 0.0;
        double posRecSum = 0.0;
        for(Input ni: inputs) {
            Synapse s = new Synapse(ni.neuron, new Synapse.Key(ni.weight < 0.0, ni.recurrent, ni.relativeRid, ni.absoluteRid, ni.matchRange, Synapse.RangeSignal.START, ni.startVisibility, Synapse.RangeSignal.END, ni.endVisibility));
            s.w = ni.weight;
            s.maxLowerWeightsSum = ni.maxLowerWeightsSum;

            if(ni.weight < 0.0) {
                if(!ni.recurrent) {
                    negDirSum += ni.weight;
                } else {
                    negRecSum += ni.weight;
                }
            } else if(ni.recurrent) {
                posRecSum += ni.weight;
            }

            is.add(s);
        }

        return Neuron.create(dummyDoc, n, bias, negDirSum, negRecSum, posRecSum, is);
    }


    public Neuron createOrNeuron(Neuron n, Input... inputs) {
        return createOrNeuron(n, new TreeSet<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron representing a disjunction of its inputs.
     *
     * @param n
     * @param inputs
     * @return
     */
    public Neuron createOrNeuron(Neuron n, Set<Input> inputs) {
        n.m = this;
        if(n.node != null) throw new RuntimeException("This neuron has already been initialized!");

        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        double bias = -0.001;
        for(Input ni: inputs) {
            Synapse s = new Synapse(ni.neuron, new Synapse.Key(ni.weight < 0.0, ni.recurrent, ni.relativeRid, ni.absoluteRid, ni.matchRange, RangeSignal.START, ni.startVisibility, RangeSignal.END, ni.endVisibility));
            s.w = ni.weight;
            s.maxLowerWeightsSum = ni.maxLowerWeightsSum;
            is.add(s);
        }

        return Neuron.create(dummyDoc, n, bias, 0.0, 0.0, 0.0, is);
    }


    /**
     * A relational neuron combines the relational id created by a cycle neuron with an input signal.
     *
     * @param n
     * @param ctn
     * @param inputSignal
     * @param dirIS
     * @return
     */
    public Neuron createRelationalNeuron(Neuron n, Neuron ctn, Neuron inputSignal, boolean dirIS) {
        n.m = this;
        if(n.node != null) throw new RuntimeException("This neuron has already been initialized!");

        double bias = -30.0;
        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        if(inputSignal != null) {
            Synapse iss = new Synapse(
                    inputSignal,
                    new Synapse.Key(
                            false,
                            false,
                            null,
                            null,
                            true,
                            dirIS ? RangeSignal.END : RangeSignal.START,
                            RangeVisibility.MATCH_INPUT,
                            dirIS ? RangeSignal.START : RangeSignal.END,
                            RangeVisibility.MAX_OUTPUT
                    )
            );
            iss.w = 20.0;
            iss.maxLowerWeightsSum = 20.0;
            is.add(iss);
        }

        if(ctn != null) {
            Synapse ctns = new Synapse(
                    ctn,
                    new Synapse.Key(
                            false,
                            false,
                            0,
                            null,
                            true,
                            RangeSignal.START,
                            RangeVisibility.MATCH_INPUT,
                            RangeSignal.END,
                            RangeVisibility.MATCH_INPUT
                    )
            );
            ctns.w = 20.0;
            ctns.maxLowerWeightsSum = 20.0;
            is.add(ctns);
        }

        return Neuron.create(dummyDoc, n, bias, 0.0, 0.0, 0.0, is);
    }


    /**
     * A counter neuron is used to compute the relational id. It simply adds a new activation after each clock
     * signal and increases the relational id by one.
     *
     * @param n
     * @param clockSignal
     * @param dirCS  The direction of the clock signal.
     * @param startSignal
     * @param dirSS  The direction of the start signal.
     * @param direction
     * @return
     */
    public Neuron createCounterNeuron(Neuron n, Neuron clockSignal, boolean dirCS, Neuron startSignal, boolean dirSS, boolean direction) {
        n.m = this;

        if(n.node != null) throw new RuntimeException("This neuron has already been initialized!");

        double bias = -44.0;
        double negRecSum = -20.0;
        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        if(clockSignal != null) {
            Synapse css = new Synapse(
                    clockSignal,
                    new Synapse.Key(
                            false,
                            false,
                            null,
                            null,
                            true,
                            RangeSignal.NONE,
                            RangeVisibility.MATCH_INPUT,
                            dirCS ? RangeSignal.START : RangeSignal.END,
                            RangeVisibility.MATCH_INPUT
                    )
            );
            css.w = 20.0;
            css.maxLowerWeightsSum = 8.0;
            is.add(css);
        }

        if(startSignal != null) {
            Synapse sss = new Synapse(
                    startSignal,
                    new Synapse.Key(
                            false,
                            false,
                            0,
                            null,
                            true,
                            dirSS ? RangeSignal.START : RangeSignal.END,
                            RangeVisibility.MATCH_INPUT,
                            RangeSignal.NONE,
                            RangeVisibility.MATCH_INPUT
                    )
            );
            sss.w = 8.0;
            sss.maxLowerWeightsSum = 0.0;
            is.add(sss);
        }

        Synapse lastCycle = new Synapse(
                n,
                new Synapse.Key(
                        false,
                        false,
                        -1,
                        null,
                        true,
                        direction ? RangeSignal.NONE : RangeSignal.END,
                        RangeVisibility.MATCH_INPUT,
                        direction ? RangeSignal.START : RangeSignal.NONE,
                        RangeVisibility.MATCH_INPUT
                )
        );
        lastCycle.w = 8.0;
        lastCycle.maxLowerWeightsSum = 0.0;
        is.add(lastCycle);

        Synapse neg = new Synapse(
                n,
                new Synapse.Key(
                        true,
                        true,
                        0,
                        null,
                        true,
                        RangeSignal.START,
                        RangeVisibility.MAX_OUTPUT,
                        RangeSignal.END,
                        RangeVisibility.MAX_OUTPUT
                )
        );
        neg.w = -20.0;
        neg.maxLowerWeightsSum = 28.0;
        is.add(neg);

        Neuron neuron = Neuron.create(dummyDoc, n, bias, 0.0, negRecSum, 0.0, is);
        neuron.node.passive = true;
        return neuron;
    }



    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(numberOfThreads);
        out.writeInt(numberOfPositions);
        TreeSet<Node> nodes = new TreeSet<>(new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                if(n1.level != -1 && n2.level == -1) return -1;
                else if(n1.level == -1 && n2.level != -1) return 1;
                int r = Integer.compare(n1.level, n2.level);
                if(r != 0) return r;
                return n1.compareTo(n2);
            }
        });

        if(initialNodes != null) {
            nodes.addAll(initialNodes.values());
        }
        for(Set<Node> n: allNodes) {
            nodes.addAll(n);
        }

        out.writeInt(nodes.size());
        for(Node n: nodes) {
            n.write(out);
        }

        out.writeInt(neurons.size());
        for(Neuron n: neurons.values()) {
            n.write(out);
        }

        out.writeInt(neurons.size());
        for(Neuron n: neurons.values()) {
            out.writeInt(n.id);
            for(Synapse s: n.inputSynapses) {
                if(s.input != null && s.input.initialized && s.output != null && s.output.initialized) {
                    out.writeBoolean(true);
                    s.write(out);
                }
            }
            out.writeBoolean(false);
        }
    }


    @Override
    public void readFields(DataInput in, Document doc) throws IOException {
        numberOfThreads = in.readInt();
        numberOfPositions = in.readInt();

        int s = in.readInt();
        initialNodes = new TreeMap<>();
        for(int i = 0; i < s; i++) {
            Node n = Node.read(in, doc);
            initialNodes.put(n.id, n);
        }

        s = in.readInt();
        for(int i = 0; i < s; i++) {
            Neuron n = Neuron.read(in, doc);
            neurons.put(n.id, n);

            if(n.label != null) {
                labeledNeurons.put(n.label, n);
            }
        }

        s = in.readInt();
        for(int i = 0; i < s; i++) {
            Neuron n = doc.m.neurons.get(in.readInt());

            while(in.readBoolean()) {
                Synapse syn = Synapse.read(in, doc);
                n.inputSynapses.add(syn);
                n.inputSynapsesByWeight.add(syn);
            }
        }
    }


    public static Model read(DataInput in) throws IOException {
        Model m = new Model();
        Document doc = m.createDocument(null, 0);
        m.readFields(in, doc);
        return m;
    }


    public static class Statistic {
        public volatile int synapses;
        public volatile int neurons;
        public volatile int neuronFromPattern;
        public volatile int nodes;
        public volatile int[] nodesPerLevel = new int[AndNode.MAX_POS_NODES + 1];
        public volatile int orNodes;
    }
}
