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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.Synapse.State.NEXT;

/**
 * The {@code INeuron} class represents a internal neuron implementation in Aikas neural network and is connected to other neurons through
 * input synapses and output synapses. The activation value of a neuron is calculated by computing the weighted sum
 * (input act. value * synapse weight) of the input synapses, adding the bias to it and sending the resulting value
 * through a transfer function (the upper part of tanh).
 *
 * @author Lukas Molzberger
 */
public abstract class INeuron<A extends Activation, S extends Synapse> extends AbstractNode<Neuron> implements Comparable<INeuron> {

    private static final Logger log = LoggerFactory.getLogger(INeuron.class);

    public static double WEIGHT_TOLERANCE = 0.001;


    public static final INeuron MIN_NEURON = new ExcitatoryNeuron();
    public static final INeuron MAX_NEURON = new ExcitatoryNeuron();

    private String label;
    private String outputText;

    private volatile double bias;
    private volatile double biasDelta;

    protected SynapseSummary synapseSummary = new SynapseSummary();

    private volatile int synapseIdCounter = 0;


    // A synapse is stored only in one direction, depending on the synapse weight.
    TreeMap<S, S> inputSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);
    TreeMap<Synapse, Synapse> outputSynapses = new TreeMap<>(Synapse.OUTPUT_SYNAPSE_COMP);


    ReadWriteLock lock = new ReadWriteLock();


    private ThreadState<A>[] threads;


    /**
     * The {@code ThreadState} is a thread local data structure containing the activationsBySlotAndPosition of a single document for
     * a specific logic node.
     */
    private static class ThreadState<A extends Activation> {
        public long lastUsed;
        public Document doc;

        private TreeMap<Integer, A> activations;

        public ThreadState() {
            activations = new TreeMap<>();
        }
    }


    protected INeuron() {
    }


    public INeuron(Neuron p) {
        provider = p;
        threads = new INeuron.ThreadState[p.getModel().numberOfThreads];
    }


    public INeuron(Model m, String label) {
        this.label = label;

        threads = new ThreadState[m.numberOfThreads];

        provider = new Neuron(m, this);

        setModified();
    }


    public abstract boolean isRecurrent(boolean isNegativeSynapse);


    public abstract boolean isWeak(Synapse synapse, Synapse.State state);


    public Integer getId() {
        return provider.getId();
    }

    public String getLabel() {
        return label;
    }


    public SynapseSummary getSynapseSummary() {
        return synapseSummary;
    }


    public Collection<S> getInputSynapses() {
        return inputSynapses.values();
    }


    public S getMaxInputSynapse(Synapse.State state) {
        return null;
    }


    public Collection<Synapse> getOutputSynapses() {
        return outputSynapses.values();
    }


    public abstract ActivationFunction getActivationFunction();


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
        return th.activations.isEmpty();
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
        th.activations.clear();
        th.doc = null;
    }


    public void clearActivations() {
        for (int i = 0; i < provider.getModel().numberOfThreads; i++) {
            clearActivations(i);
        }
    }


    public void clearActivations(int threadId) {
        ThreadState th = getThreadState(threadId, false);
        if (th == null) return;
        th.activations.clear();
        th.doc = null;
    }


    public Model getModel() {
        return provider.getModel();
    }



    public Stream<Activation> getActivations(Document doc, boolean onlyFinal) {
        return onlyFinal ?
                getActivations(doc)
                        .filter(act -> act.isFinalActivation()) :
                getActivations(doc);
    }


    private ThreadState<A> getThreadState(int threadId, boolean create) {
        ThreadState<A> th = threads[threadId];
        if (th == null) {
            if (!create) return null;

            th = new ThreadState();
            threads[threadId] = th;
        }
        th.lastUsed = provider.getModel().docIdCounter.get();
        return th;
    }


    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param input
     */
    public Activation addInput(Document doc, Activation.Builder input) {
        Activation act = createActivation(doc);

        // TODO: add input links

        act.setInputState(input);

        doc.addInputNeuronActivation(act);
        doc.addFinallyActivatedNeuron(act.getINeuron());


        propagate(act);

        doc.propagate();

        return act;
    }


    protected abstract Activation createActivation(Document doc);



    public void commit(Collection<? extends Synapse> modifiedSynapses) {
        for (Synapse s : modifiedSynapses) {
            INeuron in = s.getInput().get();
            in.lock.acquireWriteLock();
            try {
                synapseSummary.updateSynapse(s);
            } finally {
                in.lock.releaseWriteLock();
            }
        }

        bias += biasDelta;
        biasDelta = 0.0;

        for (Synapse s : modifiedSynapses) {
            s.commit();
        }

        synapseSummary.commit();

        setModified();
    }


    // TODO
    public void remove() {
        clearActivations();

        for (Synapse s : inputSynapses.values()) {
            INeuron<?, ?> in = s.getInput().get();
            in.provider.lock.acquireWriteLock();
            in.provider.activeOutputSynapses.remove(s);
            in.provider.lock.releaseWriteLock();
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.activeOutputSynapses.values()) {
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

        // TODO: Prüfen, ob die Aktivierung schon existiert.

        getOutputSynapses()
                .forEach(s -> doc.getLinker().link(s, act, createActivation(doc)));

        doc.getLinker().process();
    }


    public int compareTo(INeuron n) {
        if (this == n) return 0;
        if (this == MIN_NEURON) return -1;
        if (n == MIN_NEURON) return 1;
        if (this == MAX_NEURON) return 1;
        if (n == MAX_NEURON) return -1;

        if (getId() < n.getId()) return -1;
        else if (getId() > n.getId()) return 1;
        else return 0;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(getType());

        out.writeBoolean(label != null);
        if(label != null) {
            out.writeUTF(label);
        }

        out.writeBoolean(outputText != null);
        if(outputText != null) {
            out.writeUTF(outputText);
        }

        out.writeDouble(bias);

        synapseSummary.write(out);

        out.writeInt(synapseIdCounter);
        for (Synapse s : inputSynapses.values()) {
            if (s.getInput() != null) {
                out.writeBoolean(true);
                getModel().writeSynapse(s, out);
            }
        }
        out.writeBoolean(false);
        for (Synapse s : outputSynapses.values()) {
            if (s.getOutput() != null) {
                out.writeBoolean(true);
                getModel().writeSynapse(s, out);
            }
        }
        out.writeBoolean(false);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        if(in.readBoolean()) {
            label = in.readUTF();
        }

        if(in.readBoolean()) {
            outputText = in.readUTF();
        }

        bias = in.readDouble();
        synapseSummary = SynapseSummary.read(in, m);

        synapseIdCounter = in.readInt();
        while (in.readBoolean()) {
            S syn = (S) m.readSynapse(in);
            inputSynapses.put(syn, syn);
        }

        while (in.readBoolean()) {
            Synapse syn = m.readSynapse(in);
            outputSynapses.put(syn, syn);
        }
    }


    @Override
    public void suspend() {
        for (Synapse s : inputSynapses.values()) {
            s.getInput().removeActiveOutputSynapse(s);
        }
        for (Synapse s : outputSynapses.values()) {
            s.getOutput().removeActiveInputSynapse(s);
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.activeInputSynapses.values()) {
            s.getInput().removeActiveOutputSynapse(s);
        }
        for (Synapse s : provider.activeOutputSynapses.values()) {
            s.getOutput().removeActiveInputSynapse(s);
        }
        provider.lock.releaseReadLock();
    }


    @Override
    public void reactivate() {
        provider.lock.acquireReadLock();
        for (Synapse s : provider.activeInputSynapses.values()) {
            s.getInput().addActiveOutputSynapse(s);
        }
        for (Synapse s : provider.activeOutputSynapses.values()) {
            s.getOutput().addActiveInputSynapse(s);
        }
        provider.lock.releaseReadLock();

        for (Synapse s : inputSynapses.values()) {
            s.getInput().addActiveOutputSynapse(s);
            if (!s.getInput().isSuspended()) {
                s.getOutput().addActiveInputSynapse(s);
            }
        }
        for (Synapse s : outputSynapses.values()) {
            s.getOutput().addActiveInputSynapse(s);
            if (!s.getOutput().isSuspended()) {
                s.getInput().addActiveOutputSynapse(s);
            }
        }
    }

    public void setBias(double b) {
        biasDelta = b - bias;
    }


    public void updateBiasDelta(double biasDelta) {
        this.biasDelta += biasDelta;
    }



    public abstract double getTotalBias(Synapse.State state);


    public double getBias() {
        return bias;
    }


    protected double getBias(Synapse.State state) {
        return state == CURRENT ? bias : bias + biasDelta;
    }


    public double getNewBias() {
        return bias + biasDelta;
    }

    public double getBiasDelta() {
        return biasDelta;
    }


    public void register(Activation act) {
        Document doc = act.getDocument();

        ThreadState th = getThreadState(act.getThreadId(), true);
        if(th.doc == null) {
            th.doc = doc;
            doc.addActivatedNeuron(act.getINeuron());
        }
        if(th.doc != doc) {
            throw new Model.StaleDocumentException();
        }

        th.activations.put(act.getId(), act);

        doc.addActivation(act);
    }


    public abstract String getType();


    public String toString() {
        return label;
    }


    protected String toDetailedString() {
        return typeToString() + " " + label + " B:" + Utils.round(bias);
    }


    public abstract String typeToString();


    public String toStringWithSynapses() {
        SortedSet<Synapse> is = new TreeSet<>(Comparator.comparing(s -> s.getInput().getId()));

        is.addAll(getProvider().getActiveInputSynapses());

        StringBuilder sb = new StringBuilder();
        sb.append(toDetailedString());
        sb.append("\n");
        for (Synapse s : is) {
            sb.append("  ");
            sb.append(s.toString());
            sb.append("\n");
        }
        return sb.toString();
    }


    public static class SynapseSummary implements Writable {
        private volatile double posDirSum;
        private volatile double negDirSum;
        private volatile double negRecSum;
        private volatile double posRecSum;
        private volatile double posPassiveSum;

        private volatile double posDirSumDelta = 0.0;
        private volatile double negDirSumDelta = 0.0;
        private volatile double negRecSumDelta = 0.0;
        private volatile double posRecSumDelta = 0.0;
        private volatile double posPassiveSumDelta = 0.0;


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

        public double getPosSum(Synapse.State state) {
            return getPosDirSum(state) + getPosRecSum(state);
        }


        private double getPosDirSum(Synapse.State state) {
            return state == CURRENT ? posDirSum : posDirSum + posDirSumDelta;
        }

        private double getPosRecSum(Synapse.State state) {
            return state == CURRENT ? posRecSum : posRecSum + posRecSumDelta;
        }

        private double getPosPassiveSum(Synapse.State state) {
            return state == CURRENT ? posPassiveSum : posPassiveSum + posPassiveSumDelta;
        }


        public void updateSynapse(Synapse s) {
            if (!s.isInactive(CURRENT)) {
                updateSynapse(CURRENT, -1.0, s);
            }
            if (!s.isInactive(NEXT)) {
                updateSynapse(NEXT, 1.0, s);
            }
        }

        private void updateSynapse(Synapse.State state, double sign, Synapse s) {
            updateSum(s.isRecurrent(), s.isNegative(state), sign * (s.getWeight(state)));

            posDirSumDelta += sign * s.computeMaxRelationWeights();
        }

        private void updateSum(boolean rec, boolean neg, double delta) {
            if(!rec) {
                if(!neg) {
                    posDirSumDelta += delta;
                } else {
                    negDirSumDelta += delta;
                }
            } else {
                if(!neg) {
                    posRecSumDelta += delta;
                } else {
                    negRecSumDelta += delta;
                }
            }
        }


        public void commit() {
            posDirSum += posDirSumDelta;
            negDirSum += negDirSumDelta;
            posRecSum += posRecSumDelta;
            negRecSum += negRecSumDelta;
            posPassiveSum += posPassiveSumDelta;

            posDirSumDelta = 0.0;
            negDirSumDelta = 0.0;
            negRecSumDelta = 0.0;
            posDirSumDelta = 0.0;
            posPassiveSumDelta = 0.0;
        }


        public static SynapseSummary read(DataInput in, Model m) throws IOException {
            SynapseSummary ss = new SynapseSummary();
            ss.readFields(in, m);
            return ss;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeDouble(posDirSum);
            out.writeDouble(negDirSum);
            out.writeDouble(negRecSum);
            out.writeDouble(posRecSum);
            out.writeDouble(posPassiveSum);
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            posDirSum = in.readDouble();
            negDirSum = in.readDouble();
            negRecSum = in.readDouble();
            posRecSum = in.readDouble();
            posPassiveSum = in.readDouble();
        }
    }
}
