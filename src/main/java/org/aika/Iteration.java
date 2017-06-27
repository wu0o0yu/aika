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


import org.aika.Activation.Key;
import org.aika.Activation.State;
import org.aika.Activation.SynapseActivation;
import org.aika.corpus.Document;
import org.aika.corpus.ExpandNode;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Neuron.NormWeight;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.RangeSignal;
import org.aika.neuron.Synapse.RangeVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Lukas Molzberger
 */
public class Iteration {

    private static final Logger log = LoggerFactory.getLogger(Iteration.class);

    public static boolean APPLY_DEBUG_OUTPUT = false;
    public static boolean OPTIMIZE_DEBUG_OUTPUT = false;
    public static boolean TRAIN_DEBUG_OUTPUT = false;

    public static int CLEANUP_INTERVAL = 20;

    public Document doc;
    public Model m;
    public int threadId;
    public long iterationId;

    public Queue queue = new Queue();
    public ValueQueue vQueue = new ValueQueue();
    public UpperBoundQueue ubQueue = new UpperBoundQueue();
    public BackPropagationQueue bQueue = new BackPropagationQueue();


    public TreeSet<Node> activatedNodes = new TreeSet<>();
    public TreeSet<Node> activatedNodesForTraining = new TreeSet<>();
    public TreeSet<Neuron> activatedInputNeurons = new TreeSet<>();
    public TreeSet<Neuron> activatedNeurons = new TreeSet<>();
    public TreeSet<Neuron> finallyActivatedNeurons = new TreeSet<>();
    public TreeSet<Activation> inputNeuronActivations = new TreeSet<>();
    public TreeSet<Activation> inputNodeActivations = new TreeSet<>();
    public TreeMap<Key, Activation> activationsByRid = new TreeMap<>(new Comparator<Key>() {
        @Override
        public int compare(Key act1, Key act2) {
            int r = Integer.compare(act1.rid, act2.rid);
            if(r != 0) return r;
            return act1.compareTo(act2);
        }
    });
    public TreeSet<Node> addedNodes = new TreeSet<>();

    public static int numberOfPositionsDelta;


    public static Comparator<Activation> ACTIVATIONS_OUTPUT_COMPARATOR = new Comparator<Activation>() {
        @Override
        public int compare(Activation act1, Activation act2) {
            int r = Range.compare(act1.key.r, act2.key.r, false);
            if(r != 0) return r;
            r = Utils.compareInteger(act1.key.rid, act2.key.rid);
            if(r != 0) return r;
            r = act1.key.o.compareTo(act2.key.o);
            if(r != 0) return r;
            return Integer.compare(act1.key.n.id, act2.key.n.id);
        }
    };



    Iteration(Document doc, Model m, int threadId, long iterationId) {
        this.doc = doc;
        this.m = m;
        this.threadId = threadId;
        this.iterationId = iterationId;
    }


    public void propagate() {
        boolean flag = true;
        while(flag) {
            queue.processChanges();
            flag = ubQueue.process();
        }
    }


    public void process() {
        for(Activation act: inputNeuronActivations) {
            vQueue.propagateWeight(0, act, Activation.visitedCounter++);
        }
        doc.root.computeSelectedOption(this);
    }


    public void count() {
        for(Node n: activatedNodes) {
            n.count(this);
        }

        for(Neuron n: finallyActivatedNeurons) {
            n.count(this);
        }
    }


    public void train() {
        m.numberOfPositions += numberOfPositionsDelta;
        numberOfPositionsDelta = 0;

        long v = Node.visitedCounter++;

        count();

        for(Node n: activatedNodes) {
            if(n.neuron instanceof InputNeuron) continue;

            n.computeNullHyp(m);
            if(n.frequencyHasChanged && !n.isBlocked && n.isFrequent()) {
                n.frequencyHasChanged = false;

                if(n instanceof AndNode) {
                    AndNode an = (AndNode) n;
                    an.updateWeight(this, v);
                }

                for(Activation act: n.getThreadState(this).activations.values()) {
                    n.discover(this, act);
                }
            }
        }

        while(true) {
            AndNode n = !m.numberOfPositionsQueue.isEmpty() ? m.numberOfPositionsQueue.iterator().next() : null;

            if(n == null || n.numberOfPositionsNotify > m.numberOfPositions) break;

            n.updateWeight(this, v);
        }

        bQueue.backpropagtion();

        for(Neuron n: finallyActivatedNeurons) {
            if(!n.noTraining) {
                for (Activation act : n.node.getThreadState(this).activations.values()) {
                    n.train(this, act);
                }
            }
        }
    }


