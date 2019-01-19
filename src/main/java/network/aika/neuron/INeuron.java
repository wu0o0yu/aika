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
import network.aika.neuron.activation.Position;
import network.aika.neuron.activation.SearchNode;
import network.aika.lattice.InputNode;
import network.aika.neuron.relation.Relation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The {@code INeuron} class represents a internal neuron implementation in Aikas neural network and is connected to other neurons through
 * input synapses and output synapses. The activation value of a neuron is calculated by computing the weighted sum
 * (input act. value * synapse weight) of the input synapses, adding the bias to it and sending the resulting value
 * through a transfer function (the upper part of tanh).
 * <p>
 * <p>The neuron does not store its activationsBySlotAndPosition by itself. The activation objects are stored within the
 * logic nodes. To access the activationsBySlotAndPosition of this neuron simply use the member variable {@code node} or use
 * the method {@code getFinalActivations(Document doc)} to ge the final activationsBySlotAndPosition of this neuron.
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


    private String outputText;

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

    public Set<Integer> slotHasInputs = new TreeSet<>();
    public Set<Integer> slotRequired = new TreeSet<>();

    public Writable extension;

    public ActivationFunction activationFunction = ActivationFunction.RECTIFIED_SCALED_LOGISTIC_SIGMOID;


    public volatile int synapseIdCounter = 0;


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
     * The {@code ThreadState} is a thread local data structure containing the activationsBySlotAndPosition of a single document for
     * a specific logic node.
     */
    public static class ThreadState {
        public long lastUsed;

        private TreeMap<ActKey, Activation> activationsBySlotAndPosition;
        private TreeMap<Integer, Activation> activations;
        public int minLength = Integer.MAX_VALUE;
        public int maxLength = 0;


        public ThreadState() {
            activationsBySlotAndPosition = new TreeMap<>();
            activations = new TreeMap<>();
        }


        public void addActivation(Activation act) {
            for(Map.Entry<Integer, Position> me: act.slots.entrySet()) {
                ActKey ak = new ActKey(me.getKey(), me.getValue(), act.id);
                activationsBySlotAndPosition.put(ak, act);
                activations.put(act.id, act);
            }
        }


        public Stream<Activation> getActivations() {
            return activations.values().stream();
        }


        public boolean isEmpty() {
            return activationsBySlotAndPosition.isEmpty();
        }


        public int size() {
            return activations.size();
        }


        public void clearActivations() {
            activationsBySlotAndPosition.clear();
            activations.clear();
        }


        public Stream<Activation> getActivations(int fromSlot, Position fromPos, boolean fromInclusive, int toSlot, Position toPos, boolean toInclusive) {
            return activationsBySlotAndPosition.subMap(
                    new INeuron.ActKey(fromSlot, fromPos, Integer.MIN_VALUE),
                    fromInclusive,
                    new INeuron.ActKey(toSlot, toPos, Integer.MAX_VALUE),
                    toInclusive
            ).values()
                    .stream();
        }


        public Stream<Activation> getActivations(boolean onlyFinal) {
            return onlyFinal ?
                    getActivations()
                            .filter(act -> act.isFinalActivation()) :
                    getActivations();
        }

        public Collection<Activation> getActivations(SortedMap<Integer, Position> slots) {
            Integer firstSlot = slots.firstKey();
            Position firstPos = slots.get(firstSlot);

            return getActivations(firstSlot, firstPos, true, firstSlot, firstPos, true)
                    .filter( act -> {
                        for(Map.Entry<Integer, Position> me: slots.entrySet()) {
                            Position pos = me.getValue();
                            if(pos.getFinalPosition() != null && pos.compare(act.getSlot(me.getKey())) != 0) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
    }


    public static class ActKey implements Comparable<ActKey> {
        int slot;
        Position pos;
        int actId;

        public ActKey(int slot, Position pos, int actId) {
            this.slot = slot;
            this.pos = pos;
            this.actId = actId;
        }

        @Override
        public int compareTo(ActKey ak) {
            int r = Integer.compare(slot, ak.slot);
            if(r != 0) return r;
            r = pos.compare(ak.pos);
            if(r != 0) return r;
            return Integer.compare(actId, ak.actId);
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
        setOutputText(outputText);

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


    public void setOutputText(String outputText) {
        this.outputText = outputText;
        slotRequired.add(Activation.BEGIN);
        slotRequired.add(Activation.END);
    }


    public String getOutputText() {
        return outputText;
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param input
     */
    public Activation addInput(Document doc, Activation.Builder input) {
        Integer firstSlot = input.positions.firstKey();
        Position firstPos = doc.lookupFinalPosition(input.positions.get(firstSlot));
        Activation act = null;
        x: for(Activation a: getThreadState(doc.threadId, true).getActivations(firstSlot, firstPos, true, firstSlot, firstPos, true).collect(Collectors.toList())) {
            for(Map.Entry<Integer, Integer> me: input.positions.entrySet()) {
                Position pos = a.getSlot(me.getKey());
                if(pos == null || me.getValue().compareTo(pos.getFinalPosition()) != 0) {
                    continue x;
                }
            }
            act = a;
        }

        if (act == null) {
            act = new Activation(doc.activationIdCounter++, doc, node.get(doc));
            for(Map.Entry<Integer, Integer> me: input.positions.entrySet()) {
                act.setSlot(me.getKey(), doc.lookupFinalPosition(me.getValue()));
            }
        }

        register(act);

        Activation.State s = new Activation.State(input.value, input.value, 1.0, input.net, 0.0, input.fired, 0.0);
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
        doc.linker.process();

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


    public synchronized int getNewSynapseId() {
        setModified();
        return synapseIdCounter++;
    }


    public synchronized void registerSynapseId(Integer synId) {
        if(synId >= synapseIdCounter) {
            setModified();
            synapseIdCounter = synId + 1;
        }
    }


    public void propagate(Activation act) {
        Document doc = act.doc;
        outputNode.get(doc).addActivation(act);
    }


    public Stream<Activation> getActivations(Document doc, boolean onlyFinal) {
        ThreadState th = getThreadState(doc.threadId, false);
        if (th == null) return Stream.empty();
        return th.getActivations(onlyFinal);
    }


    public Stream<Activation> getActivations(Document doc, int slot, Position pos, boolean onlyFinal) {
        ThreadState th = getThreadState(doc.threadId, false);
        if (th == null) return Stream.empty();

        return th.getActivations(slot, pos, true, slot, pos, false)
                .filter(act -> !onlyFinal || act.isFinalActivation());
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

        out.writeInt(slotHasInputs.size());
        for(Integer slot: slotHasInputs) {
            out.writeInt(slot);
        }

        out.writeInt(slotRequired.size());
        for(Integer slot: slotRequired) {
            out.writeInt(slot);
        }

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

        int l = in.readInt();
        for(int i = 0; i < l; i++) {
            slotHasInputs.add(in.readInt());
        }

        l = in.readInt();
        for(int i = 0; i < l; i++) {
            slotRequired.add(in.readInt());
        }

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

        l = in.readInt();
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
            s.input.removeInMemoryOutputSynapse(s);
        }
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            s.output.removeInMemoryInputSynapse(s);
        }
        provider.lock.releaseReadLock();
    }


    @Override
    public void reactivate() {
        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryInputSynapses.values()) {
            s.input.addInMemoryOutputSynapse(s);
        }
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            s.output.addInMemoryInputSynapse(s);
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

        Integer l = act.length();
        if(l != null) {
            th.minLength = Math.min(th.minLength, l);
            th.maxLength = Math.max(th.maxLength, l);
        }

        th.addActivation(act);


        for(Map.Entry<Integer, Position> me: act.slots.entrySet()) {
            me.getValue().addActivation(me.getKey(), act);
        }

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
