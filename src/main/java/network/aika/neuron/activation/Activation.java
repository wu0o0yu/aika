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
package network.aika.neuron.activation;

import network.aika.Config;
import network.aika.Model;
import network.aika.Thought;
import network.aika.fields.*;
import network.aika.neuron.*;
import network.aika.direction.Direction;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.BranchBindingSignal;
import network.aika.neuron.bindingsignal.PatternBindingSignal;
import network.aika.neuron.bindingsignal.Scope;
import network.aika.sign.Sign;
import network.aika.steps.activation.*;
import network.aika.steps.link.InformationGainGradient;
import network.aika.steps.link.LinkCounting;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.steps.LinkingOrder.POST_FIRED;
import static network.aika.steps.LinkingOrder.PRE_FIRED;

/**
 * @author Lukas Molzberger
 */
public abstract class Activation<N extends Neuron> extends Element<Activation> {

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(Activation::getId);

    protected final int id;
    protected N neuron;
    protected Thought thought;

    protected Timestamp creationTimestamp = NOT_SET;
    protected Timestamp fired = NOT_SET;

    protected boolean isInput;
    private boolean finalMode = false;

    protected Field value = new Field();
    protected Field net = new QueueField(this, "net");

    protected Map<NeuronProvider, Link> inputLinks;
    protected NavigableMap<OutputKey, Link> outputLinks;

    protected SortedMap<Activation<?>, PatternBindingSignal> patternBindingSignals = new TreeMap<>(
            Comparator.comparing(Activation::getId)
    );
    protected SortedMap<Activation<?>, BranchBindingSignal> branchBindingSignals = new TreeMap<>(
            Comparator.comparing(Activation::getId)
    );

    private Field entropy = new Field();
    protected Field inputGradient = new QueueField(this, "inputGradient");

    protected MultiSourceFieldOutput outputGradientMul = new FieldMultiplication(
            inputGradient,
            new FieldFunction(net, x ->
                    getNeuron().getActivationFunction().outerGrad(x)
            )
    );
    protected Field outputGradient = new QueueField(this, "outputGradient");

    protected Activation(int id, N n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(int id, Thought t, N n) {
        this(id, n);
        this.thought = t;

        entropy.setFieldListener(u -> receiveOwnGradientUpdate(u));

        initNet();

        value.setFieldListener(u ->
                getOutputLinks()
                        .forEach(l -> l.propagateValue())
        );

        inputGradient.setFieldListener(u -> {
                    if (outputGradientMul.updateAvailable(1))
                        outputGradient.addAndTriggerUpdate(outputGradientMul.getUpdate(1));
                }
        );

        outputGradient.setFieldListener(u ->
                propagateGradient(u, true, true)
        );

        thought.register(this);
        neuron.register(this);

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);
    }

    private void initNet() {
        net.setPropagatePreCondition((cv, nv, u) ->
                !Utils.belowTolerance(u) && (cv >= 0.0 || nv >= 0.0)
        );
        net.add(getNeuron().getBias().getCurrentValue());

        net.setFieldListener(u -> {
            double v = net.getNewValue();
            updateValue(v);

            propagateGradient();

            if (net.getCurrentValue() <= 0.0)
                return;

            setFired();
        });
    }

    public boolean checkPropagatePatternBindingSignal(PatternBindingSignal bs) {
        return true;
    }

    public boolean checkPropagateBranchBindingSignal(BranchBindingSignal bs) {
        return true;
    }

    protected void updateValue(double net) {
        if(!isInput)
            value.setAndTriggerUpdate(getActivationFunction().f(net));
    }


    protected void propagateGradient() {
        if(outputGradientMul.updateAvailable(2))
            outputGradient.addAndTriggerUpdate(outputGradientMul.getUpdate(2));
    }

    protected void propagateGradient(double g, boolean updateWeights, boolean backPropagate) {
        getNeuron().getBias().addAndTriggerUpdate(getConfig().getLearnRate() * g);

        inputLinks.values().forEach(l -> {
                    if (!l.getSynapse().isAllowTraining())
                        return;

                    if (updateWeights)
                        l.updateWeight(g);

                    if (backPropagate)
                        l.backPropagate();
                }
        );
    }

