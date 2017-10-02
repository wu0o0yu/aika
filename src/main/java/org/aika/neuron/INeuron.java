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
import org.aika.neuron.Activation.State;
import org.aika.neuron.Activation.SynapseActivation;
import org.aika.corpus.*;
import org.aika.corpus.SearchNode.Coverage;
import org.aika.corpus.Range.Operator;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.lattice.Node.ThreadState;
import org.aika.lattice.NodeActivation;
import org.aika.lattice.OrNode;
import org.aika.neuron.Synapse.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.aika.corpus.Range.Mapping.END;
import static org.aika.corpus.Range.Mapping.START;
import static org.aika.corpus.Range.Operator.*;
import static org.aika.neuron.Activation.State.*;

/**
 * The {@code INeuron} class represents a internal neuron implementation in Aikas neural network and is connected to other neurons through
 * input synapses and output synapses. The activation value of a neuron is calculated by computing the weighted sum
 * (input act. value * synapse weight) of the input synapses, adding the bias to it and sending the resulting value
 * through a transfer function (the upper part of tanh).
 * <p>
 * <p>The neuron does not store its activations by itself. The activation objects are stored within the
 * logic nodes. To access the activations of this neuron simply use the member variable {@code node} or use
 * the method {@code getFinalActivations(Document doc)} to ge the final activations of this neuron.
 *
 * @author Lukas Molzberger
 */
public class INeuron extends AbstractNode<Neuron> implements Comparable<INeuron> {

    private static final Logger log = LoggerFactory.getLogger(INeuron.class);

    public final static double LEARN_RATE = 0.01;
    public static final double WEIGHT_TOLERANCE = 0.001;
    public static final double TOLERANCE = 0.000001;
    public static final int MAX_SELF_REFERENCING_DEPTH = 5;

    public String label;

    public volatile double bias;
    public volatile double negDirSum;
    public volatile double negRecSum;
    public volatile double posRecSum;

    public volatile double maxRecurrentSum = 0.0;


