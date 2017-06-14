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
package org.aika.neuron;


import org.aika.*;
import org.aika.corpus.ExpandNode;
import org.aika.corpus.Option;
import org.aika.Activation.State;
import org.aika.corpus.Range;
import org.aika.lattice.*;
import org.aika.neuron.Synapse.Key;
import org.aika.lattice.InputNode.SynapseKey;
import org.aika.Activation.SynapseActivation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Lukas Molzberger
 */
public class Neuron implements Comparable<Neuron>, Writable {

    private static final Logger log = LoggerFactory.getLogger(Neuron.class);

    public final static double LEARN_RATE = 0.01;
    public static final double WEIGHT_TOLERANCE= 0.001;
    public static final double TOLERANCE = 0.000001;
    public static final int MAX_SELF_REFERENCING_DEPTH = 5;

    public Model m;

    public static AtomicInteger currentNeuronId = new AtomicInteger(0);
    public int id = currentNeuronId.addAndGet(1);
    public String label;

    public volatile double bias;
    public volatile double negDirSum;
    public volatile double negRecSum;
    public volatile double posRecSum;

    public TreeSet<Synapse> outputSynapses = new TreeSet<>(Synapse.OUTPUT_SYNAPSE_COMP);
    public TreeSet<Synapse> inputSynapses = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);

    public TreeMap<Key, InputNode> outputNodes = new TreeMap<>();

    public Node node;
    public boolean initialized = false;

    public boolean isBlocked;
    public boolean noTraining;

    public volatile double activationSum;
    public volatile int numberOfActivations;

    public ReadWriteLock lock = new ReadWriteLock();


    public Neuron() {
    }


    public Neuron(String label) {
        this.label = label;
    }


    public Neuron(String label, boolean isBlocked, boolean noTraining) {
        this.label = label;
        this.isBlocked = isBlocked;
        this.noTraining = noTraining;
    }


    public double avgActivation() {
        return numberOfActivations > 0.0 ? activationSum / numberOfActivations : 1.0;
    }


    public static Neuron create(Iteration t, Neuron n, double bias, double negDirSum, double negRecSum, double posRecSum, Set<Synapse> inputs) {
        n.m = t.m;
        n.m.stat.neurons++;
        n.bias = bias;
        n.negDirSum = negDirSum;
        n.negRecSum = negRecSum;
        n.posRecSum = posRecSum;

        n.lock.acquireWriteLock(t.threadId);
        n.node = new OrNode(t);
        n.node.neuron = n;
        n.lock.releaseWriteLock();

        double sum = 0.0;
        for(Synapse s: inputs) {
            s.output = n;
            s.link(t);

            if(s.maxLowerWeightsSum == Double.MAX_VALUE) {
                s.maxLowerWeightsSum = sum;
            }

            sum += s.w;
        }

        if(!Node.adjust(t, n, -1)) return null;

        n.publish(t);

        n.initialized = true;
        return n;
    }


    public void publish(Iteration t) {
        m.neurons.put(id, this);
    }


    public void unpublish(Iteration t) {
        m.neurons.remove(this);
    }


    public void remove(Iteration t) {
        unpublish(t);

        for(Synapse s: inputSynapses) {
            s.input.lock.acquireWriteLock(t.threadId);
            s.input.outputSynapses.remove(s);
            s.input.lock.releaseWriteLock();
        }

        for(Synapse s: outputSynapses) {
            s.output.lock.acquireWriteLock(t.threadId);
            s.output.inputSynapses.remove(s);
            s.output.lock.releaseWriteLock();
        }
    }



    public void propagateAddedActivation(Iteration t, Activation act) {
        t.ubQueue.add(act);
    }


    public void propagateRemovedActivation(Iteration t, Activation act) {
        for(InputNode out: outputNodes.values()) {
            out.removeActivation(t, act);
        }
    }


    public void computeBounds(Activation act) {
        double ub = bias + posRecSum - (negDirSum + negRecSum);
        double lb = bias + posRecSum - (negDirSum + negRecSum);

        for (SynapseActivation sa: act.neuronInputs) {
            Synapse s = sa.s;
            Activation iAct = sa.input;

            if(iAct == act || iAct.isRemoved) continue;

            if (s.key.isNeg) {
                if (!checkSelfReferencing(act.key.o, iAct.key.o, null, 0) && act.key.o.contains(iAct.key.o, true)) {
                    ub += iAct.lowerBound * s.w;
                }

                lb += s.w;
            } else {
                ub += iAct.upperBound * s.w;
                lb += iAct.lowerBound * s.w;
            }
        }

        act.upperBound = transferFunction(ub);
        act.lowerBound = transferFunction(lb);
    }


    public State computeWeight(int round, Activation act, ExpandNode en) {
        double directSum = bias - (negDirSum + negRecSum);
        double recurrentSum = 0.0;
        double maxRecurrentSum = 0.0;

        int fired = -1;

        for (Synapse s : inputSynapses) {
            if (s.key.isRecurrent) {
                maxRecurrentSum += Math.abs(s.w);
            }
        }

        if(round == 0) {
            recurrentSum += posRecSum;
        }

        Comparator<Activation> firedComparator = new Comparator<Activation>() {
            @Override
            public int compare(Activation act1, Activation act2) {
                int c = Integer.compare(act1.rounds.get(round).fired, act2.rounds.get(round).fired);
                if(c != 0) return c;
                return act1.compareTo(act2);
            }
        };

        TreeSet<Activation> inputActs = new TreeSet<>(firedComparator);
        for(SynapseActivation sa: act.neuronInputs) {
            inputActs.add(sa.input);
        }

        ArrayList<SynapseActivation> tmp = new ArrayList<>();
        Synapse lastSynapse = null;
        SynapseActivation maxSA = null;
        for (SynapseActivation sa: act.neuronInputs) {
            if(lastSynapse != null && lastSynapse != sa.s) {
                tmp.add(maxSA);
                maxSA = null;
            }
            if(maxSA == null || maxSA.input.rounds.get(sa.s.key.isRecurrent ? round - 1 : round).value < sa.input.rounds.get(sa.s.key.isRecurrent ? round - 1 : round).value) {
                maxSA = sa;
            }
            lastSynapse = sa.s;
        }
        if(maxSA != null) {
            tmp.add(maxSA);
        }

        // Was ist mit den negativen Inputs die nach dem überschreiten des Schwellwerts gefeuert werden?
        for (SynapseActivation sa: tmp) {
            Synapse s = sa.s;

            lastSynapse = s;
            Activation iAct = sa.input;

            if (iAct == act || iAct.isRemoved) continue;

            if (s.key.isNeg && s.key.isRecurrent) {
                if (!checkSelfReferencing(act.key.o, iAct.key.o, en, 0)) {
                    if (round == 0) {
                        if (en.isCovered(iAct.key.o.markedCovered)) {
                            recurrentSum += s.w;
                        }
                    } else {
                        State is = iAct.rounds.get(round - 1);

                        recurrentSum += is.value * s.w;
                    }
                }
            } else if (s.key.isNeg && !s.key.isRecurrent) {
                State is = iAct.rounds.get(round);
                directSum += is.value * s.w;

            } else if (!s.key.isNeg && s.key.isRecurrent) {
                State is = iAct.rounds.get(round - 1);
                recurrentSum += is.value * s.w;

            } else if (!s.key.isNeg && !s.key.isRecurrent) {
                State is = iAct.rounds.get(round);
                directSum += is.value * s.w;

                if (directSum + recurrentSum >= 0.0 && fired < 0) {
                    fired = iAct.rounds.get(round).fired + 1;
                }
            }
        }

        boolean covered = en.isCovered(act.key.o.markedCovered);

        double sum = directSum + recurrentSum;

        // Compute only the recurrent part is above the threshold.
        NormWeight newWeight = NormWeight.create(
                covered ? (directSum + negRecSum) < 0.0 ? Math.max(0.0, sum) : recurrentSum - negRecSum : 0.0,
                (directSum + negRecSum) < 0.0 ? Math.max(0.0, directSum + negRecSum + maxRecurrentSum) : maxRecurrentSum
        );

        return new State(
                covered ? transferFunction(sum) : 0.0,
                covered ? fired : -1,
                newWeight
        );
    }


    public void computeErrorSignal(Iteration t, Activation act) {
        act.errorSignal = act.initialErrorSignal;
        for(SynapseActivation sa: act.neuronOutputs) {
            Synapse s = sa.s;
            Activation oAct = sa.output;

            act.errorSignal += s.w * oAct.errorSignal * (1.0 - act.finalState.value);
        }

        for(SynapseActivation sa: act.neuronInputs) {
            t.bQueue.add(sa.input);
        }
    }


    public void train(Iteration t, Activation act) {
        if(Math.abs(act.errorSignal) < TOLERANCE) return;

        long v = Activation.visitedCounter++;
        Range targetRange = null;
        if(act.key.r != null) {
            int s = act.key.r.end - act.key.r.begin;
            targetRange = new Range(Math.max(0, act.key.r.begin - (s / 2)), Math.min(t.doc.length(), act.key.r.end + (s / 2)));
        }
        ArrayList<Activation> inputActs = new ArrayList<>();
        for(Activation iAct: t.inputNodeActivations) {
            if(Range.overlaps(iAct.key.r, targetRange)) {
                inputActs.add(iAct);
            }
        }

        if(Iteration.TRAIN_DEBUG_OUTPUT) {
            log.info("Debug discover:");

            log.info("Old Synapses:");
            for(Synapse s: inputSynapses) {
                log.info("S:" + s.input + " RID:" + s.key.relativeRid + " W:" + s.w);
            }
            log.info("");
        }

        for(SynapseActivation sa: act.neuronInputs) {
            inputActs.add(sa.input);
        }

        for(Activation iAct: inputActs) {
            Integer rid = Utils.nullSafeSub(iAct.key.rid, false, act.key.rid, false);
            train(t, iAct, rid, LEARN_RATE * act.errorSignal, v);
        }

        if(Iteration.TRAIN_DEBUG_OUTPUT) {
            log.info("");
        }

        Node.adjust(t, this, act.errorSignal > 0.0 ? 1 : -1);
    }


    public void train(Iteration t, Activation iAct, Integer rid, double x, long v) {
        if(iAct.visitedNeuronTrain == v) return;
        iAct.visitedNeuronTrain = v;

        Activation iiAct = iAct.inputs.firstEntry().getValue();
        if(iiAct.key.n.neuron != this && iiAct.finalState != null && iiAct.finalState.value > TOLERANCE) {
            InputNode in = (InputNode) iAct.key.n;

            double deltaW = x * iiAct.finalState.value;

            SynapseKey sk = new SynapseKey(rid, this);
            Synapse s = in.getSynapse(sk);
            if(s == null) {
                s = new Synapse(
                        iiAct.key.n.neuron,
                        new Key(
                                in.key.isNeg,
                                in.key.isRecurrent,
                                rid,
                                null,
                                in.key.matchRange,
                                in.key.startSignal,
                                in.key.startVisibility,
                                in.key.endSignal,
                                in.key.endVisibility
                        )
                );
                s.output = this;
                in.setSynapse(t, sk, s);
                s.link(t);
            }

            inputSynapses.remove(s);

            double oldW = s.w;
            s.w -= deltaW;

            if(Iteration.TRAIN_DEBUG_OUTPUT) {
                log.info("S:" + s.input + " RID:" + s.key.relativeRid + " OldW:" + oldW + " NewW:" + s.w);
            }
            inputSynapses.add(s);
        }
    }


    private static boolean checkSelfReferencing(Option nx, Option ny, ExpandNode en, int depth) {
        if(nx == ny && (en == null || en.isCovered(ny.markedCovered))) return true;

        if(depth > MAX_SELF_REFERENCING_DEPTH) return false;

        if(ny.orOptions != null) {
            for (Option n: ny.orOptions.values()) {
                if(checkSelfReferencing(nx, n, en, depth + 1)) return true;
            }
        }

        return false;
    }


    public static double transferFunction(double x) {
        return x > 0.0 ? (2.0 * sigmoid(x)) - 1.0 : 0.0;
    }


    public static double sigmoid(double x) {
        return (1/( 1 + Math.pow(Math.E,(-x))));
    }


    public void count(Iteration t) {
        for(Activation act: node.getThreadState(t).activations.values()) {
            if(act.finalState != null && act.finalState.value > 0.0) {
                activationSum += act.finalState.value;
                numberOfActivations++;
            }
        }
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(this instanceof InputNeuron);

        out.writeInt(id);
        out.writeUTF(label);

        out.writeDouble(bias);
        out.writeDouble(negDirSum);
        out.writeDouble(negRecSum);
        out.writeDouble(posRecSum);

        out.writeInt(outputNodes.size());
        for(Map.Entry<Key, InputNode> me: outputNodes.entrySet()) {
            me.getKey().write(out);
            out.writeInt(me.getValue().id);
        }

        out.writeBoolean(node != null);
        if(node != null) {
            out.writeInt(node.id);
        }

        out.writeBoolean(isBlocked);
        out.writeBoolean(noTraining);

        out.writeDouble(activationSum);
        out.writeInt(numberOfActivations);
    }


    @Override
    public void readFields(DataInput in, Iteration t) throws IOException {
        id = in.readInt();
        label = in.readUTF();

        bias = in.readDouble();
        negDirSum = in.readDouble();
        negRecSum = in.readDouble();
        posRecSum = in.readDouble();

        int s = in.readInt();
        for(int i = 0; i < s; i++) {
            Key k = Key.read(in, t);
            InputNode n = (InputNode) t.m.initialNodes.get(in.readInt());
            outputNodes.put(k, n);
            n.inputNeuron = this;
        }

        if(in.readBoolean()) {
            node = t.m.initialNodes.get(in.readInt());
            node.neuron = this;
        }

        isBlocked = in.readBoolean();
        noTraining = in.readBoolean();

        activationSum = in.readDouble();
        numberOfActivations = in.readInt();
    }


    public static Neuron read(DataInput in, Iteration t) throws IOException {
        Neuron n = in.readBoolean() ? new InputNeuron() : new Neuron();
        n.readFields(in, t);
        return n;
    }


    @Override
    public int compareTo(Neuron n) {
        if(id < n.id) return -1;
        else if(id > n.id) return 1;
        else return 0;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("n(");
        sb.append(id);
        if(label != null) {
            sb.append(",");
            sb.append(label);
        }
        sb.append(")");
        return sb.toString();
    }


    public String toStringWithSynapses() {
        SortedSet<Synapse> is = new TreeSet<>(new Comparator<Synapse>() {
            @Override
            public int compare(Synapse s1, Synapse s2) {
                int r = Double.compare(s2.w, s1.w);
                if(r != 0) return r;
                return Integer.compare(s1.input.id, s2.input.id);
            }
        });

        is.addAll(inputSynapses);

        StringBuilder sb = new StringBuilder();
        sb.append(toString());
        sb.append("<");
        sb.append("B:");
        sb.append(Utils.round(bias));
        for(Synapse s: is) {
            sb.append(", ");
            sb.append(Utils.round(s.w));
            sb.append(":");
            sb.append(s.key.relativeRid);
            sb.append(":");
            sb.append(s.input.toString());
        }
        sb.append(">");
        return sb.toString();
    }


    public static class NormWeight {
        public final static NormWeight ZERO_WEIGHT = new NormWeight(0.0, 0.0);

        public final double w;
        public final double n;

        private NormWeight(double w, double n) {
            this.w = w;
            this.n = n;
        }

        public static NormWeight create(double w, double n) {
            if(w == 0.0 && n == 0.0) return ZERO_WEIGHT;
            return new NormWeight(w, n);
        }

        public NormWeight add(NormWeight nw) {
            if(nw == null || nw == ZERO_WEIGHT) return this;
            return new NormWeight(w + nw.w, n + nw.n);
        }

        public NormWeight sub(NormWeight nw) {
            if(nw == null || nw == ZERO_WEIGHT) return this;
            return new NormWeight(w - nw.w, n - nw.n);
        }

        public double getNormWeight() {
            return n > 0 ? w / n : 0.0;
        }


        public boolean equals(NormWeight nw) {
            return (Math.abs(w - nw.w) <= Neuron.WEIGHT_TOLERANCE && Math.abs(n - nw.n) <= Neuron.WEIGHT_TOLERANCE);
        }

        public String toString() {
            return "W:" + w + " N:" + n + " NW:" + getNormWeight();
        }
    }

}