    public void clearActivations() {
        for(Node n: activatedNodes) {
            n.clearActivations(this);
        }
        for(Node n: activatedNodesForTraining) {
            n.clearActivations(this);
        }
        activatedNodes.clear();
        addedNodes.clear();

        if(m.lastCleanup[threadId] + CLEANUP_INTERVAL < iterationId) {
            for (Node n : m.allNodes[threadId]) {
                Node.ThreadState th = n.threads[threadId];
                if (th != null && th.lastUsed + CLEANUP_INTERVAL < iterationId) {
                    n.threads[threadId] = null;
                }
            }
        }
    }


    public void changeNumberOfPositions(int delta) {
        numberOfPositionsDelta += delta;
    }


    public class Queue {

        public final TreeSet<Node> queue = new TreeSet<>(new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                int r = Integer.compare(n1.level, n2.level);
                if(r != 0) return r;
                return Long.compare(n1.queueId, n2.queueId);
            }
        });

        private long queueIdCounter = 0;


        public void add(Node n) {
            if(!n.isQueued) {
                n.isQueued = true;
                n.queueId = queueIdCounter++;
                queue.add(n);
            }
        }


        public void processChanges() {
            while(!queue.isEmpty()) {
                Node n = queue.pollFirst();

                n.isQueued = false;
                n.processChanges(Iteration.this);

                if(APPLY_DEBUG_OUTPUT) {
                    log.info("QueueId:" + n.queueId);
                    log.info(n.toString() + "\n");
                    log.info("\n" + networkStateToString(false, false));
                }
            }
        }
    }


    public class UpperBoundQueue {
        public final ArrayDeque<Activation> queue = new ArrayDeque<>();


        public void add(Activation act) {
            if(!act.ubQueued) {
                act.ubQueued = true;
                queue.addLast(act);
            }
        }


        public boolean process() {
            boolean flag = false;
            while(!queue.isEmpty()) {
                flag = true;
                Activation act = queue.pollFirst();
                act.ubQueued = false;

                double oldUpperBound = act.upperBound;

                Neuron n = act.key.n.neuron;

                n.computeBounds(act);

                if(Math.abs(act.upperBound - oldUpperBound) > 0.01) {
                    for(SynapseActivation sa: act.neuronOutputs) {
                        add(sa.output);
                    }
                }

                if(oldUpperBound <= 0.0 && act.upperBound > 0.0) {
                    for(InputNode out: n.outputNodes.values()) {
                        out.addActivation(Iteration.this, act);
                    }
                } else if(oldUpperBound > 0.0 && act.upperBound <= 0.0) {
                    for(InputNode out: n.outputNodes.values()) {
                        out.removeActivation(Iteration.this, act);
                    }
                }
            }
            return flag;
        }

    }


    public class ValueQueue {
        public final TreeSet<VEntry> queue = new TreeSet<>();

        public void propagateWeight(int round, Activation act, long v)  {
            for(SynapseActivation sa: act.neuronOutputs) {
                int r = sa.s.key.isRecurrent ? round + 1 : round;
                add(r, sa.output, v);
            }
        }


        public NormWeight adjustWeight(ExpandNode cand, List<Option> changed) {
            long v = Activation.visitedCounter++;

            for(Option n: changed) {
                addAllActs(n.getNeuronActivations(), v);

                // Does not need to be expanded recursively, because the activation will be propagated anyway.
                if(n.refByOrOption != null) {
                    for (Option on: n.refByOrOption) {
                        addAllActs(on.getNeuronActivations(), v);
                    }
                }
            }

            return processChanges(cand, v);
        }


        private void addAllActs(Collection<Activation> acts, long v) {
            for(Activation act: acts) {
                if(!(act.key.n.neuron instanceof InputNeuron)) {
                    add(0, act, v);
                }
            }
        }


        public void add(int round, Activation act, long v) {
            queue.add(new VEntry(round, act));
        }


        public NormWeight processChanges(ExpandNode en, long v) {
            NormWeight delta = NormWeight.ZERO_WEIGHT;
            while(!queue.isEmpty()) {
                VEntry e = queue.pollFirst();
                int round = e.round;
                Activation act = e.act;

                State s = act.key.n.neuron.computeWeight(e.round, act, en);

                if(OPTIMIZE_DEBUG_OUTPUT) {
                    log.info(act.key + " Round:" + round);
                    log.info("Value:" + s.value + "  Weight:" + s.weight.w + "  Norm:" + s.weight.n + "\n");
                }

                if(round == 0 || !act.rounds.get(round).equalsWithWeights(s)) {
                    ExpandNode.StateChange.saveOldState(en.modifiedActs, act, v);

                    State oldState = act.rounds.get(round);

                    boolean propagate = act.rounds.set(round, s);

                    ExpandNode.StateChange.saveNewState(act);

                    if(propagate) {
                        propagateWeight(round, act, v);
                    }

                    if(round == 0) {
                        // In case that there is a positive feedback loop.
                        add(1, act, v);
                    }

                    if(act.rounds.getLastRound() != null && round >= act.rounds.getLastRound()) { // Consider only the final round.
                        NormWeight oldWeight = oldState.weight;
                        delta = delta.add(s.weight.sub(oldWeight));
                    }

                }
            }
            return delta;
        }
    }

    public static class VEntry implements Comparable<VEntry> {
        public int round;
        public Activation act;


        public VEntry(int round, Activation act) {
            this.round = round;
            this.act = act;
        }

        @Override
        public int compareTo(VEntry ve) {
            int r = Integer.compare(round, ve.round);
            if(r != 0) return r;
            return act.compareTo(ve.act);
        }
    }


    public class BackPropagationQueue {

        public final TreeSet<Activation> queue = new TreeSet<>(new Comparator<Activation>() {
            @Override
            public int compare(Activation act1, Activation act2) {
                State fs1 = act1.finalState;
                State fs2 = act2.finalState;

                int r = 0;
                if(fs2 == null && fs1 != null) return -1;
                if(fs2 != null && fs1 == null) return 1;
                if(fs2 != null && fs1 != null) {
                    r = Integer.compare(fs2.fired, fs1.fired);
                }
                if(r != 0) return r;
                return act1.key.compareTo(act2.key);
            }
        });

        private long queueIdCounter = 0;


        public void add(Activation act) {
            if(!act.isQueued && !act.key.n.neuron.noTraining) {
                act.isQueued = true;
                act.queueId = queueIdCounter++;
                queue.add(act);
            }
        }


        public void backpropagtion() {
            while(!queue.isEmpty()) {
                Activation act = queue.pollFirst();

                act.isQueued = false;
                act.key.n.neuron.computeErrorSignal(Iteration.this, act);
            }
        }
    }


    public InputNeuron createOrLookupInputSignal(String label) {
        return createOrLookupInputSignal(label, false);
    }


    public InputNeuron createOrLookupInputSignal(String label, boolean isBlocked) {
        InputNeuron n = (InputNeuron) m.labeledNeurons.get(label);
        if(n == null) {
            n = InputNeuron.create(this, new InputNeuron(label, isBlocked));
            m.labeledNeurons.put(label, n);
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
        n.m = this.m;
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
                bias -= Math.abs(ni.weight) * ni.minInput;
                minWeight = Math.min(minWeight, Math.abs(ni.weight) * ni.minInput);
            }
            is.add(s);
        }
        bias += minWeight * threshold;

        return Neuron.create(this, n, bias, negDirSum, negRecSum, posRecSum, is);
    }


    public Neuron createNeuron(Neuron n, double bias, Input... inputs) {
        return createNeuron(n, bias, new TreeSet<>(Arrays.asList(inputs)));
    }


    public Neuron createNeuron(Neuron n, double bias, Collection<Input> inputs) {
        n.m = this.m;
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

        return Neuron.create(this, n, bias, negDirSum, negRecSum, posRecSum, is);
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
        n.m = this.m;
        if(n.node != null) throw new RuntimeException("This neuron has already been initialized!");

        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        double bias = -0.001;
        for(Input ni: inputs) {
            Synapse s = new Synapse(ni.neuron, new Synapse.Key(ni.weight < 0.0, ni.recurrent, ni.relativeRid, ni.absoluteRid, ni.matchRange, RangeSignal.START, ni.startVisibility, RangeSignal.END, ni.endVisibility));
            s.w = ni.weight;
            s.maxLowerWeightsSum = ni.maxLowerWeightsSum;
            is.add(s);
        }

        return Neuron.create(this, n, bias, 0.0, 0.0, 0.0, is);
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
        n.m = this.m;
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

        return Neuron.create(this, n, bias, 0.0, 0.0, 0.0, is);
    }


    /**
     * A cycle neuron is used to compute the relational id. It simply adds a new activation after each clock
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
        n.m = this.m;

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

        Neuron neuron = Neuron.create(this, n, bias, 0.0, negRecSum, 0.0, is);
        neuron.node.passive = true;
        return neuron;
    }


    public String networkStateToString(boolean withWeights) {
        return networkStateToString(true, withWeights);
    }


    public String networkStateToString(boolean neuronsOnly, boolean withWeights) {
        Set<Activation> acts = new TreeSet<>(ACTIVATIONS_OUTPUT_COMPARATOR);

        if(neuronsOnly) {
            for (Neuron n : m.neurons.values()) {
                acts.addAll(Activation.select(this, n.node, null, null, null, null, Option.Relation.CONTAINED_IN).collect(Collectors.toList()));
            }
        } else {
            if(m.initialNodes != null) {
                for (Node n : m.initialNodes.values()) {
                    acts.addAll(Activation.select(this, n, null, null, null, null, Option.Relation.CONTAINED_IN).collect(Collectors.toList()));
                }
            }
            for(int th = 0; th < m.numberOfThreads; th++) {
                for (Node n : m.allNodes[th]) {
                    acts.addAll(Activation.select(this, n, null, null, null, null, Option.Relation.CONTAINED_IN).collect(Collectors.toList()));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        NormWeight weightSum = NormWeight.ZERO_WEIGHT;
        for(Activation act: acts) {
            if (act.key.n.neuron != null && "SPACE".equals(act.key.n.neuron.label)) continue;

            sb.append(act.key.r);
            sb.append(" - ");

            sb.append(act.key.o);
            sb.append(" - ");

            sb.append(act.key.n);
            sb.append(" - Rid:");
            sb.append(act.key.rid);
            sb.append(" - UB:");
            sb.append(Utils.round(act.upperBound));
            if (withWeights) {
                if(act.key.n instanceof AndNode) {
                    AndNode an = (AndNode) act.key.n;
                    sb.append(" - BW:");
                    sb.append(an.weight);
                }

                sb.append(" - ");
                for(Map.Entry<Integer, State> me: act.rounds.rounds.entrySet()) {
                    State s = me.getValue();
                    sb.append("[R:" + me.getKey());
                    sb.append(" V:" + Utils.round(s.value));
                    sb.append(" F:" + s.fired);
                    sb.append(" W:" + Utils.round(s.weight.w));
                    sb.append(" N:" + Utils.round(s.weight.n));
                    sb.append("]");
                }

                if (act.finalState != null && act.finalState.weight != null) {
                    sb.append(" - FV:" + Utils.round(act.finalState.value));
                    sb.append(" FW:" + Utils.round(act.finalState.weight.w));
                    sb.append(" FN:" + Utils.round(act.finalState.weight.n));
                }
            }
            if (act.finalState != null && act.finalState.weight != null) {
                weightSum = weightSum.add(act.finalState.weight);
            }
            sb.append("\n");
        }
        sb.append("\nWeightSum:" + weightSum.toString() + "\n");
        return sb.toString();
    }


    public static class Input implements Comparable<Input> {
        public boolean recurrent;
        public boolean optional;
        public Neuron neuron;
        public double weight;
        public double maxLowerWeightsSum = Double.MAX_VALUE;
        public double minInput;
        public boolean matchRange = true;
        public RangeVisibility startVisibility = RangeVisibility.MAX_OUTPUT;
        public RangeVisibility endVisibility = RangeVisibility.MAX_OUTPUT;
        public RangeSignal startSignal = RangeSignal.START;
        public RangeSignal endSignal = RangeSignal.END;

        public Integer relativeRid;
        public Integer absoluteRid;


        /**
         * If recurrent is set to true, then this input will describe an feedback loop.
         * The input neuron may depend on the output of this neuron.
         *
         * @param recurrent
         * @return
         */
        public Input setRecurrent(boolean recurrent) {
            this.recurrent = recurrent;
            return this;
        }

        /**
         * If optional is set to true, then this input is an optional part of a conjunction.
         * This is only used for the method createAndNeuron.
         *
         * @param optional
         * @return
         */
        public Input setOptional(boolean optional) {
            this.optional = optional;
            return this;
        }

        /**
         * Determines the input neuron.
         *
         * @param neuron
         * @return
         */
        public Input setNeuron(Neuron neuron) {
            assert neuron != null;
            this.neuron = neuron;
            return this;
        }

        /**
         * MaxLowerWeightsSum is the expected sum of all weights smaller then the current weight. It is
         * used as an hint to compute the boolean representation of this neuron.
         *
         * @param maxLowerWeightsSum
         * @return
         */
        public Input setMaxLowerWeightsSum(double maxLowerWeightsSum) {
            this.maxLowerWeightsSum = maxLowerWeightsSum;
            return this;
        }

        /**
         * The synapse weight of this input.
         *
         * @param weight
         * @return
         */
        public Input setWeight(Double weight) {
            this.weight = weight;
            return this;
        }

        /**
         * The minimum activation value that is required for this input. The minInput
         * value is used to compute the neurons bias.
         *
         * @param minInput
         * @return
         */
        public Input setMinInput(double minInput) {
            this.minInput = minInput;
            return this;
        }

        /**
         * If the absolute relational id (rid) not null, then it is required to match the rid of input activation.
         *
         * @param absoluteRid
         * @return
         */
        public Input setAbsoluteRid(Integer absoluteRid) {
            this.absoluteRid = absoluteRid;
            return this;
        }

        /**
         * The relative relational id (rid) determines the relative position of this inputs rid with respect to
         * other inputs of this neuron.
         *
         * @param relativeRid
         * @return
         */
        public Input setRelativeRid(Integer relativeRid) {
            this.relativeRid = relativeRid;
            return this;
        }

        /**
         * If set to true then the range of this inputs activation needs to match.
         *
         * @param matchRange
         * @return
         */
        public Input setMatchRange(boolean matchRange) {
            this.matchRange = matchRange;
            return this;
        }

        /**
         * Determines if this input is used to compute the range begin of the output activation.
         *
         * @param rv
         * @return
         */
        public Input setStartVisibility(RangeVisibility rv) {
            this.startVisibility = rv;
            return this;
        }

        /**
         * Determines if this input is used to compute the range end of the output activation.
         *
         * @param rv
         * @return
         */
        public Input setEndVisibility(RangeVisibility rv) {
            this.endVisibility = rv;
            return this;
        }


        public Input setStartSignal(RangeSignal startSignal) {
            this.startSignal = startSignal;
            return this;
        }


        public Input setEndSignal(RangeSignal endSignal) {
            this.endSignal = endSignal;
            return this;
        }



        @Override
        public int compareTo(Input in) {
            int r = Double.compare(weight, in.weight);
            if(r != 0) return r;
            r = Double.compare(minInput, in.minInput);
            if(r != 0) return r;
            r = Boolean.compare(optional, in.optional);
            if(r != 0) return r;
            r = neuron.compareTo(in.neuron);
            if(r != 0) return r;
            r = Boolean.compare(matchRange, in.matchRange);
            if(r != 0) return r;
            r = Utils.compareInteger(relativeRid, in.relativeRid);
            if (r != 0) return r;
            r = Utils.compareInteger(absoluteRid, in.absoluteRid);
            if (r != 0) return r;
            r = Utils.compareInteger(startVisibility.ordinal(), in.startVisibility.ordinal());
            if (r != 0) return r;
            r = Utils.compareInteger(endVisibility.ordinal(), in.endVisibility.ordinal());
            if (r != 0) return r;
            r = Utils.compareInteger(startSignal.ordinal(), in.startSignal.ordinal());
            if (r != 0) return r;
            r = Utils.compareInteger(endSignal.ordinal(), in.endSignal.ordinal());
            return r;
        }
    }
}
