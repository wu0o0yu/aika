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
package network.aika.neuron;


import network.aika.*;
import network.aika.lattice.OrNode;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.range.Position;
import network.aika.neuron.range.Range;
import network.aika.neuron.activation.SearchNode;
import network.aika.lattice.InputNode;
import network.aika.neuron.relation.Relation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


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
public class INeuron extends AbstractNode<Neuron, Activation> implements Comparable<INeuron> {

    public static boolean ALLOW_WEAK_NEGATIVE_WEIGHTS = false;

    private static final Logger log = LoggerFactory.getLogger(INeuron.class);

    public static double WEIGHT_TOLERANCE = 0.001;
    public static double TOLERANCE = 0.000001;

    public String label;
    public Type type;


    public enum Type {
        EXCITATORY,
        INHIBITORY
    }


    public String outputText;

    public volatile double bias;
    public volatile double biasDelta;
    public volatile double biasSum;
    public volatile double biasSumDelta;



    public volatile double posDirSum;
    public volatile double negDirSum;
    public volatile double negRecSum;
    public volatile double posRecSum;
    public volatile double posPassiveSum;

    public volatile double requiredSum;

    public volatile int numDisjunctiveSynapses = 0;

    public Writable extension;

    public ActivationFunction activationFunction = ActivationFunction.RECTIFIED_SCALED_LOGISTIC_SIGMOID;


    public int synapseIdCounter = 0;


    // synapseId -> relation
    public Map<Integer, Relation> outputRelations;


