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
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.sign.Sign;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.sign.Sign.NEG;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.neuron.activation.Visitor.Transition.ACT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Neuron<S extends Synapse> implements Writable {

    public static int debugId = 0;
    public static boolean debugOutput = false;

    private static final Logger log = LoggerFactory.getLogger(Neuron.class);

    volatile long retrievalCount = 0;

    private volatile boolean modified;

    private NeuronProvider provider;

    private String label;

    private Writable customData;

    private volatile double bias;

    protected TreeMap<NeuronProvider, S> inputSynapses = new TreeMap<>();
    protected TreeMap<NeuronProvider, Synapse> outputSynapses = new TreeMap<>();

    protected final ReadWriteLock lock = new ReadWriteLock();

    protected double frequency;
    protected SampleSpace sampleSpace = new SampleSpace();

    protected boolean isInputNeuron; // Input Neurons won't be trained!

    protected boolean allowTraining = true;

    private Set<Neuron<?>> templates = new TreeSet<>(Comparator.comparing(n -> n.getId()));

    protected Neuron() {
    }

    public Neuron(NeuronProvider p) {
        provider = p;
    }

    public Neuron(Model m) {
        provider = new NeuronProvider(m, this);
        modified = true;
    }

    public abstract Neuron<?> instantiateTemplate();

    public abstract void addDummyLinks(Activation act);

    public abstract ActivationFunction getActivationFunction();

    public abstract Fired incrementFired(Fired f);

    public abstract byte getType();

    public abstract boolean checkGradientThreshold(Activation act);

    public abstract Scope[] getInitialScopes(Direction dir);

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public boolean isTemplate() {
        return getId() < 0;
    }

    public Set<Neuron<?>> getTemplates() {
        return templates;
    }

    public void transition(Visitor v, Activation act) {
        Visitor nv = v.prepareNextStep(act, null, v.getScopes(), ACT);

        if(nv == null)
            return;

        act.followLinks(nv);
    }

    public Synapse getOutputSynapse(NeuronProvider n) {
        lock.acquireReadLock();
        Synapse s = outputSynapses.get(n);
        lock.releaseReadLock();
        return s;
    }

    public SampleSpace getSampleSpace() {
        return sampleSpace;
    }

    public NeuronProvider getProvider() {
        return provider;
    }

    public void setProvider(NeuronProvider p) {
        this.provider = p;
    }

    public Stream<? extends Synapse> getInputSynapses() {
        throw new UnsupportedOperationException();
    }

    public Stream<Synapse> getOutputSynapses() {
        return outputSynapses.values().stream();
    }

    public void setInputNeuron(boolean inputNeuron) {
        isInputNeuron = inputNeuron;
    }

    public boolean isInputNeuron() {
        return isInputNeuron;
    }

    public boolean containsInputSynapse(Synapse s) {
        return inputSynapses.containsKey(s.getPInput());
    }

    public boolean containsOutputSynapse(Synapse s) {
        return outputSynapses.containsKey(s.getPOutput());
    }

    public Synapse getInputSynapse(NeuronProvider n) {
        lock.acquireReadLock();
        Synapse s = inputSynapses.get(n);
        lock.releaseReadLock();
        return s;
    }

    public void addInputSynapse(S s) {
        S os = inputSynapses.put(s.getPInput(), s);
        if(os != s) {
            setModified(true);
        }
    }

    public void removeInputSynapse(S s) {
        if(inputSynapses.remove(s.getPInput()) != null) {
            setModified(true);
        }
    }

    public void addOutputSynapse(Synapse s) {
        Synapse os = outputSynapses.put(s.getPOutput(), s);
        if(os != s) {
            setModified(true);
        }
    }

    public void removeOutputSynapse(Synapse s) {
        if(outputSynapses.remove(s.getPOutput()) != null) {
            setModified(true);
        }
    }

    public Long getId() {
        return provider.getId();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Writable getCustomData() {
        return customData;
    }

    public void setCustomData(Writable customData) {
        this.customData = customData;
    }

    public <M extends Model> M getModel() {
        return (M) provider.getModel();
    }

    public Config getConfig() {
        return getModel().getConfig();
    }

    public long getRetrievalCount() {
        return retrievalCount;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void addConjunctiveBias(double b, boolean recurrent) {
    }

    public void setBias(double b) {
        bias += b;
        modified = true;
    }

    public void addBias(double biasDelta) {
        bias += biasDelta;
        modified = true;
    }

    public double getBias(boolean isFinal) {
        return bias;
    }

    public double getRawBias() {
        return bias;
    }

    public double updateBias(Activation act) {
        double learnRate = getConfig().getLearnRate();

        double biasDelta = learnRate * act.getGradientSum();
        addBias(biasDelta);

        double finalBias = getBias(true);
        if(finalBias > 0.0) {
            addBias(-finalBias);
        }

        return biasDelta;
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public void count(Activation act) {
        addDummyLinks(act);

        if(act.isActive()) {
            sampleSpace.update(getModel(), act.getReference());
            frequency += 1.0;
            modified = true;
        }
    }

    public void applyMovingAverage(Config trainingConfig) {
        Double alpha = trainingConfig.getAlpha();
        if(alpha != null) {
            frequency *= alpha;
            modified = true;
        }
    }

    public double getSurprisal(Sign s) {
        double N = sampleSpace.getN();
        if(isTemplate() || N == 0.0)
            return 0.0;

        double p = getP(s, N);
        return -Math.log(p);
    }

    public double getP(Sign s, double n) {
        BetaDistribution dist = new BetaDistribution(
                getFrequency(s, n) + 1,
                getFrequency(s.invert(), n) + 1
        );

        double p = dist.inverseCumulativeProbability(
                getModel().getConfig().getBetaThreshold()
        );

        return p;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getFrequency(Sign s, double n) {
        return (s == POS ? frequency : n - frequency);
    }

    public void setFrequency(double f) {
        frequency = f;
        modified = true;
    }

    public void reactivate() {
    }

    public void suspend() {
    }

    public void updateSynapseInputLinks() {
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());

        out.writeBoolean(label != null);
        if(label != null) {
            out.writeUTF(label);
        }

        out.writeDouble(bias);

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

        out.writeDouble(frequency);
        sampleSpace.write(out);

        out.writeBoolean(isInputNeuron);

        out.writeBoolean(customData != null);
        if(customData != null) {
            customData.write(out);
        }
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        if(in.readBoolean()) {
            label = in.readUTF();
        }

        bias = in.readDouble();

        while (in.readBoolean()) {
            S syn = (S) m.readSynapse(in);
            inputSynapses.put(syn.getPInput(), syn);
        }

        while (in.readBoolean()) {
            Synapse syn = m.readSynapse(in);
            outputSynapses.put(syn.getPOutput(), syn);
        }

        frequency = in.readDouble();
        sampleSpace = SampleSpace.read(in, m);

        isInputNeuron = in.readBoolean();

        if(in.readBoolean()) {
            customData = m.getConfig().getCustomDataInstanceSupplier().get();
            customData.readFields(in, m);
        }
    }

    public String toString() {
        return getId() + ":" + getLabel();
    }

    public String toDetailedString() {
        return "n " + getClass().getSimpleName() + " " + toString() + " b:" + Utils.round(bias);
    }

    public String statToString() {
        return getClass().getSimpleName() + " " +
                getId() + ":" + getLabel() + " " +
                "f:" + Utils.round(frequency) + " " +
                "N:" + Utils.round(sampleSpace.getN()) + " " +
                "p:" + Utils.round(getP(POS, sampleSpace.getN())) + " " +
                "s(p):" + Utils.round(getSurprisal(POS)) + " " +
                "s(n):" + Utils.round(getSurprisal(NEG)) + " " +
                "\n";
    }
}
