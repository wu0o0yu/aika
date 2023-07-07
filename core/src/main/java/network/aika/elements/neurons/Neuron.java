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
package network.aika.elements.neurons;

import network.aika.Model;
import network.aika.Thought;
import network.aika.elements.synapses.CategoryInputSynapse;
import network.aika.elements.synapses.CategorySynapse;
import network.aika.fields.*;
import network.aika.elements.activations.Activation;
import network.aika.elements.Element;
import network.aika.elements.links.Link;
import network.aika.elements.activations.Timestamp;
import network.aika.elements.synapses.Synapse;
import network.aika.visitor.ActLinkingOperator;
import network.aika.visitor.LinkLinkingOperator;
import network.aika.steps.activation.Save;
import network.aika.utils.Writable;
import network.aika.visitor.linking.LinkingOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.elements.synapses.Synapse.getLatentLinkingPreNet;
import static network.aika.elements.activations.Timestamp.MAX;
import static network.aika.elements.activations.Timestamp.MIN;
import static network.aika.steps.Phase.TRAINING;
import static network.aika.utils.Utils.TOLERANCE;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Neuron<A extends Activation> implements Element, Writable {

    protected static final Logger log = LoggerFactory.getLogger(Neuron.class);

    volatile long retrievalCount = 0;

    private volatile boolean modified;

    private NeuronProvider provider;

    private String label;

    private Writable customData;

    protected MultiInputField bias = initBias();

    protected boolean allowTraining = true;

    protected Neuron<?> template;

    private final WeakHashMap<Long, WeakReference<PreActivation<A>>> activations = new WeakHashMap<>();

    private boolean callActivationCheckCallback;


    public Long getId() {
        return provider.getId();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isCallActivationCheckCallback() {
        return callActivationCheckCallback;
    }

    public void setCallActivationCheckCallback(boolean callActivationCheckCallback) {
        this.callActivationCheckCallback = callActivationCheckCallback;
    }
    @Override
    public void disconnect() {
    }

    public void addProvider(Model m) {
        if (provider == null)
            provider = new NeuronProvider(m, this);
        setModified();
    }

    public void register(A act) {
        Thought t = act.getThought();
        PreActivation<A> npd = getOrCreatePreActivation(t);
        npd.addActivation(act);
    }

    public PreActivation<A> getOrCreatePreActivation(Thought t) {
        PreActivation<A> npd;
        synchronized (activations) {
            WeakReference<PreActivation<A>> weakRef = activations
                    .computeIfAbsent(
                            t.getId(),
                            n -> new WeakReference<>(
                                    new PreActivation<>(t, provider)
                            )
                    );

            npd = weakRef.get();
        }
        return npd;
    }

    public PreActivation<A> getPreActivation(Thought t) {
        PreActivation<A> npd = null;
        synchronized (activations) {
            WeakReference<PreActivation<A>> weakRef = activations
                    .get(t.getId());

            if(weakRef != null)
                npd = weakRef.get();
        }
        return npd;
    }

    public Stream<PreActivation<A>> getPreActivations() {
        synchronized (activations) {
            return activations.values()
                    .stream()
                    .map(Reference::get)
                    .filter(Objects::nonNull);
        }
    }

    public abstract void startVisitor(LinkingOperator c, Activation act, Synapse syn);

    public void linkOutgoing(Synapse synA, Activation iAct) {
        synA.getOutput().startVisitor(
                new LinkLinkingOperator(iAct, synA),
                iAct,
                synA
        );
    }

    public void latentLinkOutgoing(Synapse synA, Activation iActA) {
        getInputSynapsesAsStream()
                .filter(synB -> synA != synB)
                .filter(synB -> !synB.isFeedback())
                .filter(synB -> getLatentLinkingPreNet(synA, synB) > 0.0)
                .forEach(synB ->
                        synB.getOutput().startVisitor(
                                new ActLinkingOperator(iActA, synA, null, synB),
                                iActA,
                                synB
                        )
                );
    }

    public void linkAndPropagateIn(Link l) {
        getInputSynapsesAsStream()
                .filter(synB -> synB != l.getSynapse())
                .forEach(synB ->
                        startVisitor(
                                new ActLinkingOperator(l.getInput(), l.getSynapse(), l, synB),
                                l.getInput(),
                                synB
                        )
                );
    }

    public SortedSet<A> getActivations(Thought t) {
        if(t == null)
            return Collections.emptySortedSet();

        WeakReference<PreActivation<A>> weakRef = activations.get(t.getId());
        if(weakRef == null)
            return Collections.emptyNavigableSet();

        PreActivation<A> acts = weakRef.get();
        if(acts == null)
            return Collections.emptyNavigableSet();

        return acts.getActivations();
    }

    public <N extends Neuron<A>> N  instantiateTemplate() {
        N n;
        try {
            n = (N) getClass().getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        n.initFromTemplate(this);
        return n;
    }

    protected void initFromTemplate(Neuron templateN) {
        addProvider(templateN.getModel());

        bias.setInitialValue(
                templateN.getBias().getUpdatedValue()
        );

        CategoryInputSynapse cis = templateN.getCategoryInputSynapse();
        createCategorySynapse()
                .setWeight(10.0)
                .init(this, cis.getInput());

        this.template = templateN;
    }

    public abstract CategorySynapse createCategorySynapse();

    public boolean isAbstract() {
        return getCategoryInputSynapse() != null;
    }

    public abstract CategoryInputSynapse getCategoryInputSynapse();

    public abstract CategorySynapse getCategoryOutputSynapse();

    public boolean isTrainingAllowed() {
        return true;
    }

    public abstract A createActivation(Thought t);

    public abstract void addInactiveLinks(Activation act);

    public abstract ActivationFunction getActivationFunction();

    public void count(A act) {
    }

    protected MultiInputField initBias() {
        return (MultiInputField) new QueueSumField(this, TRAINING, "bias", TOLERANCE)
                .addListener("onBiasModified", (fl, nr, u) ->
                        setModified()
                );
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public Neuron getTemplate() {
        return template;
    }


    public NeuronProvider getProvider() {
        return provider;
    }

    public NeuronProvider getProvider(boolean permanent) {
        provider.setPermanent(permanent);
        return provider;
    }

    public void setProvider(NeuronProvider p) {
        this.provider = p;
    }

    public Stream<Synapse> getInputSynapsesAsStream() {
        return getInputSynapses().stream();
    }

    public Collection<Synapse> getInputSynapses() {
        return provider.inputSynapses.values();
    }

    public Stream<? extends Synapse> getOutputSynapsesAsStream() {
        return getOutputSynapses().stream();
    }

    public Collection<? extends Synapse> getOutputSynapses() {
        return provider.outputSynapses.values();
    }

    public Stream<? extends Synapse> getOutputSynapsesAsStream(Thought t) {
        WeakReference<PreActivation<A>> wRefNpd = activations.get(t.getId());
        if(wRefNpd == null)
            return getOutputSynapsesAsStream();

        PreActivation<A> npd = wRefNpd.get();
        if(npd == null)
            return getOutputSynapsesAsStream();

        return Stream.concat(
                npd.getOutputSynapses(),
                getOutputSynapsesAsStream()
        );
    }

    public Synapse getOutputSynapse(NeuronProvider n) {
        provider.lock.acquireReadLock();
        Synapse syn = getOutputSynapsesAsStream()
                .filter(s -> s.getPOutput().getId() == n.getId())
                .findFirst()
                .orElse(null);
        provider.lock.releaseReadLock();
        return syn;
    }

    public Synapse getInputSynapse(NeuronProvider n) {
        provider.lock.acquireReadLock();
        Synapse syn = selectInputSynapse(s ->
                s.getPInput().getId() == n.getId()
        );

        provider.lock.releaseReadLock();
        return syn;
    }

    public <IS extends Synapse> IS getInputSynapseByType(Class<IS> synapseType) {
        return getInputSynapsesByType(synapseType)
                .findAny()
                .orElse(null);
    }

    public <IS extends Synapse> Stream<IS> getInputSynapsesByType(Class<IS> synapseType) {
        return getProvider().getInputSynapses()
                .filter(synapseType::isInstance)
                .map(synapseType::cast);
    }

    public <OS> OS getOutputSynapseByType(Class<OS> synapseType) {
        return getProvider().getOutputSynapses()
                .filter(synapseType::isInstance)
                .map(synapseType::cast)
                .findAny()
                .orElse(null);
    }

    protected Synapse selectInputSynapse(Predicate<? super Synapse> predicate) {
        return getInputSynapsesAsStream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    public void addInputSynapse(Synapse s) {
        setModified();
    }

    public void removeInputSynapse(Synapse s) {
        setModified();
    }

    public void addOutputSynapse(Synapse s) {
        setModified();
    }

    public void removeOutputSynapse(Synapse s) {
        setModified();
    }

    public void delete() {
        log.info("Delete Neuron: " + this);
        provider.getInputSynapses().forEach(Synapse::unlinkInput);
        provider.getOutputSynapses().forEach(Synapse::unlinkOutput);
    }

    public Writable getCustomData() {
        return customData;
    }

    public void setCustomData(Writable customData) {
        this.customData = customData;
    }

    public <M extends Model> M getModel() {
        if(provider == null)
            return null;

        return (M) provider.getModel();
    }

    public long getRetrievalCount() {
        return retrievalCount;
    }


    public void setModified() {
        if (!modified)
            Save.add(this);

        modified = true;
    }

    public void resetModified() {
        this.modified = false;
    }

    public boolean isModified() {
        return modified;
    }

    public MultiInputField getBias() {
        return bias;
    }

    public double getCurrentCompleteBias() {
        return getBias().getUpdatedValue();
    }

    public void suspend() {
        for (Synapse s : getInputSynapsesAsStream()
                .filter(s -> s.getStoredAt() == OUTPUT)
                .toList()
        ) {
            provider.removeInputSynapse(s);
            s.getPInput().removeOutputSynapse(s);
        }
        for (Synapse s : getOutputSynapsesAsStream()
                .filter(s -> s.getStoredAt() == INPUT)
                .toList()
        ) {
            provider.removeOutputSynapse(s);
            s.getPOutput().removeInputSynapse(s);
        }
    }

    public void reactivate(Model m) {
        m.incrementRetrievalCounter();
        retrievalCount = m.getCurrentRetrievalCount();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(getClass().getCanonicalName());

        out.writeBoolean(label != null);
        if(label != null)
            out.writeUTF(label);

        bias.write(out);

        for (Synapse s : getInputSynapses()) {
            if (s.getStoredAt() == OUTPUT) {
                out.writeBoolean(true);
                s.write(out);
            }
        }
        out.writeBoolean(false);

        for (Synapse s : getOutputSynapses()) {
            if (s.getStoredAt() == INPUT) {
                out.writeBoolean(true);
                s.write(out);
            }
        }
        out.writeBoolean(false);

        out.writeBoolean(customData != null);
        if(customData != null)
            customData.write(out);

        out.writeBoolean(callActivationCheckCallback);
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
            Synapse syn = Synapse.read(in, m);
            syn.link();
        }

        while (in.readBoolean()) {
            Synapse syn = Synapse.read(in, m);
            syn.link();
        }

        if(in.readBoolean()) {
            customData = m.getCustomDataInstanceSupplier().get();
            customData.readFields(in, m);
        }

        callActivationCheckCallback = in.readBoolean();
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
        Model m = getModel();
        if(m == null)
            return null;

        return m.getCurrentThought();
    }

    public <N extends Neuron> N init(Model m, String label) {
        addProvider(m);
        setLabel(label);
        setBias(0.0);
//        connect(INPUT, true, false);
        return (N) this;
    }

    public <N extends Neuron> N setBias(double bias) {
        getBias().setValue(bias);
        return (N) this;
    }

    public String toKeyString() {
        return getId() + ":" + (getLabel() != null ? getLabel() : "--");
    }

    public String toString() {
        return getClass().getSimpleName() + " " + toKeyString();
    }
}
