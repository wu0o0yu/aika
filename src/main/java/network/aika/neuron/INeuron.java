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
import network.aika.neuron.activation.Activation.Option;
import network.aika.neuron.activation.Position;
import network.aika.neuron.activation.SearchNode;
import network.aika.lattice.InputNode;
import network.aika.neuron.relation.Relation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.SearchNode.Decision.SELECTED;

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
public class INeuron extends AbstractNode<Neuron> implements Comparable<INeuron> {

    private static final Logger log = LoggerFactory.getLogger(INeuron.class);

    public static boolean ALLOW_WEAK_NEGATIVE_WEIGHTS = false;
    public static double WEIGHT_TOLERANCE = 0.001;


    public static final INeuron MIN_NEURON = new INeuron();
    public static final INeuron MAX_NEURON = new INeuron();

    String label;
    Type type;


    public enum Type {
        INPUT,
        EXCITATORY,
        INHIBITORY
    }


    private String outputText;

    private volatile double bias;
    private volatile double biasDelta;

    private SynapseSummary synapseSummary = new SynapseSummary();

    public Set<Integer> slotHasInputs = new TreeSet<>();
    public Set<Integer> slotRequired = new TreeSet<>();

    private Writable extension;

    ActivationFunction activationFunction;


    private volatile int synapseIdCounter = 0;

    // synapseId -> relation
    private Map<Integer, Relation> outputRelations = new TreeMap<>();


