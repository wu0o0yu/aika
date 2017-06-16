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
import org.aika.neuron.Neuron;
import org.aika.lattice.Node;
import org.aika.neuron.Synapse;


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

    public Map<String, Neuron> labeledNeurons = Collections.synchronizedMap(new LinkedHashMap<>());
    public Map<Integer, Neuron> neurons = Collections.synchronizedMap(new TreeMap<>());

    public Map<Integer, Node> initialNodes;
    public Set<Node> allNodes[];

    public Statistic stat = new Statistic();

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
        this.numberOfThreads = numberOfThreads;

        lastCleanup = new int[numberOfThreads];
        iterationCounter = new int[numberOfThreads];
        allNodes = new Set[numberOfThreads];

        for(int i = 0; i < numberOfThreads; i++) {
            allNodes[i] = new TreeSet<>();
        }
    }


    public Iteration startIteration(Document doc, int threadId) {
        Iteration t = new Iteration(doc, this, threadId, iterationCounter[threadId]++);

        if(doc != null) {
            t.changeNumberOfPositions(doc.length());
        }

        return t;
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
    public void readFields(DataInput in, Iteration t) throws IOException {
        numberOfThreads = in.readInt();
        numberOfPositions = in.readInt();

        int s = in.readInt();
        initialNodes = new TreeMap<>();
        for(int i = 0; i < s; i++) {
            Node n = Node.read(in, t);
            initialNodes.put(n.id, n);
        }

        s = in.readInt();
        for(int i = 0; i < s; i++) {
            Neuron n = Neuron.read(in, t);
            neurons.put(n.id, n);

            if(n.label != null) {
                labeledNeurons.put(n.label, n);
            }
        }

        s = in.readInt();
        for(int i = 0; i < s; i++) {
            Neuron n = t.m.neurons.get(in.readInt());

            while(in.readBoolean()) {
                n.inputSynapses.add(Synapse.read(in, t));
            }
        }
    }


    public static Model read(DataInput in) throws IOException {
        Model m = new Model();
        Iteration t = m.startIteration(null, 0);
        m.readFields(in, t);
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
