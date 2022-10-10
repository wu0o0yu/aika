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
import network.aika.callbacks.NeuronProducer;
import network.aika.direction.Direction;
import network.aika.fields.LimitedField;
import network.aika.fields.QueueField;
import network.aika.neuron.activation.Activation;
import network.aika.fields.Field;
import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.Timestamp;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.sign.Sign;
import network.aika.steps.activation.Save;
import network.aika.utils.Bound;
import network.aika.utils.ReadWriteLock;
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Timestamp.MAX;
import static network.aika.neuron.activation.Timestamp.MIN;
import static network.aika.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Neuron<S extends Synapse, A extends Activation> implements Element, Writable {

    volatile long retrievalCount = 0;

    private volatile boolean modified;

    private NeuronProvider provider;

    private String label;

    private Writable customData;

    protected QueueField bias = new LimitedField(this, "bias", 0.0, () -> {
        setModified();
    });

    protected List<S> inputSynapses = new ArrayList<>();
    protected List<Synapse> outputSynapses = new ArrayList<>();

    protected final ReadWriteLock lock = new ReadWriteLock();

    protected double frequency;
    protected SampleSpace sampleSpace = new SampleSpace();

    protected boolean isNetworkInput; // Input Neurons won't be trained!

    protected boolean allowTraining = true;

    private Neuron<?, ?> template;

    private WeakHashMap<Long, WeakReference<SortedSet<A>>> activations = new WeakHashMap<>();

    public void addProvider(Model m) {
        if(provider == null)
            provider = new NeuronProvider(m, this);
        setModified();
    }

    public void register(A act) {
        Thought t = act.getThought();
        synchronized (activations) {
            WeakReference<SortedSet<A>> weakRef = activations
                    .computeIfAbsent(
                            t.getId(),
                            n -> new WeakReference<>(initActivationsSet(t))
                    );

            weakRef.get()
                    .add(act);
        }
    }

    public Stream<BindingSignal> getRelatedBindingSignals(PatternActivation fromOriginAct, State state) {
        Stream<BindingSignal> relatedBSs = fromOriginAct
                .getReverseBindingSignals(this, state);
        return relatedBSs
                .collect(Collectors.toList())
                .stream();
    }

    private TreeSet<A> initActivationsSet(Thought t) {
        TreeSet<A> acts = new TreeSet<>();
        t.register(provider, (TreeSet<Activation>) acts);
        return acts;
    }

    public SortedSet<A> getActivations(Thought t) {
        if(t == null)
            return Collections.emptySortedSet();

        WeakReference<SortedSet<A>> weakRef = activations.get(t.getId());
        if(weakRef == null)
            return Collections.emptyNavigableSet();

        SortedSet<A> acts = weakRef.get();
        if(acts == null)
            return Collections.emptyNavigableSet();

        return acts;
    }

    protected void initFromTemplate(Neuron n) {
        n.bias.setValue(bias.getCurrentValue());
        n.template = this;
    }

    public abstract A createActivation(Thought t);

    public abstract <N extends Neuron<S, A>> N instantiateTemplate(boolean addProvider);

    public abstract void addInactiveLinks(BindingSignal bs);

    public abstract ActivationFunction getActivationFunction();

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public Neuron getTemplate() {
        return template;
    }

    public boolean isOfTemplate(Neuron templateNeuron) {
        if(template == templateNeuron)
            return true;

        if(template == null)
            return false;

        return template.isOfTemplate(templateNeuron);
    }

    public double getCandidateGradient(Activation act) {
        Range range = act.getAbsoluteRange();
        assert range != null;

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

    public Stream<? extends Synapse> getTargetSynapses(Direction dir) {
        return dir.getSynapses(this);
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
        if(!modified)
            Save.add(this);

        modified = true;
    }

    public void resetModified() {
        this.modified = false;
    }

    public boolean isModified() {
        return modified;
    }

    public Field getBias() {
        return bias;
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public void count(Activation act) {
        double oldN = sampleSpace.getN();

        Range absoluteRange = act.getAbsoluteRange();
        sampleSpace.countSkippedInstances(absoluteRange);

        sampleSpace.count();
        frequency += 1.0;

        Double alpha = act.getConfig().getAlpha();
        if (alpha != null)
            applyMovingAverage(
                    Math.pow(alpha, sampleSpace.getN() - oldN)
            );

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

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(getClass().getCanonicalName());

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
        String neuronClazz = in.readUTF();
        Neuron n = (Neuron) m.modelClass(neuronClazz);

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

    @Override
    public Timestamp getCreated() {
        return MIN;
    }

    @Override
    public Timestamp getFired() {
        return MAX;
    }

    @Override
    public Thought getThought() {
        return getModel().getCurrentThought();
    }


    public <N extends Neuron> N init(Model m, String label) {
        addProvider(m);
        setLabel(label);
        return (N) this;
    }

    public <N extends Neuron> N init(Model m, String label, boolean isNetworkInput) {
        addProvider(m);
        setLabel(label);
        setNetworkInput(isNetworkInput);
        return (N) this;
    }

    public Neuron updateBias(double bias) {
        getBias().receiveUpdate(bias);
        return this;
    }

    public String toKeyString() {
        return getId() + ":" + (getLabel() != null ? getLabel() : "--");
    }

    public String toString() {
        return getClass().getSimpleName() + " " + toKeyString();
    }
}