    // A synapse is stored only in one direction, depending on the synapse weight.
    TreeMap<Synapse, Synapse> inputSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);
    TreeMap<Synapse, Synapse> outputSynapses = new TreeMap<>(Synapse.OUTPUT_SYNAPSE_COMP);
    TreeMap<Synapse, Synapse> passiveInputSynapses = null;

    private Provider<InputNode> outputNode;
    private Provider<OrNode> inputNode;


    ReadWriteLock lock = new ReadWriteLock();


    PassiveInputFunction passiveInputFunction = null;


    private ThreadState[] threads;


    /**
     * The {@code ThreadState} is a thread local data structure containing the activationsBySlotAndPosition of a single document for
     * a specific logic node.
     */
    private static class ThreadState {
        public long lastUsed;

        private TreeMap<ActKey, Activation> activationsBySlotAndPosition;
        private TreeMap<Integer, Activation> activations;
        public int minLength = Integer.MAX_VALUE;
        public int maxLength = 0;


        public ThreadState() {
            activationsBySlotAndPosition = new TreeMap<>();
            activations = new TreeMap<>();
        }
    }


    public void setOutputNode(Provider<InputNode> node) {
        outputNode = node;
    }


    public String getLabel() {
        return label;
    }

    public Type getType() {
        return type;
    }


    public Provider<InputNode> getOutputNode() {
        return outputNode;
    }


    public Provider<OrNode> getInputNode() {
        return inputNode;
    }


    public SynapseSummary getSynapseSummary() {
        return synapseSummary;
    }


    public Map<Integer, Relation> getOutputRelations() {
        return outputRelations;
    }


    public Collection<Synapse> getInputSynapses() {
        return inputSynapses.values();
    }


    public Collection<Synapse> getPassiveInputSynapses() {
        if(passiveInputSynapses == null) {
             return Collections.emptyList();
        }

        return passiveInputSynapses.values();
    }


    public void addRequiredSlot(int slot) {
        slotRequired.add(slot);
    }


    public ActivationFunction getActivationFunction() {
        return activationFunction;
    }


    public <T extends Writable> T getExtension() {
        return (T) extension;
    }


    public boolean addActivation(Activation act) {
        ThreadState th = getThreadState(act.getThreadId(), true);

        boolean first = th.activationsBySlotAndPosition.isEmpty();

        Integer l = act.length();
        if(l != null) {
            th.minLength = Math.min(th.minLength, l);
            th.maxLength = Math.max(th.maxLength, l);
        }

        for(Map.Entry<Integer, Position> me: act.slots.entrySet()) {
            ActKey ak = new ActKey(me.getKey(), me.getValue(), act.getId());
            th.activationsBySlotAndPosition.put(ak, act);
            th.activations.put(act.getId(), act);
        }

        return first;
    }


    public Stream<Activation> getActivations(Document doc) {
        ThreadState th = getThreadState(doc.getThreadId(), false);
        if(th == null) {
            return Stream.empty();
        }

        return th.activations.values().stream();
    }


    public boolean isEmpty(Document doc) {
        ThreadState th = getThreadState(doc.getThreadId(), false);
        if(th == null) {
            return true;
        }
        return th.activationsBySlotAndPosition.isEmpty();
    }


    public int size(Document doc) {
        ThreadState th = getThreadState(doc.getThreadId(), false);
        if(th == null) {
            return 0;
        }

        return th.activations.size();
    }


    public void clearActivations(Document doc) {
        ThreadState th = getThreadState(doc.getThreadId(), false);
        if(th == null) {
            return;
        }
        th.activationsBySlotAndPosition.clear();
        th.activations.clear();
    }


    public Stream<Activation> getActivations(Document doc, int slot, Position pos, boolean onlyFinal) {
        return getActivations(doc, slot, pos, true, slot, pos, false)
                .filter(act -> !onlyFinal || act.isFinalActivation());
    }


    public void clearActivations() {
        for (int i = 0; i < provider.model.numberOfThreads; i++) {
            clearActivations(i);
        }
    }


    public void clearActivations(int threadId) {
        ThreadState th = getThreadState(threadId, false);
        if (th == null) return;
        th.activationsBySlotAndPosition.clear();
        th.activations.clear();
    }


    public Stream<Activation> getActivations(Document doc, int fromSlot, Position fromPos, boolean fromInclusive, int toSlot, Position toPos, boolean toInclusive) {
        ThreadState th = getThreadState(doc.getThreadId(), false);
        if(th == null) {
            return Stream.empty();
        }
        return th.activationsBySlotAndPosition.subMap(
                new INeuron.ActKey(fromSlot, fromPos, Integer.MIN_VALUE),
                fromInclusive,
                new INeuron.ActKey(toSlot, toPos, Integer.MAX_VALUE),
                toInclusive
        ).values()
                .stream();
    }


    public Stream<Activation> getActivations(Document doc, boolean onlyFinal) {
        return onlyFinal ?
                getActivations(doc)
                        .filter(act -> act.isFinalActivation()) :
                getActivations(doc);
    }


    public Collection<Activation> getActivations(Document doc, SortedMap<Integer, Position> slots) {
        Integer firstSlot = slots.firstKey();
        Position firstPos = slots.get(firstSlot);

        return getActivations(doc, firstSlot, firstPos, true, firstSlot, firstPos, true)
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


    private ThreadState getThreadState(int threadId, boolean create) {
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


    public INeuron(Model m, String label, String outputText, Type type, ActivationFunction actF) {
        this.label = label;
        this.type = type;
        this.activationFunction = actF;

        setOutputText(outputText);

        if(m.getNeuronExtensionFactory() != null) {
            extension = m.getNeuronExtensionFactory().createObject();
        }

        threads = new ThreadState[m.numberOfThreads];

        provider = new Neuron(m, this);

        OrNode node = new OrNode(m);
        InputNode iNode = new InputNode(m);

        node.setOutputNeuron(provider);
        inputNode = node.getProvider();

        iNode.setInputNeuron(provider);
        outputNode = iNode.getProvider();

        setModified();
        iNode.setModified();

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
        x: for(Activation a: getActivations(doc, firstSlot, firstPos, true, firstSlot, firstPos, true).collect(Collectors.toList())) {
            for(Map.Entry<Integer, Integer> me: input.positions.entrySet()) {
                Position pos = a.getSlot(me.getKey());
                if(pos == null || me.getValue().compareTo(pos.getFinalPosition()) != 0) {
                    continue x;
                }
            }
            act = a;
        }

        if (act == null) {
            act = new Activation(doc, this, input.getSlots(doc));
        }


        Activation.State s = new Activation.State(input.value, input.value, input.net, 0.0, input.fired, 0.0);
        act.rounds.set(0, s);

        if(SearchNode.COMPUTE_SOFT_MAX) {
            Option o = act.new Option(-1, SELECTED);
            o.p = 1.0;
            o.state = s;

            act.options = new ArrayList<>();
            act.options.add(o);
        }

        act.inputValue = input.value;
        act.upperBound = input.value;
        act.lowerBound = input.value;

        act.inputDecision = SELECTED;
        act.finalDecision = act.inputDecision;
        act.setDecision(act.inputDecision, doc.getNewVisitedId());


        act.setTargetValue(input.targetValue);

        doc.addInputNeuronActivation(act);
        doc.addFinallyActivatedNeuron(act.getINeuron());

        doc.getLinker().linkInput(act);
        doc.getLinker().process();

        propagate(act);

        doc.propagate();

        return act;
    }


    public void commit(Collection<Synapse> modifiedSynapses) {
        synapseSummary.updateNeuronBias(biasDelta);

        for (Synapse s : modifiedSynapses) {
            INeuron in = s.getInput().get();
            in.lock.acquireWriteLock();
            try {
                synapseSummary.updateSynapse(s);

                s.commit();
            } finally {
                in.lock.releaseWriteLock();
            }

            if(s.isZero()) {
                s.unlink();
            }
        }

        bias += biasDelta;
        biasDelta = 0.0;

        setModified();
    }



    public boolean checkRequiredSlots(Document doc, SortedMap<Integer, Position> slots) {
        for(Integer slot : slotRequired) {
            if (!slots.containsKey(slot)) {
                if (!slotHasInputs.contains(slot)) {
                    slots.put(slot, new Position(doc));
                } else {
                    return true;
                }
            }
        }
        return false;
    }



    public Activation lookupActivation(Document doc, SortedMap<Integer, Position> slots, Predicate<Activation.Link> filter) {
        return getActivations(doc, slots)
                .stream()
                .filter(act -> act.match(filter))
                .findFirst()
                .orElse(null);
    }


    // TODO
    public void remove() {
        clearActivations();

        for (Synapse s : inputSynapses.values()) {
            INeuron in = s.getInput().get();
            in.provider.lock.acquireWriteLock();
            in.provider.inMemoryOutputSynapses.remove(s);
            in.provider.lock.releaseWriteLock();
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            INeuron out = s.getOutput().get();
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
        Document doc = act.getDocument();
        outputNode.get(doc).addActivation(act);
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

        synapseSummary.write(out);

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

        out.writeBoolean(inputNode != null);
        if (inputNode != null) {
            out.writeInt(inputNode.id);
        }

        out.writeInt(synapseIdCounter);
        for (Synapse s : inputSynapses.values()) {
            if (s.getInput() != null) {
                out.writeBoolean(true);
                s.write(out);

                out.writeBoolean(passiveInputSynapses != null && passiveInputSynapses.containsKey(s));
            }
        }
        out.writeBoolean(false);
        for (Synapse s : outputSynapses.values()) {
            if (s.getOutput() != null) {
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
        synapseSummary = SynapseSummary.read(in, m);

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
            inputNode = m.lookupNodeProvider(nId);
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
            s.getInput().removeInMemoryOutputSynapse(s);
        }
        for (Synapse s : outputSynapses.values()) {
            s.getOutput().removeInMemoryInputSynapse(s);
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryInputSynapses.values()) {
            s.getInput().removeInMemoryOutputSynapse(s);
        }
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            s.getOutput().removeInMemoryInputSynapse(s);
        }
        provider.lock.releaseReadLock();
    }


    @Override
    public void reactivate() {
        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryInputSynapses.values()) {
            s.getInput().addInMemoryOutputSynapse(s);
        }
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            s.getOutput().addInMemoryInputSynapse(s);
        }
        provider.lock.releaseReadLock();

        for (Synapse s : inputSynapses.values()) {
            s.getInput().addInMemoryOutputSynapse(s);
            if (!s.getInput().isSuspended()) {
                s.getOutput().addInMemoryInputSynapse(s);
            }
        }
        for (Synapse s : outputSynapses.values()) {
            s.getOutput().addInMemoryInputSynapse(s);
            if (!s.getOutput().isSuspended()) {
                s.getInput().addInMemoryOutputSynapse(s);
            }
        }
    }

    public void setBias(double b) {
        biasDelta = b - bias;
    }


    public void updateBiasDelta(double biasDelta) {
        this.biasDelta += biasDelta;
    }


    public double getBias() {
        return bias;
    }


    public double getNewBias() {
        return bias + biasDelta;
    }


    public void register(Activation act) {
        Document doc = act.getDocument();

        if (addActivation(act)) {
            doc.addActivatedNeuron(act.getINeuron());
        }

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
            int r = Double.compare(s2.getWeight(), s1.getWeight());
            if (r != 0) return r;
            return Integer.compare(s1.getInput().id, s2.getInput().id);
        });

        is.addAll(inputSynapses.values());

        StringBuilder sb = new StringBuilder();
        sb.append(toString());
        sb.append("<");
        sb.append("B:");
        sb.append(Utils.round(bias));
        for (Synapse s : is) {
            sb.append(", ");
            sb.append(Utils.round(s.getWeight()));
            sb.append(":");
            sb.append(s.getInput().toString());
        }
        sb.append(">");
        return sb.toString();
    }


    public static class SynapseSummary implements Writable {
        private volatile double biasSum;
        private volatile double posDirSum;
        private volatile double negDirSum;
        private volatile double negRecSum;
        private volatile double posRecSum;
        private volatile double posPassiveSum;

        private volatile int numDisjunctiveSynapses = 0;


        public double getBiasSum() {
            return biasSum;
        }

        public double getPosDirSum() {
            return posDirSum;
        }

        public double getNegDirSum() {
            return negDirSum;
        }

        public double getNegRecSum() {
            return negRecSum;
        }

        public double getPosRecSum() {
            return posRecSum;
        }

        public double getPosPassiveSum() {
            return posPassiveSum;
        }

        public int getNumDisjunctiveSynapses() {
            return numDisjunctiveSynapses;
        }

        public void updateNeuronBias(double biasDelta) {
            biasSum += biasDelta;
        }

        public void updateSynapse(Synapse s) {
            if (!s.isInactive()) {
                biasSum -= s.getBias();
                biasSum += s.getNewBias();

                updateSum(s.isRecurrent(), s.isNegative(), -(s.getLimit() * s.getWeight()));
                updateSum(s.isRecurrent(), s.getNewWeight() <= 0.0, s.getNewLimit() * s.getNewWeight());

                if(s.getInput().get().isPassiveInputNeuron() && !s.isNegative()) {
                    posPassiveSum -= !s.isNegative() ? (s.getLimit() * s.getWeight()) : 0.0;
                    posPassiveSum += s.getNewWeight() > 0.0 ? (s.getNewLimit() * s.getNewWeight()) : 0.0;
                }

                if(!s.isRecurrent()) {
                    if (!s.isDisjunction(Synapse.State.OLD) && s.isDisjunction(Synapse.State.NEW)) {
                        numDisjunctiveSynapses++;
                    } else if (s.isDisjunction(Synapse.State.OLD) && !s.isDisjunction(Synapse.State.NEW)) {
                        numDisjunctiveSynapses--;
                    }
                }
            }

            assert Double.isFinite(biasSum);
        }


        private void updateSum(boolean rec, boolean neg, double delta) {
            if(!rec) {
                if(!neg) {
                    posDirSum += delta;
                } else {
                    negDirSum += delta;
                }
            } else {
                if(!neg) {
                    posRecSum += delta;
                } else {
                    negRecSum += delta;
                }
            }
        }


        public static SynapseSummary read(DataInput in, Model m) throws IOException {
            SynapseSummary ss = new SynapseSummary();
            ss.readFields(in, m);
            return ss;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeDouble(biasSum);
            out.writeDouble(posDirSum);
            out.writeDouble(negDirSum);
            out.writeDouble(negRecSum);
            out.writeDouble(posRecSum);
            out.writeDouble(posPassiveSum);

            out.writeInt(numDisjunctiveSynapses);
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            biasSum = in.readDouble();
            posDirSum = in.readDouble();
            negDirSum = in.readDouble();
            negRecSum = in.readDouble();
            posRecSum = in.readDouble();
            posPassiveSum = in.readDouble();

            numDisjunctiveSynapses = in.readInt();
        }
    }
}
