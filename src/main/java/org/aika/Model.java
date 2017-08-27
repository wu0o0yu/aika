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
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.lattice.AndNode;
import org.aika.lattice.Node;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.aika.Provider.Type.NEURON;
import static org.aika.Provider.Type.NODE;


/**
 * The model consists of two layers. The first layer is the actual neural network consisting of neurons and synapses.
 * The second layer is a pattern lattice containing a boolean logic representation of all the neurons. Whenever the
 * synapse weights of a neuron are adjusted, then the underlying boolean logic representation of this neuron will be
 * updated too.
 *
 * <p>The model supports parallel processing using a fixed number of threads.
 *
 * @author Lukas Molzberger
 */
public class Model {

    public int numberOfThreads = 1;

    public int[] lastCleanup;

    public Document[] docs;

    public SuspensionHook suspensionHook;

    public AtomicInteger currentId = new AtomicInteger(0);

    public Map<String, Provider<InputNeuron>> inputNeurons = Collections.synchronizedMap(new LinkedHashMap<>());

    public Map<Integer, Provider<? extends Neuron>> neuronsInMemory = Collections.synchronizedMap(new WeakHashMap<>());
    public Map<Integer, Provider<? extends Node>> nodesInMemory = Collections.synchronizedMap(new WeakHashMap<>());

    public Statistic stat = new Statistic();

    public int defaultThreadId = 0;

    public Set<Provider<AndNode>> numberOfPositionsQueue = Collections.synchronizedSet(new TreeSet<>(new Comparator<Provider<AndNode>>() {
        @Override
        public int compare(Provider<AndNode> n1, Provider<AndNode> n2) {
            int r = Integer.compare(n1.get().numberOfPositionsNotify, n2.get().numberOfPositionsNotify);
            if(r != 0) return r;
            return n1.compareTo(n2);
        }
    }));

    public volatile int numberOfPositions;


    /**
     * Creates a model with a single thread.
     */
    public Model() {
        this(1);
    }


    public Model(int numberOfThreads) {
        assert numberOfThreads >= 1;
        this.numberOfThreads = numberOfThreads;

        lastCleanup = new int[numberOfThreads];
        docs = new Document[numberOfThreads];
    }


    public Neuron createNeuron() {
        return createNeuronProvider(new Neuron()).get();
    }


    public Neuron createNeuron(String label) {
        return createNeuronProvider(new Neuron(label)).get();
    }


    public Neuron createNeuron(String label, boolean isBlocked, boolean noTraining) {
        return createNeuronProvider(new Neuron(label, isBlocked, noTraining)).get();
    }


    public <T extends Neuron> Provider<T> createNeuronProvider(T n) {
        int id = suspensionHook != null ? suspensionHook.getNewId() : currentId.addAndGet(1);
        Provider<T> np = new Provider<T>(this, id, NEURON, n);
        n.provider = np;
        neuronsInMemory.put(id, np);
        return np;
    }


    public <T extends Node> Provider<T> createNodeProvider(T n) {
        int id = suspensionHook != null ? suspensionHook.getNewId() : currentId.addAndGet(1);
        Provider<T> np = new Provider<T>(this, id, NODE, n);
        n.provider = np;
        nodesInMemory.put(id, np);
        return np;
    }


    public Document createDocument(String txt) {
        return createDocument(txt, 0);
    }


    public Document createDocument(String txt, int threadId) {
        Document doc = new Document(txt, this, threadId);

        if(txt != null) {
            doc.changeNumberOfPositions(doc.length());

            if(docs[threadId] != null) {
                throw new RuntimeException("Two documents are using the same thread. Call clearActivations() first, before processing the next document.");
            }
            docs[threadId] = doc;
        }

        return doc;
    }


    public <T extends Neuron> Provider<T> lookupNeuronProvider(int id) {
        Provider np = neuronsInMemory.get(id);
        if(np == null) {
            np = new Provider<>(this, id, NEURON, null);
            neuronsInMemory.put(id, np);
        }
        return np;
    }


    public <T extends Node> Provider<T> lookupNodeProvider(int id) {
        Provider np = nodesInMemory.get(id);
        if(np == null) {
            np = new Provider<>(this, id, NODE, null);
            nodesInMemory.put(id, np);
        }
        return np;
    }