    public void init(Synapse originSynapse, Activation originAct) {
        setCreationTimestamp();
        thought.onActivationCreationEvent(this, originSynapse, originAct);
    }

    public Field getEntropy() {
        return entropy;
    }

    public Field getInputGradient() {
        return inputGradient;
    }

    public void updateBias(double u) {
        getNet().addAndTriggerUpdate(u);
    }

    public void addFeedbackSteps() {}

    public int getId() {
        return id;
    }

    public Field getValue() {
        return value;
    }

    public boolean isInput() {
        return isInput;
    }

    public void setInput(boolean input) {
        isInput = input;
    }

    public Field getNet() {
        return net;
    }

    public Timestamp getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp() {
        this.creationTimestamp = thought.getCurrentTimestamp();
    }

    public boolean isFinal() {
        return finalMode;
    }

    public void setFinal() {
        if(finalMode)
            return;
        finalMode = true;

        onFinal();
    }

    public Timestamp getFired() {
        return fired;
    }

    public boolean isFired() {
        return fired != NOT_SET;
    }

    public void setFired() {
        if(isFired())
            return;

        fired = thought.getCurrentTimestamp();

        onFired();
    }

    protected void onFired() {
        if(isTemplate())
            Induction.add(this);

        Propagate.add(this, false);

        if(isFinal())
            onFinalFired();

        getBindingSignals()
                .forEach(bs ->
                        onBindingSignalArrivedFired(bs)
                );
    }

    protected void onFinal() {
        if(isFired())
            onFinalFired();
    }

    protected void onFinalFired() {
        Propagate.add(this, true);

        addEntropySteps();
        addFeedbackSteps();
        addCountingSteps();

        getBindingSignals()
                .forEach(bs ->
                        onBindingSignalArrivedFinalFired(bs)
                );
    }

    protected void onBindingSignalArrived(BindingSignal bs) {
        if(!getNeuron().isNetworkInput()) {
            Linking.add(this, bs, PRE_FIRED, false);
            Linking.add(this, bs, PRE_FIRED, true);
        }

        if(isFired())
            onBindingSignalArrivedFired(bs);
    }

    protected void onBindingSignalArrivedFired(BindingSignal bs) {
        Linking.add(this, bs, POST_FIRED, false);

        if(isFinal())
            onBindingSignalArrivedFinalFired(bs);
    }

    protected void onBindingSignalArrivedFinalFired(BindingSignal bs) {
        Linking.add(this, bs, POST_FIRED, true);
    }

    private void addEntropySteps() {
        EntropyGradient.add(this);

        inputLinks.values()
                .forEach(l ->
                        InformationGainGradient.add(l)
                );
    }

    private void addCountingSteps() {
        InactiveLinks.add(this);
        Counting.add(this);
        getInputLinks().forEach(l ->
                LinkCounting.add(l)
        );
    }

    public Thought getThought() {
        return thought;
    }

    public abstract boolean isSelfRef(Activation iAct);

    public boolean isNetworkInput() {
        return getNeuron().isNetworkInput();
    }

    public boolean isTemplate() {
        return getNeuron().isTemplate();
    }

    public boolean checkAllowPropagate() {
        return isFired();
    }

    public abstract Range getRange();

    public Range getAbsoluteRange() {
        Range r = getRange();
        if(r == null) return null;
        return r.getAbsoluteRange(thought.getRange());
    }

    public Stream<? extends BindingSignal> getBindingSignals() {
        return Stream.concat(
                getPatternBindingSignals().values().stream(),
                getBranchBindingSignals().values().stream()
        );
    }

    public BindingSignal addBindingSignal(BindingSignal bindingSignal) {
        if (bindingSignal.exists())
            return null;

        bindingSignal.link();
        return bindingSignal;
    }

    public void registerPatternBindingSignal(PatternBindingSignal bs) {
        onBindingSignalArrived(bs);
        patternBindingSignals.put(bs.getOriginActivation(), bs);
    }

    public void registerBranchBindingSignal(BranchBindingSignal bs) {
        onBindingSignalArrived(bs);
        branchBindingSignals.put(bs.getOriginActivation(), bs);
    }