    // A synapse is stored only in one direction, depending on the synapse weight.
    public TreeMap<Synapse, Synapse> inputSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);
    public TreeMap<Synapse, Synapse> outputSynapses = new TreeMap<>(Synapse.OUTPUT_SYNAPSE_COMP);
    public TreeMap<Synapse, Synapse> passiveInputSynapses = null;

    public Provider<InputNode> outputNode;

    public Provider<OrNode> node;


    public ReadWriteLock lock = new ReadWriteLock();


    public PassiveInputFunction passiveInputFunction = null;


    public ThreadState[] threads;

    /**
     * The {@code ThreadState} is a thread local data structure containing the activations of a single document for
     * a specific logic node.
     */
    public static class ThreadState {
        public long lastUsed;

        private TreeMap<ActKey, Activation> activations;
        private TreeMap<ActKey, Activation> activationsEnd;
        public int minLength = Integer.MAX_VALUE;
        public int maxLength = 0;


        public ThreadState() {
            activations = new TreeMap<>(BEGIN_COMP);
            activationsEnd = new TreeMap<>(END_COMP);
        }


        public void addActivation(Activation act) {
            ActKey ak = new ActKey(act.range, act.id);
            activations.put(ak, act);

            TreeMap<ActKey, Activation> actEnd = activationsEnd;
            if (actEnd != null) actEnd.put(ak, act);
        }


        public Collection<Activation> getActivations() {
            return activations.values();
        }


        public boolean isEmpty() {
            return activations.isEmpty();
        }


        public int size() {
            return activations.size();
        }


        public void clearActivations() {
            activations.clear();

            if (activationsEnd != null) activationsEnd.clear();
        }


        public Collection<Activation> getActivationsByRangeBeginLimited(Position fromKey, boolean fromInclusive, Position toKey, boolean toInclusive) {
            if(fromKey.getFinalPosition() != null && toKey.getFinalPosition() != null) {
                if(fromKey != Position.MIN) {
                    fromKey = new Position(fromKey.doc, fromKey.getFinalPosition() - maxLength);
                }

                if (fromKey.compare(Position.Operator.GREATER_THAN, toKey)) return Collections.EMPTY_LIST;

                return getActivationsByRangeBegin(fromKey, fromInclusive, toKey, toInclusive);
            } else {
                return Collections.EMPTY_LIST; // TODO:
            }
        }

        public Collection<Activation> getActivationsByRangeEndLimited(Position fromKey, boolean fromInclusive, Position toKey, boolean toInclusive) {
            if(fromKey.getFinalPosition() != null && toKey.getFinalPosition() != null) {
                if(fromKey != Position.MIN) {
                    fromKey = new Position(fromKey.doc, fromKey.getFinalPosition() - maxLength);
                }
                if (fromKey.compare(Position.Operator.GREATER_THAN, toKey)) return Collections.EMPTY_LIST;

                return getActivationsByRangeEnd(fromKey, fromInclusive, toKey, toInclusive);
            } else {
                return Collections.EMPTY_LIST; // TODO:
            }
        }


        public Collection<Activation> getActivationsByRangeBegin(Position fromKey, boolean fromInclusive, Position toKey, boolean toInclusive) {
            return activations.subMap(
                    new INeuron.ActKey(new Range(fromKey, Position.MIN), Integer.MIN_VALUE),
                    fromInclusive,
                    new INeuron.ActKey(new Range(toKey, Position.MAX), Integer.MAX_VALUE),
                    toInclusive
            ).values();
        }


        public Collection<Activation> getActivationsByRangeEnd(Position fromKey, boolean fromInclusive, Position toKey, boolean toInclusive) {
            return activationsEnd.subMap(
                    new INeuron.ActKey(new Range(Position.MIN, fromKey), Integer.MIN_VALUE),
                    fromInclusive,
                    new INeuron.ActKey(new Range(Position.MAX, toKey), Integer.MAX_VALUE),
                    toInclusive
            ).values();
        }


        public Activation getActivationByRange(Range r) {
            Map.Entry<ActKey, Activation> me = activations.higherEntry(new ActKey(r, Integer.MIN_VALUE));

            if(me != null && me.getValue().range.equals(r)) {
                return me.getValue();
            }
            return null;
        }


        public Collection<Activation> getActivations(boolean onlyFinal) {
            return onlyFinal ?
                    activations
                            .values()
                            .stream()
                            .filter(act -> act.isFinalActivation())
                            .collect(Collectors.toList()) :
                    getActivations();
        }
    }


    public static final Comparator<ActKey> BEGIN_COMP = (ak1, ak2) -> {
        int r = Range.BEGIN_COMP.compare(ak1.r, ak2.r);
        if(r != 0) return r;
        return Integer.compare(ak1.actId, ak2.actId);
    };


    public static final Comparator<ActKey> END_COMP = (ak1, ak2) -> {
        int r = Range.END_COMP.compare(ak1.r, ak2.r);
        if(r != 0) return r;
        return Integer.compare(ak1.actId, ak2.actId);
    };


    public static class ActKey {
        Range r;
        int actId;

        public ActKey(Range r, int actId) {
            this.r = r;
            this.actId = actId;
        }
    }


    public ThreadState getThreadState(int threadId, boolean create) {
        ThreadState th = threads[threadId];
        if (th == null) {
            if (!create) return null;

            th = new ThreadState();
            threads[threadId] = th;
        }
        th.lastUsed = provider.model.docIdCounter.get();
        return th;
    }



    private INeuron() {
    }


    public INeuron(Model m) {
        this(m, null);
    }


    public INeuron(Model m, String label) {
        this(m, label, null);
    }


    public INeuron(Model m, String label, String outputText) {
        this.label = label;
        this.outputText = outputText;

        if(m.getNeuronExtensionFactory() != null) {
            extension = m.getNeuronExtensionFactory().createObject();
        }

        threads = new ThreadState[m.numberOfThreads];

        provider = new Neuron(m, this);

        OrNode node = new OrNode(m);

        node.neuron = provider;
        this.node = node.provider;

        setModified();
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param input
     */
    public Activation addInput(Document doc, Activation.Builder input) {
        Range r = new Range(doc, input.begin, input.end);
        Activation act = getThreadState(doc.threadId, true).getActivationByRange(r);
        if(act == null) {
            act = new Activation(doc.activationIdCounter++, doc, node.get(doc));
            act.range = r;
        }

        register(act);

        Activation.State s = new Activation.State(input.value, input.value, 1.0, 0.0, 0.0, input.fired, 0.0);
        act.rounds.set(0, s);
        act.avgState = s;
        act.inputValue = input.value;
        act.upperBound = input.value;
        act.lowerBound = input.value;

        act.inputDecision = SearchNode.Decision.SELECTED;
        act.finalDecision = act.inputDecision;
        act.setDecision(act.inputDecision, doc.visitedCounter++);


        act.setTargetValue(input.targetValue);

        doc.inputNeuronActivations.add(act);
        doc.finallyActivatedNeurons.add(act.getINeuron());

        doc.linker.linkInput(act);

        propagate(act);

        doc.propagate();

        return act;
    }


    // TODO
    public void remove() {

        clearActivations();

        for (Synapse s : inputSynapses.values()) {
            INeuron in = s.input.get();
            in.provider.lock.acquireWriteLock();
            in.provider.inMemoryOutputSynapses.remove(s);
            in.provider.lock.releaseWriteLock();
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


    public int getNewSynapseId() {
        return synapseIdCounter++;
    }


    public void registerSynapseId(Integer synId) {
        if(synId >= synapseIdCounter) {
            synapseIdCounter = synId + 1;
        }
    }


    public void propagate(Activation act) {
        Document doc = act.doc;
        outputNode.get(doc).addActivation(act);
    }


    public Collection<Activation> getActivations(Document doc, boolean onlyFinal) {
        ThreadState th = getThreadState(doc.threadId, false);
        if (th == null) return Collections.EMPTY_LIST;
        return th.getActivations(onlyFinal);
    }


    public Activation getActivation(Document doc, Range r, boolean onlyFinal) {
        ThreadState th = getThreadState(doc.threadId, false);
        if (th == null) return null;

        if (r.begin != null) {
            for (Activation act : th.getActivationsByRangeBegin(r.begin, true, r.begin, false)) {
                if (!onlyFinal || act.isFinalActivation()) {
                    return act;
                }
            }
        } else if(r.end != null) {
            for (Activation act : th.getActivationsByRangeEnd(r.end, true, r.end, false)) {
                if (!onlyFinal || act.isFinalActivation()) {
                    return act;
                }
            }
        }
        return null;
    }


    public void clearActivations() {
        for (int i = 0; i < provider.model.numberOfThreads; i++) {
            clearActivations(i);
        }
    }


    public void clearActivations(Document doc) {
        clearActivations(doc.threadId);
    }


    public void clearActivations(int threadId) {
        ThreadState th = getThreadState(threadId, false);
        if (th == null) return;
        th.clearActivations();
    }


    public int compareTo(INeuron n) {
        if (provider.id < n.provider.id) return -1;
        else if (provider.id > n.provider.id) return 1;
        else return 0;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(true);

        out.writeBoolean(label != null);
        if(label != null) {
            out.writeUTF(label);
        }

        out.writeBoolean(type != null);
        if(type != null) {
            out.writeUTF(type.name());
        }

        out.writeBoolean(outputText != null);
        if(outputText != null) {
            out.writeUTF(outputText);
        }

        out.writeBoolean(extension != null);
        if(extension != null) {
            extension.write(out);
        }

        out.writeDouble(bias);
        out.writeDouble(biasSum);
        out.writeDouble(posDirSum);
        out.writeDouble(negDirSum);
        out.writeDouble(negRecSum);
        out.writeDouble(posRecSum);
        out.writeDouble(posPassiveSum);

        out.writeDouble(requiredSum);

        out.writeInt(numDisjunctiveSynapses);

        out.writeUTF(activationFunction.name());

        out.writeInt(outputNode.id);

        out.writeBoolean(node != null);
        if (node != null) {
            out.writeInt(node.id);
        }

        out.writeInt(synapseIdCounter);
        for (Synapse s : inputSynapses.values()) {
            if (s.input != null) {
                out.writeBoolean(true);
                s.write(out);

                out.writeBoolean(passiveInputSynapses != null && passiveInputSynapses.containsKey(s));
            }
        }
        out.writeBoolean(false);
        for (Synapse s : outputSynapses.values()) {
            if (s.output != null) {
                out.writeBoolean(true);
                s.write(out);
            }
        }
        out.writeBoolean(false);

        if(outputRelations != null) {
            out.writeInt(outputRelations.size());
            for (Map.Entry<Integer, Relation> me : outputRelations.entrySet()) {
                out.writeInt(me.getKey());
                me.getValue().write(out);
            }
        } else  {
            out.writeInt(0);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        if(in.readBoolean()) {
            label = in.readUTF();
        }

        if(in.readBoolean()) {
            type = Type.valueOf(in.readUTF());
        }

        if(in.readBoolean()) {
            outputText = in.readUTF();
        }

        if(in.readBoolean()) {
            extension = m.getNeuronExtensionFactory().createObject();
            extension.readFields(in, m);
        }

        bias = in.readDouble();
        biasSum = in.readDouble();
        posDirSum = in.readDouble();
        negDirSum = in.readDouble();
        negRecSum = in.readDouble();
        posRecSum = in.readDouble();
        posPassiveSum = in.readDouble();

        requiredSum = in.readDouble();

        numDisjunctiveSynapses = in.readInt();

        activationFunction = ActivationFunction.valueOf(in.readUTF());

        outputNode = m.lookupNodeProvider(in.readInt());

        if (in.readBoolean()) {
            Integer nId = in.readInt();
            node = m.lookupNodeProvider(nId);
        }

        synapseIdCounter = in.readInt();
        while (in.readBoolean()) {
            Synapse syn = Synapse.read(in, m);
            inputSynapses.put(syn, syn);

            if(in.readBoolean()) {
                registerPassiveInputSynapse(syn);
            }
        }

        while (in.readBoolean()) {
            Synapse syn = Synapse.read(in, m);
            outputSynapses.put(syn, syn);
        }

        int l = in.readInt();
        if(l > 0) {
            outputRelations = new TreeMap<>();
            for(int i = 0; i < l; i++) {
                Integer relId = in.readInt();
                Relation r = Relation.read(in, m);
                outputRelations.put(relId, r);
            }
        }

        passiveInputFunction = m.passiveActivationFunctions.get(provider.id);
    }


    @Override
    public void suspend() {
        for (Synapse s : inputSynapses.values()) {
            s.input.removeInMemoryOutputSynapse(s);
        }
        for (Synapse s : outputSynapses.values()) {
            s.output.removeInMemoryInputSynapse(s);
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryInputSynapses.values()) {
            if(!s.isConjunction) {
                s.input.removeInMemoryOutputSynapse(s);
            }
        }
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            if(s.isConjunction) {
                s.output.removeInMemoryInputSynapse(s);
            }
        }
        provider.lock.releaseReadLock();
    }


    @Override
    public void reactivate() {
        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryInputSynapses.values()) {
            if(!s.isConjunction) {
                s.input.addInMemoryOutputSynapse(s);
            }
        }
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            if(s.isConjunction) {
                s.output.addInMemoryInputSynapse(s);
            }
        }
        provider.lock.releaseReadLock();

        for (Synapse s : inputSynapses.values()) {
            s.input.addInMemoryOutputSynapse(s);
            if (!s.input.isSuspended()) {
                s.output.addInMemoryInputSynapse(s);
            }
        }
        for (Synapse s : outputSynapses.values()) {
            s.output.addInMemoryInputSynapse(s);
            if (!s.output.isSuspended()) {
                s.input.addInMemoryOutputSynapse(s);
            }
        }
    }

    public void setBias(double b) {
        double newBiasDelta = b - bias;
        biasSumDelta += newBiasDelta - biasDelta;
        biasDelta = newBiasDelta;
    }


    public void changeBias(double bd) {
        biasDelta += bd;
        biasSumDelta += bd;
    }


    public double getNewBiasSum() {
        return biasSum + biasSumDelta;
    }


    public void register(Activation act) {
        Document doc = act.doc;
        INeuron.ThreadState th = act.node.neuron.get().getThreadState(doc.threadId, true);

        if (th.isEmpty()) {
            doc.activatedNeurons.add(act.node.neuron.get());
        }

        Integer l = act.range.length();
        if(l != null) {
            th.minLength = Math.min(th.minLength, act.range.length());
            th.maxLength = Math.max(th.maxLength, act.range.length());
        }

        th.addActivation(act);


        act.range.begin.addBeginActivation(act);
        act.range.end.addEndActivations(act);

        doc.addActivation(act);
    }


    public static INeuron readNeuron(DataInput in, Neuron p) throws IOException {
        INeuron n = new INeuron();
        n.provider = p;
        n.threads = new ThreadState[p.model.numberOfThreads];
        n.readFields(in, p.model);
        return n;
    }


    public boolean isPassiveInputNeuron() {
        return passiveInputFunction != null;
    }


    public void registerPassiveInputSynapse(Synapse s) {
        if(passiveInputSynapses == null) {
            passiveInputSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);
        }
        passiveInputSynapses.put(s, s);
    }


    public String toString() {
        return label;
    }


    public String toStringWithSynapses() {
        SortedSet<Synapse> is = new TreeSet<>((s1, s2) -> {
            int r = Double.compare(s2.weight, s1.weight);
            if (r != 0) return r;
            return Integer.compare(s1.input.id, s2.input.id);
        });

        is.addAll(inputSynapses.values());

        StringBuilder sb = new StringBuilder();
        sb.append(toString());
        sb.append("<");
        sb.append("B:");
        sb.append(Utils.round(biasSum));
        for (Synapse s : is) {
            sb.append(", ");
            sb.append(Utils.round(s.weight));
            sb.append(":");
            sb.append(s.input.toString());
        }
        sb.append(">");
        return sb.toString();
    }
}
