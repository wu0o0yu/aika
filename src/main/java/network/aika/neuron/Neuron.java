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
import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.neuron.activation.Activation;
import network.aika.fields.Field;
import network.aika.sign.Sign;
import network.aika.steps.activation.PostTraining;
import network.aika.steps.activation.Save;
import network.aika.utils.Bound;
import network.aika.utils.ReadWriteLock;
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Neuron<S extends Synapse, A extends Activation> implements Writable {

    volatile long retrievalCount = 0;

    private volatile boolean modified;

    private NeuronProvider provider;

    private String label;

    private Writable customData;

    protected Field bias = new Field("bias", (l, u) ->
        biasUpdate(u)
    );

    protected List<S> inputSynapses = new ArrayList<>();
    protected List<Synapse> outputSynapses = new ArrayList<>();

    protected final ReadWriteLock lock = new ReadWriteLock();

    protected double frequency;
    protected SampleSpace sampleSpace = new SampleSpace();

    protected boolean isNetworkInput; // Input Neurons won't be trained!

    protected boolean allowTraining = true;

    private Neuron<?, ?> template;

    private TemplateNeuronInfo templateInfo;

    private WeakHashMap<Long, SortedSet<A>> activations = new WeakHashMap<>();

    protected Neuron() {
    }

    public Neuron(NeuronProvider p) {
        provider = p;
    }

    public Neuron(Model m, boolean addProvider) {
        if(addProvider)
            provider = new NeuronProvider(m, this);
        setModified();
    }

    public void register(A act) {
        Thought t = act.getThought();
        synchronized (activations) {
            activations
                    .computeIfAbsent(
                            t.getId(),
                            n -> initActivationsSet(t)
                    )
                    .add(act);
        }
    }

    public boolean templateNeuronMatches(Neuron<?, ?> targetN) {
        return getTemplateGroup().stream()
                .anyMatch(tn ->
                        tn.getId().intValue() == targetN.getId().intValue()
                );
    }

    private TreeSet<A> initActivationsSet(Thought t) {
        TreeSet<A> acts = new TreeSet<>();
        t.register(provider, acts);
        return acts;
    }

    public SortedSet<A> getActivations(Thought t) {
        if(t == null)
            return Collections.emptySortedSet();

        SortedSet<A> acts = activations.get(t.getId());
        return acts != null ? acts : Collections.emptyNavigableSet();
    }

    public TemplateNeuronInfo getTemplateInfo() {
        assert isTemplate();
        if(templateInfo == null) {
            templateInfo = new TemplateNeuronInfo();
        }

        return templateInfo;
    }

    protected void initFromTemplate(Neuron n) {
        n.bias.setAndTriggerUpdate(bias.getCurrentValue());
        n.template = this;
    }

    public abstract A createActivation(Thought t);

    public abstract Neuron<?, ?> instantiateTemplate(boolean addProvider);

    public abstract void addInactiveLinks(Activation act);

    public abstract ActivationFunction getActivationFunction();

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public boolean isTemplate() {
        return getId() < 0;
    }

    public Neuron getTemplate() {
        if(isTemplate())
            return this;
        return template;
    }

    public Set<Neuron> getTemplateGroup() {
        return getTemplate().getTemplateInfo().getTemplateGroup();
    }


    protected void biasUpdate(double u) {
        PostTraining.add(this);
        setModified();
    }

    public double getCandidateGradient(Activation act) {
        Range range = act.getAbsoluteRange();
        assert range != null;

        if(isTemplate())
            return 0.0;

        return getSurprisal(POS, range, true);
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
        return inputSynapses.stream();
    }

    public Stream<? extends Synapse> getOutputSynapses() {
        return outputSynapses.stream();
    }

    public void setNetworkInput(boolean networkInput) {
        isNetworkInput = networkInput;
    }

    public boolean isNetworkInput() {
        return isNetworkInput;
    }

    public Stream<? extends Synapse> getTargetSynapses(Direction dir, boolean template) {
        return (template ?
                getTemplateGroup().stream().flatMap(dir::getSynapses) :
                dir.getSynapses(this));
    }


    public Synapse getOutputSynapse(NeuronProvider n) {
        lock.acquireReadLock();
        Synapse syn = selectOutputSynapse(s ->
                s.getPOutput().getId() == n.getId()
        );
        lock.releaseReadLock();
        return syn;
    }

    public Synapse getInputSynapse(NeuronProvider n) {
        lock.acquireReadLock();
        Synapse syn = selectInputSynapse(s ->
                s.getPInput().getId() == n.getId()
        );

        lock.releaseReadLock();
        return syn;
    }

    protected Synapse selectInputSynapse(Predicate<? super Synapse> predicate) {
        return inputSynapses.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    protected Synapse selectOutputSynapse(Predicate<? super Synapse> predicate) {
        return outputSynapses.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    public void addInputSynapse(S s) {
        inputSynapses.add(s);
        setModified();
    }

    public void removeInputSynapse(S s) {
        if(inputSynapses.remove(s))
            setModified();
    }

    public void addOutputSynapse(Synapse s) {
        outputSynapses.add(s);
        setModified();
    }

    public void removeOutputSynapse(Synapse s) {
        if(outputSynapses.remove(s))
            setModified();
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

    public void setModified() {
        this.modified = true;
        Save.add(this);
    }

    public void resetModified() {
        this.modified = false;
    }

    public boolean isModified() {
        return modified;
    }

    public void limitBias() {
        if(bias.getCurrentValue() > 0.0)
            bias.setAndTriggerUpdate(0.0);
    }

    public Field getBias() {
        return bias;
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public void count(Activation act) {
        Range absoluteRange = act.getAbsoluteRange();
        sampleSpace.countSkippedInstances(absoluteRange);

        Double alpha = act.getConfig().getAlpha();
        if (alpha != null)
            applyMovingAverage(alpha);

        sampleSpace.count();
        frequency += 1.0;

        sampleSpace.updateLastPosition(absoluteRange);
        setModified();
    }

    public void applyMovingAverage(double alpha) {
        sampleSpace.applyMovingAverage(alpha);
        frequency *= alpha;
        setModified();
    }

    public double getSurprisal(Sign s, Range range, boolean addCurrentInstance) {
        double n = sampleSpace.getN(range);
        double p = getProbability(s, n, addCurrentInstance);
        return Utils.surprisal(p);
    }

    public double getProbability(Sign s, double n, boolean addCurrentInstance) {
        double f = getFrequency(s, n);

        if(addCurrentInstance) {
            f += 1.0;
            n += 1.0;
        }

        return Bound.UPPER.probability(f, n);
    }

    public double getFrequency() {
        return frequency;
    }

    public double getFrequency(Sign s, double n) {
        return s == POS ?
                frequency :
                n - frequency;
    }

    public void setFrequency(double f) {
        frequency = f;
        setModified();
    }

    public boolean checkTemplateInductionThreshold(Activation act)  {
        return Math.abs(getCandidateGradient(act)) > act.getConfig().getInductionThreshold();
    }

    public void reactivate(Model m) {
        m.incrementRetrievalCounter();
        retrievalCount = m.getCurrentRetrievalCount();
    }

    public void suspend() {
    }

    public void updateAllowPropagate() {
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte((byte) getTemplate().getId().intValue());

        out.writeBoolean(label != null);
        if(label != null)
            out.writeUTF(label);

        bias.write(out);

        for (Synapse s : inputSynapses) {
            if (s.getInput() != null) {
                out.writeBoolean(true);
                s.write(out);
            }
        }
        out.writeBoolean(false);

        for (Synapse s : outputSynapses) {
            if (s.getOutput() != null) {
                out.writeBoolean(true);
                s.write(out);
            }
        }
        out.writeBoolean(false);

        out.writeDouble(frequency);
        sampleSpace.write(out);

        out.writeBoolean(isNetworkInput);

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

        bias.readFields(in, m);

        while (in.readBoolean()) {
            S syn = (S) Synapse.read(in, m);
            inputSynapses.add(syn);
        }

        while (in.readBoolean()) {
            Synapse syn = Synapse.read(in, m);
            outputSynapses.add(syn);
        }

        frequency = in.readDouble();
        sampleSpace = SampleSpace.read(in, m);

        isNetworkInput = in.readBoolean();

        if(in.readBoolean()) {
            customData = m.getCustomDataInstanceSupplier().get();
            customData.readFields(in, m);
        }
    }

    public String toKeyString() {
        return getId() + ":" + getLabel();
    }

    public String toString() {
        return (isTemplate() ? "Template-" : "") + getClass().getSimpleName() + " " + toKeyString();
    }
}