    public Map<Activation<?>, PatternBindingSignal> getPatternBindingSignals() {
        return patternBindingSignals;
    }

    public Map<Activation<?>, BranchBindingSignal> getBranchBindingSignals() {
        return branchBindingSignals;
    }

    public abstract Stream<? extends BindingSignal<?>> getReverseBindingSignals();

    public boolean checkIfPrimaryPatternBindingSignalAlreadyExists() {
        return getPatternBindingSignals().values().stream()
                .anyMatch(mainBS -> mainBS.getScope() == Scope.INPUT);
    }

    @Override
    public int compareTo(Activation act) {
        return ID_COMPARATOR.compare(this, act);
    }

    public OutputKey getOutputKey() {
        return new OutputKey(getNeuronProvider(), getId());
    }

    public String getLabel() {
        return getNeuron().getLabel();
    }

    public N getNeuron() {
        return neuron;
    }

    public void setNeuron(N n) {
        this.neuron = n;
    }

    public ActivationFunction getActivationFunction() {
        return neuron.getActivationFunction();
    }

    public Model getModel() {
        return neuron.getModel();
    }

    public Config getConfig() {
        return getThought().getConfig();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
    }

    protected void linkClone(Activation clonedAct, Synapse excludedSyn) {
        inputLinks
                .values()
                .stream()
                .filter(l -> l.getSynapse() != excludedSyn)
                .forEach(l ->
                        l.getSynapse().createLink(l.getInput(), clonedAct)
                );
    }

    public void setInputValue(double v) {
        value.setAndTriggerUpdate(v);
        isInput = true;
    }

    public Link getInputLink(Neuron n) {
        return inputLinks.get(n.getProvider());
    }

    public Link getInputLink(Synapse s) {
        return inputLinks.get(s.getPInput());
    }

    public boolean inputLinkExists(Synapse s) {
        return inputLinks.containsKey(s.getPInput());
    }

    public boolean linkExists(Direction dir, Synapse ts, boolean template) {
        return template ?
                dir.getLinks(this)
                        .map(Link::getSynapse)
                        .anyMatch(s -> s.isOfTemplate(ts)) :
                !getOutputLinks(ts).isEmpty();
    }

    public SortedMap<OutputKey, Link> getOutputLinks(Synapse s) {
        return outputLinks
                .subMap(
                        new OutputKey(s.getOutput().getProvider(), Integer.MIN_VALUE),
                        true,
                        new OutputKey(s.getOutput().getProvider(), MAX_VALUE),
                        true
                );
    }

    public FieldOutput getOutputGradient() {
        return outputGradient;
    }

    public void updateEntropyGradient() {
        Range range = getAbsoluteRange();
        assert range != null;

        entropy.setAndTriggerUpdate(
                getNeuron().getSurprisal(
                        this,
                        Sign.getSign(this),
                        range
                )
        );
    }

    public void receiveOwnGradientUpdate(double u) {
        inputGradient.addAndTriggerUpdate(u);
    }

    public void linkInputs() {
        inputLinks
                .values()
                .forEach(Link::linkInput);
    }

    public void unlinkInputs() {
        inputLinks
                .values()
                .forEach(Link::unlinkInput);
    }

    public void linkOutputs() {
        outputLinks
                .values()
                .forEach(Link::linkOutput);
    }

    public void unlinkOutputs() {
        outputLinks
                .values()
                .forEach(Link::unlinkOutput);
    }

    public void link() {
        linkInputs();
        linkOutputs();
    }

    public void unlink() {
        unlinkInputs();
        unlinkOutputs();
    }

    public Stream<Link> getInputLinks() {
        return inputLinks.values().stream();
    }

    public Stream<Link> getOutputLinks() {
        return outputLinks.values().stream();
    }

    public String toString() {
        return (isTemplate() ? "Template-" : "") + getClass().getSimpleName() + " " + toKeyString();
    }

    public String toKeyString() {
        return "id:" + getId() + " n:[" + getNeuron().toKeyString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Activation)) return false;
        Activation<?> that = (Activation<?>) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