    public TreeMap<Synapse, Synapse> inputSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);

    public TreeMap<Key, Provider<InputNode>> outputNodes = new TreeMap<>();

    public Provider<OrNode> node;

    public boolean isBlocked;
    public boolean noTraining;

    public volatile double activationSum;
    public volatile int numberOfActivations;

    public ReadWriteLock lock = new ReadWriteLock();


    private INeuron() {
    }


    public INeuron(Model m) {
        this(m, null);
    }


    public INeuron(Model m, String label) {
        this(m, label, false, false);
    }


    public INeuron(Model m, String label, boolean isBlocked, boolean noTraining) {
        this.label = label;
        this.isBlocked = isBlocked;
        this.noTraining = noTraining;

        provider = new Neuron(m, this);

        OrNode node = new OrNode(m);

        node.neuron = provider;
        this.node = node.provider;

        provider.setModified();
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     * @param rid   The relational id (e.g. the word position)
     * @param o     The interpretation node
     * @param value The activation value of this input activation
     */
    public Activation addInput(Document doc, int begin, int end, Integer rid, InterprNode o, double value) {
        if (value <= 0.0) return null;

        Node.addActivationAndPropagate(doc, new NodeActivation.Key(node.get(), new Range(begin, end), rid, o), Collections.emptySet());

        doc.propagate();

        Activation act = NodeActivation.get(doc, node.get(), rid, new Range(begin, end), EQUALS, EQUALS, o, InterprNode.Relation.EQUALS);
        State s = new State(value, value, value, 0, NormWeight.ZERO_WEIGHT, NormWeight.ZERO_WEIGHT);
        act.rounds.set(0, s);
        act.finalState = s;
        act.upperBound = value;
        act.isInput = true;

        doc.inputNeuronActivations.add(act);
        doc.finallyActivatedNeurons.add(act.key.n.neuron.get());

        doc.ubQueue.add(act);

        doc.propagate();

        return act;
    }


    public void removeInput(Document doc, int begin, int end, Integer rid, InterprNode o) {
        Range r = new Range(begin, end);
        NodeActivation act = NodeActivation.get(doc, node.get(), rid, r, EQUALS, EQUALS, o, InterprNode.Relation.EQUALS);
        Node.removeActivationAndPropagate(doc, act, Collections.emptySet());

        doc.propagate();
        doc.inputNeuronActivations.remove(act);
    }


    public double avgActivation() {
        return numberOfActivations > 0.0 ? activationSum / numberOfActivations : 1.0;
    }


    public void publish(int threadId) {
    }


    public void unpublish(int threadId) {
    }


    public void remove(int threadId) {
        unpublish(threadId);

        for (Synapse s : inputSynapses.values()) {
            INeuron in = s.input.get();
            in.lock.acquireWriteLock();
            in.provider.inMemoryOutputSynapses.remove(s);
            in.lock.releaseWriteLock();
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            INeuron out = s.output.get();
            out.lock.acquireWriteLock();
            out.inputSynapses.remove(s);
            out.lock.releaseWriteLock();
        }
        provider.lock.releaseReadLock();
    }


    public void propagateAddedActivation(Document doc, Activation act) {
        doc.ubQueue.add(act);
    }


    public void propagateRemovedActivation(Document doc, NodeActivation act) {
        for (Provider<InputNode> out : outputNodes.values()) {
            out.get().removeActivation(doc, act);
        }
    }


    public void computeBounds(Activation act) {
        double ub = bias + posRecSum - (negDirSum + negRecSum);
        double lb = bias + posRecSum - (negDirSum + negRecSum);

        for (SynapseActivation sa : act.neuronInputs) {
            Synapse s = sa.s;
            Activation iAct = sa.input;

            if (iAct == act || iAct.isRemoved) continue;

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


    public State computeWeight(int round, Activation act, SearchNode sn, Document doc) {
        InterprNode o = act.key.o;
        double st = bias - (negDirSum + negRecSum);
        double[][] sum = {{st, st, st}, {0.0, 0.0, 0.0}};

        int fired = -1;

        for (SynapseActivation sa : getInputSAs(act, round)) {
            Synapse s = sa.s;

            Activation iAct = sa.input;
            InterprNode io = iAct.key.o;

            if (iAct == act || iAct.isRemoved) continue;

            State is = State.ZERO;
            if (s.key.isRecurrent) {
                if (!s.key.isNeg || !checkSelfReferencing(o, io, sn, 0)) {
                    is = round == 0 ? getInitialState(sn.getCoverage(io)) : iAct.rounds.get(round - 1);
                }
            } else {
                is = iAct.rounds.get(round);
            }

            int t = s.key.isRecurrent ? REC : DIR;
            sum[t][VALUE] += is.value * s.w;
            sum[t][UB] += (s.key.isNeg ? is.lb : is.ub) * s.w;
            sum[t][LB] += (s.key.isNeg ? is.ub : is.lb) * s.w;

            if (!s.key.isRecurrent && !s.key.isNeg && sum[DIR][VALUE] + sum[REC][VALUE] >= 0.0 && fired < 0) {
                fired = iAct.rounds.get(round).fired + 1;
            }
        }


        double drSum = sum[DIR][VALUE] + sum[REC][VALUE];
        double drSumUB = sum[DIR][UB] + sum[REC][UB];
        double drSumLB = sum[DIR][LB] + sum[REC][LB];

        Coverage c = sn.getCoverage(act.key.o);
        // Compute only the recurrent part is above the threshold.
        NormWeight newWeight = NormWeight.create(
                c == Coverage.SELECTED ? (sum[DIR][VALUE] + negRecSum) < 0.0 ? Math.max(0.0, drSum) : sum[REC][VALUE] - negRecSum : 0.0,
                (sum[DIR][VALUE] + negRecSum) < 0.0 ? Math.max(0.0, sum[DIR][VALUE] + negRecSum + maxRecurrentSum) : maxRecurrentSum
        );
        NormWeight newWeightUB = NormWeight.create(
                c == Coverage.SELECTED || c == Coverage.UNKNOWN ? (sum[DIR][UB] + negRecSum) < 0.0 ? Math.max(0.0, drSumUB) : sum[REC][UB] - negRecSum : 0.0,
                (sum[DIR][LB] + negRecSum) < 0.0 ? Math.max(0.0, sum[DIR][LB] + negRecSum + maxRecurrentSum) : maxRecurrentSum
        );

        if (doc.debugActId == act.id && doc.debugActWeight <= newWeight.w) {
            storeDebugOutput(doc, act, newWeight, drSum, round);
        }

        return new State(
                c == Coverage.SELECTED ? transferFunction(drSum) : 0.0,
                c == Coverage.SELECTED || c == Coverage.UNKNOWN ? transferFunction(drSumUB) : 0.0,
                c == Coverage.SELECTED ? transferFunction(drSumLB) : 0.0,
                c == Coverage.SELECTED ? fired : -1,
                newWeight,
                newWeightUB
        );
    }


    private State getInitialState(Coverage c) {
        return new State(
                c == Coverage.SELECTED ? 1.0 : 0.0,
                c == Coverage.SELECTED || c == Coverage.UNKNOWN ? 1.0 : 0.0,
                c == Coverage.SELECTED ? 1.0 : 0.0,
                0,
                NormWeight.ZERO_WEIGHT,
                NormWeight.ZERO_WEIGHT
        );
    }


    private List<SynapseActivation> getInputSAs(Activation act, int round) {
        ArrayList<SynapseActivation> tmp = new ArrayList<>();
        Synapse lastSynapse = null;
        SynapseActivation maxSA = null;
        for (SynapseActivation sa : act.neuronInputs) {
            if (lastSynapse != null && lastSynapse != sa.s) {
                tmp.add(maxSA);
                maxSA = null;
            }
            if (maxSA == null || maxSA.input.rounds.get(sa.s.key.isRecurrent ? round - 1 : round).value < sa.input.rounds.get(sa.s.key.isRecurrent ? round - 1 : round).value) {
                maxSA = sa;
            }
            lastSynapse = sa.s;
        }
        if (maxSA != null) {
            tmp.add(maxSA);
        }

        return tmp;
    }


    private void storeDebugOutput(Document doc, Activation act, NormWeight nw, double sum, int round) {
        StringBuilder sb = new StringBuilder();
        sb.append("Activation ID: " + doc.debugActId + "\n");
        sb.append("Neuron: " + label + "\n");
        sb.append("Sum: " + sum + "\n");
        sb.append("Bias: " + bias + "\n");
        sb.append("Round: " + round + "\n");
        sb.append("Positive Recurrent Sum: " + posRecSum + "\n");
        sb.append("Negative Recurrent Sum: " + negRecSum + "\n");
        sb.append("Negative Direct Sum: " + negDirSum + "\n");
        sb.append("Inputs:\n");

        for (SynapseActivation sa : getInputSAs(act, round)) {
            String actValue = "";
            if (sa.s.key.isRecurrent) {
                if (round > 0) {
                    actValue = "" + sa.input.rounds.get(round - 1);
                }
            } else {
                actValue = "" + sa.input.rounds.get(round);
            }

            sb.append("    " + sa.input.key.n.neuron.get().label + "  SynWeight: " + sa.s.w + "  ActValue: " + actValue);
            sb.append("\n");
        }
        sb.append("Weight: " + nw.w + "\n");
        sb.append("Norm: " + nw.n + "\n");
        sb.append("\n");
        doc.debugOutput = sb.toString();
    }


    public void computeErrorSignal(Document doc, Activation act) {
        act.errorSignal = act.initialErrorSignal;
        for (SynapseActivation sa : act.neuronOutputs) {
            Synapse s = sa.s;
            Activation oAct = sa.output;

            act.errorSignal += s.w * oAct.errorSignal * (1.0 - act.finalState.value);
        }

        for (SynapseActivation sa : act.neuronInputs) {
            doc.bQueue.add(sa.input);
        }
    }


    public void train(Document doc, Activation act) {
        if (Math.abs(act.errorSignal) < TOLERANCE) return;

        long v = NodeActivation.visitedCounter++;
        Range targetRange = null;
        if (act.key.r != null) {
            int s = act.key.r.end - act.key.r.begin;
            targetRange = new Range(Math.max(0, act.key.r.begin - (s / 2)), Math.min(doc.length(), act.key.r.end + (s / 2)));
        }
        ArrayList<Activation> inputActs = new ArrayList<>();
        for (INeuron n : doc.finallyActivatedNeurons) {
            for (Activation iAct : n.getFinalActivations(doc)) {
                if (Range.overlaps(iAct.key.r, targetRange)) {
                    inputActs.add(iAct);
                }
            }
        }

        if (Document.TRAIN_DEBUG_OUTPUT) {
            log.info("Debug discover:");

            log.info("Old Synapses:");
            for (Synapse s : inputSynapses.values()) {
                log.info("S:" + s.input + " RID:" + s.key.relativeRid + " W:" + s.w);
            }
            log.info("");
        }

        for (SynapseActivation sa : act.neuronInputs) {
            inputActs.add(sa.input);
        }

        for (Activation iAct : inputActs) {
            Integer rid = Utils.nullSafeSub(iAct.key.rid, false, act.key.rid, false);
            train(doc, iAct, rid, LEARN_RATE * act.errorSignal, v);
        }

        if (Document.TRAIN_DEBUG_OUTPUT) {
            log.info("");
        }

        Node.adjust(doc.m, doc.threadId, this, act.errorSignal > 0.0 ? 1 : -1, inputSynapses.values());
    }


    public void train(Document doc, Activation iAct, Integer rid, double x, long v) {
        if (iAct.visitedNeuronTrain == v) return;
        iAct.visitedNeuronTrain = v;

        INeuron n = iAct.key.n.neuron.get();
        double deltaW = x * iAct.finalState.value;

/*        SynapseKey sk = new SynapseKey(rid, provider);
        Synapse s = in.getSynapse(sk);
        if(s == null) {
            s = new Synapse(
                    n,
                    new Key(
                            in.key.isNeg,
                            in.key.isRecurrent,
                            rid,
                            null,
                            in.key.startRangeMatch,
                            in.key.startRangeMapping,
                            in.key.startRangeOutput,
                            in.key.endRangeMatch,
                            in.key.endRangeMapping,
                            in.key.endRangeOutput
                    )
            );
            s.output = provider;
            in.setSynapse(doc.threadId, sk, s);
            s.link(doc.threadId);
        }

        inputSynapses.remove(s);
        inputSynapsesByWeight.remove(s);

        double oldW = s.w;
        s.w -= deltaW;

        if(Document.TRAIN_DEBUG_OUTPUT) {
            log.info("S:" + s.input + " RID:" + s.key.relativeRid + " OldW:" + oldW + " NewW:" + s.w);
        }
        inputSynapses.put(s, s);
        inputSynapsesByWeight.add(s);
        */
    }


    private static boolean checkSelfReferencing(InterprNode nx, InterprNode ny, SearchNode en, int depth) {
        if (nx == ny && (en == null || en.isCovered(ny.markedSelected))) return true;

        if (depth > MAX_SELF_REFERENCING_DEPTH) return false;

        if (ny.orInterprNodes != null) {
            for (InterprNode n : ny.orInterprNodes.values()) {
                if (checkSelfReferencing(nx, n, en, depth + 1)) return true;
            }
        }

        return false;
    }


    public static double transferFunction(double x) {
        return x > 0.0 ? (2.0 * sigmoid(x)) - 1.0 : 0.0;
    }


    public static double sigmoid(double x) {
        return (1 / (1 + Math.pow(Math.E, (-x))));
    }


    public void count(Document doc) {
        ThreadState<OrNode, Activation> th = node.get().getThreadState(doc.threadId, false);
        if (th == null) return;
        for (Activation act : th.activations.values()) {
            if (act.finalState != null && act.finalState.value > 0.0) {
                activationSum += act.finalState.value;
                numberOfActivations++;
            }
        }
    }

    /**
     * Sets the incoming and outgoing links between neuron activations.
     *
     * @param doc
     * @param act
     */
    public void linkNeuronRelations(Document doc, Activation act) {
        int v = doc.visitedCounter++;
        lock.acquireReadLock();
        linkNeuronActs(doc, act, v, 0);
        linkNeuronActs(doc, act, v, 1);
        lock.releaseReadLock();
    }


    private void linkNeuronActs(Document doc, Activation act, int v, int dir) {
        ArrayList<Activation> recNegTmp = new ArrayList<>();

        provider.lock.acquireReadLock();
        NavigableMap<Synapse, Synapse> syns = (dir == 0 ? provider.inMemoryInputSynapses : provider.inMemoryOutputSynapses);

        for (Synapse s : getActiveSynapses(doc, dir, syns)) {
            Neuron p = (dir == 0 ? s.input : s.output);
            INeuron an = p.getIfNotSuspended();
            if (an != null) {
                OrNode n = an.node.get();
                ThreadState th = n.getThreadState(doc.threadId, false);
                if (th == null || th.activations.isEmpty()) continue;

                linkActSyn(n, doc, act, dir, recNegTmp, s);
            }
        }
        provider.lock.releaseReadLock();

        for (Activation rAct : recNegTmp) {
            Activation oAct = (dir == 0 ? act : rAct);
            Activation iAct = (dir == 0 ? rAct : act);

            markConflicts(iAct, oAct, v);

            addConflict(doc, oAct.key.o, iAct.key.o, iAct, Collections.singleton(act), v);
        }
    }


    private static void linkActSyn(OrNode n, Document doc, Activation act, int dir, ArrayList<Activation> recNegTmp, Synapse s) {
        Synapse.Key sk = s.key;

        Integer rid;
        if (dir == 0) {
            rid = sk.absoluteRid != null ? sk.absoluteRid : Utils.nullSafeAdd(act.key.rid, false, sk.relativeRid, false);
        } else {
            rid = Utils.nullSafeSub(act.key.rid, false, sk.relativeRid, false);
        }


        Operator begin = replaceFirstAndLast(sk.startRangeMatch);
        Operator end = replaceFirstAndLast(sk.endRangeMatch);
        Range r = act.key.r;
        if (dir == 0) {
            begin = Operator.invert(sk.startRangeMapping == START ? begin : (sk.endRangeMapping == START ? end : NONE));
            end = Operator.invert(sk.endRangeMapping == END ? end : (sk.startRangeMapping == END ? begin : NONE));

            if (sk.startRangeMapping != START || sk.endRangeMapping != END) {
                r = new Range(s.key.endRangeMapping == START ? r.end : (sk.startRangeMapping == START ? r.begin : null), sk.startRangeMapping == END ? r.begin : (sk.endRangeMapping == END ? r.end : null));
            }
        } else {
            if (sk.startRangeMapping != START || sk.endRangeMapping != END) {
                r = new Range(sk.startRangeMapping == END ? r.end : (sk.startRangeMapping == START ? r.begin : null), sk.endRangeMapping == START ? r.begin : (sk.endRangeMapping == END ? r.end : null));
            }
        }

        Stream<Activation> tmp = NodeActivation.select(
                doc,
                n,
                rid,
                r,
                begin,
                end,
                null,
                null
        );

        final int d = dir;
        tmp.forEach(rAct -> {
            Activation oAct = (d == 0 ? act : rAct);
            Activation iAct = (d == 0 ? rAct : act);

            SynapseActivation sa = new SynapseActivation(s, iAct, oAct);
            iAct.addSynapseActivation(0, sa);
            oAct.addSynapseActivation(1, sa);

            if (sk.isNeg && sk.isRecurrent) {
                recNegTmp.add(rAct);
            }
        });
    }


    private static Collection<Synapse> getActiveSynapses(Document doc, int dir, NavigableMap<Synapse, Synapse> syns) {
        // Optimization in case the set of synapses is very large
        if (syns.size() < 10 || doc.activatedNeurons.size() * 20 > syns.size()) {
            return syns.values();
        }

        Collection<Synapse> synsTmp;
        ArrayList<Synapse> newSyns = new ArrayList<>();
        Synapse lk = new Synapse(null, Synapse.Key.MIN_KEY);
        Synapse uk = new Synapse(null, Synapse.Key.MAX_KEY);

        for (INeuron n : doc.activatedNeurons) {
            if (dir == 0) {
                lk.input = n.provider;
                uk.input = n.provider;
            } else {
                lk.output = n.provider;
                uk.output = n.provider;
            }

            // Using addAll is not efficient here.
            for (Synapse s : syns.subMap(lk, true, uk, true).values()) {
                newSyns.add(s);
            }
        }

        synsTmp = newSyns;
        return synsTmp;
    }


    private static Operator replaceFirstAndLast(Operator rm) {
        return rm == FIRST || rm == LAST ? EQUALS : rm;
    }


    public static void unlinkNeuronRelations(Document doc, Activation act) {
        int v = doc.visitedCounter++;
        for (int dir = 0; dir < 2; dir++) {
            for (SynapseActivation sa : (dir == 0 ? act.neuronInputs : act.neuronOutputs)) {
                Synapse s = sa.s;
                Activation rAct = dir == 0 ? sa.input : sa.output;

                if (s.key.isNeg && s.key.isRecurrent) {
                    Activation oAct = (dir == 0 ? act : rAct);
                    Activation iAct = (dir == 0 ? rAct : act);

                    markConflicts(iAct, oAct, v);

                    removeConflict(doc, oAct.key.o, iAct.key.o, iAct, act, v);
                }
            }
        }

        for (int dir = 0; dir < 2; dir++) {
            for (SynapseActivation sa : (dir == 0 ? act.neuronInputs : act.neuronOutputs)) {
                Activation rAct = dir == 0 ? sa.input : sa.output;
                rAct.removeSynapseActivation(dir, sa);
            }
        }
    }


    private static void addConflict(Document doc, InterprNode io, InterprNode o, NodeActivation act, Collection<NodeActivation> inputActs, long v) {
        if (o.markedConflict == v || o.orInterprNodes == null) {
            if (!isAllowed(doc.threadId, io, o, inputActs)) {
                Conflicts.add(doc, act, io, o);
            }
        } else {
            for (InterprNode no : o.orInterprNodes.values()) {
                addConflict(doc, io, no, act, inputActs, v);
            }
        }
    }


    private static boolean isAllowed(int threadId, InterprNode io, InterprNode o, Collection<NodeActivation> inputActs) {
        if (io != null && o.contains(io, false)) return true;
        for (NodeActivation act : inputActs) {
            if (act.key.n.isAllowedOption(threadId, o, act, Node.visitedCounter++)) return true;
        }
        return false;
    }


    private static void removeConflict(Document doc, InterprNode io, InterprNode o, NodeActivation act, NodeActivation nAct, long v) {
        if (o.markedConflict == v || o.orInterprNodes == null) {
            if (!nAct.key.n.isAllowedOption(doc.threadId, o, nAct, Node.visitedCounter++)) {
                assert io != null;

                Conflicts.remove(doc, act, io, o);
            }
        } else {
            for (InterprNode no : o.orInterprNodes.values()) {
                removeConflict(doc, io, no, act, nAct, v);
            }
        }
    }


    private static void markConflicts(Activation iAct, Activation oAct, int v) {
        oAct.key.o.markedConflict = v;
        for (SynapseActivation sa : iAct.neuronOutputs) {
            if (sa.s.key.isRecurrent && sa.s.key.isNeg) {
                sa.output.key.o.markedConflict = v;
            }
        }
    }


    public Synapse getInputSynapse(Synapse s) {
        return inputSynapses.getOrDefault(s, s);
    }


    public int compareTo(INeuron n) {
        if (provider.id < n.provider.id) return -1;
        else if (provider.id > n.provider.id) return 1;
        else return 0;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(true);

        out.writeUTF(label);

        out.writeDouble(bias);
        out.writeDouble(negDirSum);
        out.writeDouble(negRecSum);
        out.writeDouble(posRecSum);
        out.writeDouble(maxRecurrentSum);

        out.writeInt(outputNodes.size());
        for (Map.Entry<Key, Provider<InputNode>> me : outputNodes.entrySet()) {
            me.getKey().write(out);
            out.writeInt(me.getValue().id);
        }

        out.writeBoolean(node != null);
        if (node != null) {
            out.writeInt(node.id);
        }

        out.writeBoolean(isBlocked);
        out.writeBoolean(noTraining);

        out.writeDouble(activationSum);
        out.writeInt(numberOfActivations);

        for (Synapse s : inputSynapses.values()) {
            if (s.input != null) {
                out.writeBoolean(true);
                s.write(out);
            }
        }
        out.writeBoolean(false);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        label = in.readUTF();

        bias = in.readDouble();
        negDirSum = in.readDouble();
        negRecSum = in.readDouble();
        posRecSum = in.readDouble();
        maxRecurrentSum = in.readDouble();

        int s = in.readInt();
        for (int i = 0; i < s; i++) {
            Key k = Key.read(in, m);
            Provider<InputNode> n = m.lookupNodeProvider(in.readInt());
            outputNodes.put(k, n);
        }

        if (in.readBoolean()) {
            Integer nId = in.readInt();
            node = m.lookupNodeProvider(nId);
        }

        isBlocked = in.readBoolean();
        noTraining = in.readBoolean();

        activationSum = in.readDouble();
        numberOfActivations = in.readInt();

        while (in.readBoolean()) {
            Synapse syn = Synapse.read(in, m);

            inputSynapses.put(syn, syn);
        }
    }


    @Override
    public void suspend() {
        for (Synapse s : inputSynapses.values()) {
            s.input.inMemoryOutputSynapses.remove(s);
            InputNode iNode = s.inputNode.getIfNotSuspended();
            if (iNode != null) {
                iNode.removeSynapse(s);
            }
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            s.output.lock.acquireWriteLock();
            s.output.inMemoryInputSynapses.remove(s);
            s.output.lock.releaseWriteLock();
        }
        provider.lock.releaseReadLock();
    }


    @Override
    public void reactivate() {
        for (Synapse s : inputSynapses.values()) {
            s.input.lock.acquireWriteLock();
            s.input.inMemoryOutputSynapses.put(s, s);
            s.input.lock.releaseWriteLock();

            InputNode iNode = s.inputNode.getIfNotSuspended();
            if (iNode != null) {
                iNode.setSynapse(s);
            }
        }
        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            s.output.lock.acquireWriteLock();
            s.output.inMemoryInputSynapses.put(s, s);
            s.output.lock.releaseWriteLock();
        }
        provider.lock.releaseReadLock();
    }


    public static Neuron init(Model m, int threadId, Neuron pn, double bias, double negDirSum, double negRecSum, double posRecSum, Set<Synapse> inputs) {
        INeuron n = pn.get();
        n.provider.m.stat.neurons++;
        n.bias = bias;
        n.negDirSum = negDirSum;
        n.negRecSum = negRecSum;
        n.posRecSum = posRecSum;

        float sum = 0.0f;
        ArrayList<Synapse> modifiedSynapses = new ArrayList<>();
        for (Synapse s : inputs) {
            assert !s.key.startRangeOutput || s.key.startRangeMatch == Range.Operator.EQUALS || s.key.startRangeMatch == Range.Operator.FIRST;
            assert !s.key.endRangeOutput || s.key.endRangeMatch == Range.Operator.EQUALS || s.key.endRangeMatch == Range.Operator.FIRST;

            s.output = n.provider;
            s.link();

            if (s.maxLowerWeightsSum == Float.MAX_VALUE) {
                s.maxLowerWeightsSum = sum;
            }

            sum += s.w;
            modifiedSynapses.add(s);
        }

        if (!Node.adjust(m, threadId, n, -1, modifiedSynapses)) return null;

        n.publish(threadId);

        return n.provider;
    }


    public static INeuron addSynapse(Model m, int threadId, Neuron pn, double biasDelta, double negDirSumDelta, double negRecSumDelta, double posRecSumDelta, Synapse s) {
        INeuron n = pn.get();
        n.bias += biasDelta;
        n.negDirSum += negDirSumDelta;
        n.negRecSum += negRecSumDelta;
        n.posRecSum += posRecSumDelta;

        s.output = n.provider;
        s.link();

        if (!Node.adjust(m, threadId, n, -1, Collections.singletonList(s))) return null;
        return n;
    }


    public static INeuron readNeuron(DataInput in, Neuron p) throws IOException {
        INeuron n = new INeuron();
        n.provider = p;
        n.readFields(in, p.m);
        return n;
    }


    public String toString() {
        return "n(" + label + ")";
    }


    public String toStringWithSynapses() {
        SortedSet<Synapse> is = new TreeSet<>(new Comparator<Synapse>() {
            @Override
            public int compare(Synapse s1, Synapse s2) {
                int r = Double.compare(s2.w, s1.w);
                if (r != 0) return r;
                return Integer.compare(s1.input.id, s2.input.id);
            }
        });

        is.addAll(inputSynapses.values());

        StringBuilder sb = new StringBuilder();
        sb.append(toString());
        sb.append("<");
        sb.append("B:");
        sb.append(Utils.round(bias));
        for (Synapse s : is) {
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


    /**
     * {@code getFinalActivations} is a convenience method to retrieve all activations of the given neuron that
     * are part of the final interpretation. Before calling this method, the {@code doc.process()} needs to
     * be called first.
     *
     * @param doc The current document
     * @return A collection with all final activations of this neuron.
     */
    public Collection<Activation> getFinalActivations(Document doc) {
        Stream<Activation> s = NodeActivation.select(doc, node.get(), null, null, null, null, null, null);
        return s.filter(act -> act.finalState != null && act.finalState.value > 0.0)
                .collect(Collectors.toList());
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
            assert w >= 0.0 && n >= 0.0;
            if (w == 0.0 && n == 0.0) return ZERO_WEIGHT;
            return new NormWeight(w, n);
        }

        public NormWeight add(NormWeight nw) {
            if (nw == null || nw == ZERO_WEIGHT) return this;
            return new NormWeight(w + nw.w, n + nw.n);
        }

        public NormWeight sub(NormWeight nw) {
            if (nw == null || nw == ZERO_WEIGHT) return this;
            return new NormWeight(w - nw.w, n - nw.n);
        }

        public double getNormWeight() {
            return n > 0 ? w / n : 0.0;
        }


        public boolean equals(NormWeight nw) {
            return (Math.abs(w - nw.w) <= INeuron.WEIGHT_TOLERANCE && Math.abs(n - nw.n) <= INeuron.WEIGHT_TOLERANCE);
        }

        public String toString() {
            return "W:" + w + " N:" + n + " NW:" + getNormWeight();
        }
    }

}