    /**
     * Suspend all neurons and logic nodes whose last used document id is lower/older than {@param docId}.
     * @param docId
     */
    public void suspendUnusedNodes(int docId) {
        for(Provider<? extends Neuron> p: neuronsInMemory.values()) {
            if(!p.isSuspended()) {
                if(p.get().lastUsedDocumentId <= docId) {
                    p.suspend();
                }
            }
        }
        for(Provider<? extends Node> p: nodesInMemory.values()) {
            if(!p.isSuspended()) {
                if(p.get().lastUsedDocumentId <= docId) {
                    p.suspend();
                }
            }
        }
    }


    public void suspendAll() {
        suspendUnusedNodes(Integer.MAX_VALUE);
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Network Weights:\n");
        sb.append(networkWeightsToString(false));
        sb.append("\n\n\n");

        return sb.toString();
    }


    public void reset() {
        inputNeurons.clear();
    }


    public void resetFrequency() {
        for(int t = 0; t < numberOfThreads; t++) {
            for(Provider<? extends Node> n: nodesInMemory.values()) {
                n.get().frequency = 0;
            }
        }

    }


    public String networkWeightsToString(boolean all) {
        StringBuilder sb = new StringBuilder();
/*        for(Provider<? extends Neuron> pn: neurons.values()) {
            Neuron n = pn.get();
            if(all || n.node.get().frequency > 0) {
                sb.append(n.toStringWithSynapses());
                sb.append("\n");
            }
        }*/
        return sb.toString();
    }


    /**
     * Creates an {@code InputNeuron} with the given label.
     *
     * @param label
     * @return
     */
    public InputNeuron createOrLookupInputNeuron(String label) {
        return createOrLookupInputNeuron(label, false);
    }


    public InputNeuron createOrLookupInputNeuron(String label, boolean isBlocked) {
        Provider<InputNeuron> np = inputNeurons.get(label);

        if(np == null) {
            np = createNeuronProvider(new InputNeuron(label, isBlocked));
            InputNeuron.init(this, defaultThreadId, np.get());
            inputNeurons.put(label, np);
        }
        return np.get();
    }


