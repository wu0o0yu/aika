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
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


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

    public Map<Integer, Provider<? extends AbstractNode>> modifiedProviders = new TreeMap<>();
    public Map<Integer, WeakReference<Provider<? extends AbstractNode>>> providers = new TreeMap<>();

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
        this(null, 1);
    }


    public Model(SuspensionHook sh, int numberOfThreads) {
        assert numberOfThreads >= 1;
        this.numberOfThreads = numberOfThreads;

        lastCleanup = new int[numberOfThreads];
        docs = new Document[numberOfThreads];
        suspensionHook = sh;
    }


    public <T extends AbstractNode> Provider<T> createProvider(T n) {
        int id = suspensionHook != null ? suspensionHook.getNewId() : currentId.addAndGet(1);
        Provider<T> np = new Provider<T>(this, id, n);
        n.provider = np;
        synchronized (providers) {
            providers.put(id, new WeakReference<>(np));
        }
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


    public <T extends AbstractNode> Provider<T> lookupProvider(int id) {
        synchronized (providers) {
            WeakReference<Provider<? extends AbstractNode>> sp = providers.get(id);
            if(sp != null) {
                Provider p = sp.get();
                if (p != null) {
                    return p;
                }
            }

            Provider p = new Provider(this, id, null);
            providers.put(id, new WeakReference(p));
            return p;
        }
    }


    /**
     * Suspend all neurons and logic nodes whose last used document id is lower/older than {@param docId}.
     * @param docId
     */
    public void suspendUnusedNodes(int docId) {
        synchronized (providers) {
            for(Iterator<Provider<? extends AbstractNode>> it = modifiedProviders.values().iterator(); it.hasNext(); ) {
                Provider<? extends AbstractNode> p = it.next();

                if(suspend(docId, p)) {
                    it.remove();
                }
            }

            for (Iterator<WeakReference<Provider<? extends AbstractNode>>> it = providers.values().iterator(); it.hasNext(); ) {
                WeakReference<Provider<? extends AbstractNode>> sp = it.next();
                Provider<? extends AbstractNode> p = sp.get();
                if(p == null) {
                    it.remove();
                } else {
                    suspend(docId, p);
                }
            }
        }
    }


    private boolean suspend(int docId, Provider<? extends AbstractNode> p) {
        if (!p.isSuspended()) {
            if (p.get().lastUsedDocumentId <= docId) {
                p.suspend();
                return true;
            }
        }
        return false;
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


    public void resetFrequency() {
        for(int t = 0; t < numberOfThreads; t++) {
            synchronized (providers) {
                for (WeakReference<Provider<? extends AbstractNode>> sp : providers.values()) {
                    Provider<? extends AbstractNode> p = sp.get();
                    if (p != null && p.get() instanceof Node) {
                        ((Node) p.get()).frequency = 0;
                    }
                }
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


    public void addSynapse(Neuron n, double biasDelta, Input input) {
        double negDirSumDelta = 0.0;
        double negRecSumDelta = 0.0;
        double posRecSumDelta = 0.0;

        Synapse s = new Synapse(input.neuron, new Synapse.Key(
                input.weight < 0.0,
                input.recurrent,
                input.relativeRid,
                input.absoluteRid,
                input.startRangeMatch,
                input.startMapping,
                input.startRangeOutput,
                input.endRangeMatch,
                input.endMapping,
                input.endRangeOutput)
        );
        s.w = input.weight;
        s.maxLowerWeightsSum = input.maxLowerWeightsSum;

        if(input.weight < 0.0) {
            if(!input.recurrent) {
                negDirSumDelta += input.weight;
            } else {
                negRecSumDelta += input.weight;
            }
        } else if(input.recurrent) {
            posRecSumDelta += input.weight;
        }
        Neuron.addSynapse(this, defaultThreadId, n, biasDelta, negDirSumDelta, negRecSumDelta, posRecSumDelta, s);
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
            iss.w = 20.0f;
            iss.maxLowerWeightsSum = 20.0f;
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
            ctns.w = 20.0f;
            ctns.maxLowerWeightsSum = 20.0f;
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
            css.w = 20.0f;
            css.maxLowerWeightsSum = 8.0f;
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
            sss.w = 8.0f;
            sss.maxLowerWeightsSum = 0.0f;
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
        lastCycle.w = 8.0f;
        lastCycle.maxLowerWeightsSum = 0.0f;
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
        neg.w = -20.0f;
        neg.maxLowerWeightsSum = 28.0f;
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
