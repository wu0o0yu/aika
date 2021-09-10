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

import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.sign.Sign;
import network.aika.utils.ReadWriteLock;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static network.aika.neuron.sign.Sign.NEG;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.utils.Utils.logChange;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Neuron<S extends Synapse> implements Writable {

    public static double BETA_THRESHOLD = 0.95;

    volatile long retrievalCount = 0;

    private volatile boolean modified;

    private NeuronProvider provider;

    private String label;

    private Writable customData;

    protected volatile double bias;

    protected TreeMap<NeuronProvider, S> inputSynapses = new TreeMap<>();
    protected TreeMap<NeuronProvider, Synapse> outputSynapses = new TreeMap<>();

    protected final ReadWriteLock lock = new ReadWriteLock();

    protected double frequency;
    protected SampleSpace sampleSpace;

    protected boolean isInputNeuron; // Input Neurons won't be trained!

    protected boolean allowTraining = true;

    private Neuron<?> template;

    private TemplateNeuronInfo templateInfo;

    protected Neuron() {
    }

    public Neuron(NeuronProvider p) {
        provider = p;
    }

    public Neuron(Model m, boolean addProvider) {
        if(addProvider)
            provider = new NeuronProvider(m, this);
        sampleSpace = new SampleSpace(m);
        modified = true;
    }

    public TemplateNeuronInfo getTemplateInfo() {
        assert isTemplate();
        if(templateInfo == null) {
            templateInfo = new TemplateNeuronInfo();
        }

        return templateInfo;
    }

    protected void initFromTemplate(Neuron n) {
        n.bias = bias;
        n.template = this;
    }

    public abstract Neuron<?> instantiateTemplate(boolean addProvider);

    public abstract void transition(ActVisitor v, Activation act);

    public void alternateBranchTransition(ActVisitor v, Activation act) {
    }

    public abstract void addDummyLinks(Activation act);

    public abstract ActivationFunction getActivationFunction();

    public abstract Fired incrementFired(Fired f);

    public abstract boolean allowTemplatePropagate(Activation act);

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public boolean isTemplate() {
        return getId() < 0;
    }

    public Neuron<?> getTemplate() {
        if(isTemplate())
            return this;
        return template;
    }

    public Set<Neuron<?>> getTemplateGroup() {
        return getTemplate().getTemplateInfo().getTemplateGroup();
    }

    public Synapse getOutputSynapse(NeuronProvider n) {
        lock.acquireReadLock();
        Synapse s = outputSynapses.get(n);
        lock.releaseReadLock();
        return s;
    }

    public double getCandidateGradient(Activation act) {
        return getSurprisal(POS, act.getReference());
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

    public Stream<S> getInputSynapses() {
        return inputSynapses.values().stream();
    }

    public Stream<? extends Synapse> getOutputSynapses() {
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

    public void addBias(double biasDelta) {
        double oldBias = bias;
        bias += biasDelta;
        logChange(this, oldBias, bias, "addBias : bias");
        modified = true;
    }

    public void limitBias() {
        double oldBias = bias;
        bias = Math.min(0.0, bias);
        logChange(this, oldBias, bias, "limitBias : bias");
    }

    public double getBias() {
        return bias;
    }

    public double getRecurrentBias() {
        return 0.0;
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public void count(Activation act) {
        addDummyLinks(act);

        sampleSpace.countSkippedInstances(act.getReference());

        Double alpha = act.getConfig().getAlpha();
        if (alpha != null)
            applyMovingAverage(alpha);

        sampleSpace.count();
        frequency += 1.0;
        modified = true;
    }

    public void applyMovingAverage(double alpha) {
        sampleSpace.applyMovingAverage(alpha);
        frequency *= alpha;
        modified = true;
    }

    public double getSurprisal(Sign s, Reference ref) {
        double N = sampleSpace.getN(ref);
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

        return dist.inverseCumulativeProbability(
                BETA_THRESHOLD
        );
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

    public void reactivate(Model m) {
        m.incrementRetrievalCounter();
        retrievalCount = m.getCurrentRetrievalCount();
    }

    public void suspend() {
    }

    public void updateSynapseInputConnections() {
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte((byte) getTemplate().getId().intValue());

        out.writeBoolean(label != null);
        if(label != null)
            out.writeUTF(label);

        out.writeDouble(bias);

        for (Synapse s : inputSynapses.values()) {
            if (s.getInput() != null) {
                out.writeBoolean(true);
                s.write(out);
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

        out.writeDouble(frequency);
        sampleSpace.write(out);

        out.writeBoolean(isInputNeuron);

        out.writeBoolean(customData != null);
        if(customData != null)
            customData.write(out);
    }

    public static Neuron read(DataInput in, Model m) throws Exception {
        byte templateNeuronId = in.readByte();
        Neuron templateNeuron = m.getTemplates().getTemplateNeuron(templateNeuronId);
        Neuron n = templateNeuron.instantiateTemplate(false);
        n.readFields(in, m);
        return n;
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        if(in.readBoolean())
            label = in.readUTF();


        bias = in.readDouble();

        while (in.readBoolean()) {
            S syn = (S) Synapse.read(in, m);
            inputSynapses.put(syn.getPInput(), syn);
        }

        while (in.readBoolean()) {
            Synapse syn = Synapse.read(in, m);
            outputSynapses.put(syn.getPOutput(), syn);
        }

        frequency = in.readDouble();
        sampleSpace = SampleSpace.read(in, m);

        isInputNeuron = in.readBoolean();

        if(in.readBoolean()) {
            customData = m.getCustomDataInstanceSupplier().get();
            customData.readFields(in, m);
        }
    }

    public String toString() {
        return getId() + ":" + getLabel();
    }

    public String toDetailedString() {
        return "n " + getClass().getSimpleName() + " " + this + " b:" + Utils.round(bias);
    }

    public String statToString() {
        return getClass().getSimpleName() + " " +
                getId() + ":" + getLabel() + " " +
                "f:" + Utils.round(frequency) + " " +
                "N:" + Utils.round(sampleSpace.getN(null)) + " " +
                "p:" + Utils.round(getP(POS, sampleSpace.getN(null))) + " " +
                "s(p):" + Utils.round(getSurprisal(POS, null)) + " " +
                "s(n):" + Utils.round(getSurprisal(NEG, null)) + " " +
                "\n";
    }
}