    /**
     * Creates a neuron representing a conjunction of its inputs. This is just a convenience method to automatically
     * compute the bias.
     *
     * @param n
     * @param threshold A value between 0 and 1, determining how sensitive the resulting neuron will be.
     * @param inputs
     * @return
     */
    public Neuron initAndNeuron(Neuron n, double threshold, Input... inputs) {
        return initAndNeuron(n, threshold, new TreeSet<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron representing a conjunction of its inputs. This is just a convenience method to automatically
     * compute the bias.
     *
     * @param n
     * @param threshold A value between 0 and 1, determining how sensitive the resulting neuron will be.
     * @param inputs
     * @return
     */
    public Neuron initAndNeuron(Neuron n, double threshold, Collection<Input> inputs) {
        n.m = this;
        if(n.node != null) throw new RuntimeException("This neuron has already been initialized!");

        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        double bias = 0.0;
        double negDirSum = 0.0;
        double negRecSum = 0.0;
        double posRecSum = 0.0;
        double minWeight = Double.MAX_VALUE;
        for(Input ni: inputs) {
            Synapse s = new Synapse(ni.neuron, new Synapse.Key(ni.weight < 0.0, ni.recurrent, ni.relativeRid, ni.absoluteRid, ni.startRangeMatch, ni.startMapping, ni.startRangeOutput, ni.endRangeMatch, ni.endMapping, ni.endRangeOutput));
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

        return Neuron.init(this, defaultThreadId, n, bias, negDirSum, negRecSum, posRecSum, is);
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public Neuron initNeuron(Neuron n, double bias, Input... inputs) {
        return initNeuron(n, bias, new TreeSet<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public Neuron initNeuron(Neuron n, double bias, Collection<Input> inputs) {
        n.m = this;
        if(n.node != null) throw new RuntimeException("This neuron has already been initialized!");

        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        double negDirSum = 0.0;
        double negRecSum = 0.0;
        double posRecSum = 0.0;
        for(Input ni: inputs) {
            Synapse s = new Synapse(ni.neuron, new Synapse.Key(ni.weight < 0.0, ni.recurrent, ni.relativeRid, ni.absoluteRid, ni.startRangeMatch, ni.startMapping, ni.startRangeOutput, ni.endRangeMatch, ni.endMapping, ni.endRangeOutput));
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

        return Neuron.init(this, defaultThreadId, n, bias, negDirSum, negRecSum, posRecSum, is);
    }


    /**
     * Creates a neuron representing a disjunction of its inputs. This is just a convenience method to automatically
     * compute the bias.
     *
     * @param n
     * @param inputs
     * @return
     */
    public Neuron initOrNeuron(Neuron n, Input... inputs) {
        return initOrNeuron(n, new TreeSet<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron representing a disjunction of its inputs. This is just a convenience method to automatically
     * compute the bias.
     *
     * @param n
     * @param inputs
     * @return
     */
    public Neuron initOrNeuron(Neuron n, Set<Input> inputs) {
        n.m = this;
        if(n.node != null) throw new RuntimeException("This neuron has already been initialized!");

        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        double bias = -0.001;
        for(Input ni: inputs) {
            Synapse s = new Synapse(ni.neuron, new Synapse.Key(ni.weight < 0.0, ni.recurrent, ni.relativeRid, ni.absoluteRid, ni.startRangeMatch, ni.startMapping, ni.startRangeOutput, ni.endRangeMatch, ni.endMapping, ni.endRangeOutput));
            s.w = ni.weight;
            s.maxLowerWeightsSum = ni.maxLowerWeightsSum;
            is.add(s);
        }

        return Neuron.init(this, defaultThreadId, n, bias, 0.0, 0.0, 0.0, is);
    }


    /**
     * A relational neuron combines the relational id created by a counter neuron with an input signal.
     *
     * @param n
     * @param ctn
     * @param inputSignal
     * @param dirIS
     * @return
     */
    public Neuron initRelationalNeuron(Neuron n, Neuron ctn, Neuron inputSignal, boolean dirIS) {
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
                            Operator.LESS_THAN,
                            dirIS ? Mapping.END : Mapping.START,
                            false,
                            Operator.GREATER_THAN,
                            dirIS ? Mapping.START : Mapping.END,
                            false
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
                            Operator.EQUALS,
                            Mapping.START,
                            true,
                            Operator.EQUALS,
                            Mapping.END,
                            true
                    )
            );
            ctns.w = 20.0;
            ctns.maxLowerWeightsSum = 20.0;
            is.add(ctns);
        }

        return Neuron.init(this, defaultThreadId, n, bias, 0.0, 0.0, 0.0, is);
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
    public Neuron initCounterNeuron(Neuron n, Neuron clockSignal, boolean dirCS, Neuron startSignal, boolean dirSS, boolean direction) {
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
                            Operator.NONE,
                            Mapping.NONE,
                            false,
                            Operator.FIRST,
                            dirCS ? Mapping.START : Mapping.END,
                            true
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
                            Operator.EQUALS,
                            dirSS ? Mapping.START : Mapping.END,
                            true,
                            Operator.NONE,
                            Mapping.NONE,
                            false
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
                        direction ? Operator.NONE : Operator.EQUALS,
                        direction ? Mapping.NONE : Mapping.END,
                        direction ? false : true,
                        direction ? Operator.EQUALS : Operator.NONE,
                        direction ? Mapping.START : Mapping.NONE,
                        direction ? true : false
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
                        Operator.EQUALS,
                        Mapping.START,
                        false,
                        Operator.EQUALS,
                        Mapping.END,
                        false
                )
        );
        neg.w = -20.0;
        neg.maxLowerWeightsSum = 28.0;
        is.add(neg);

        return Neuron.init(this, defaultThreadId, n, bias, 0.0, negRecSum, 0.0, is);
    }


    public static class Statistic {
        public volatile int synapses;
        public volatile int neurons;
        public volatile int nodes;
        public volatile int[] nodesPerLevel = new int[AndNode.MAX_POS_NODES + 1];
        public volatile int orNodes;
    }
}
