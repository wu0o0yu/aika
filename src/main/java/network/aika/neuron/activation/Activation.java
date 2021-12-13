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
import network.aika.neuron.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.fields.*;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.StepType;
import network.aika.neuron.steps.activation.*;
import network.aika.neuron.steps.link.LinkCounting;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.neuron.activation.BindingActivation.MAX_BINDING_ACT;
import static network.aika.neuron.activation.BindingActivation.MIN_BINDING_ACT;
import static network.aika.neuron.activation.PatternActivation.MAX_PATTERN_ACT;
import static network.aika.neuron.activation.PatternActivation.MIN_PATTERN_ACT;
import static network.aika.neuron.activation.Timestamp.NOT_SET;

/**
 * @author Lukas Molzberger
 */
public abstract class Activation<N extends Neuron> extends Element<Activation> {

//    public static double INITIAL_NET = -0.001;

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(Activation::getId);

    protected final int id;
    protected N neuron;
    protected Thought thought;

    protected Timestamp creationTimestamp = NOT_SET;
    protected Timestamp fired = NOT_SET;

    protected boolean isInput;

    protected Field value = new Field();
    protected Field net = new QueueField(this, "net", Phase.LINKING, StepType.INFERENCE);

    protected Map<NeuronProvider, Link> inputLinks;
    protected NavigableMap<OutputKey, Link> outputLinks;

    protected SortedMap<Activation<?>, BindingSignal> bindingSignals = new TreeMap<>(
            Comparator.<Activation, Byte>comparing(act -> act.getType())
                    .thenComparing(Activation::getId)
    );
    protected Map<Activation<?>, BindingSignal> reverseBindingSignals = new TreeMap<>();

    private Field entropy = new Field();
    protected Field inputGradient = new QueueField(this, "inputGradient", Phase.LINKING, StepType.TRAINING);

    protected FieldOutput outputGradient = new FieldMultiplication(
            inputGradient,
            new FieldFunction(net, x ->
                    getNeuron().getActivationFunction().outerGrad(x)
            )
    );

    protected Activation(int id, N n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(int id, Thought t, N n) {
        this(id, n);
        this.thought = t;

        initEntropy();
        initNet();
        initValue();
        initInputGradient();

        thought.register(this);
        neuron.register(this);

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);
    }

    private void initEntropy() {
        entropy.setFieldListener(u -> receiveOwnGradientUpdate(u));
    }

    private void initInputGradient() {
        inputGradient.setFieldListener(u -> {
                    if (outputGradient.updateAvailable(1))
                        propagateGradient(outputGradient.getUpdate(1, true), true, true);
                }
        );
    }

    private void initValue() {
        value.setFieldListener(u ->
                getOutputLinks()
                        .forEach(l -> l.propagateValue())
        );
    }

    private void initNet() {
        net.setPropagatePreCondition((cv, nv, u) ->
                !Utils.belowTolerance(u) && (cv >= 0.0 || nv >= 0.0)
        );
        net.add(getNeuron().getBias().getCurrentValue());

        net.setFieldListener(u -> {
            double v = net.getNewValue(true);
            if(!isInput)
                value.setAndTriggerUpdate(getBranchProbability() * getActivationFunction().f(v));

            propagateGradient();

            if (isFired() || net.getCurrentValue() <= 0.0)
                return;

            setFired();
            propagate();
        });
    }

    protected void propagateGradient() {
        if(outputGradient.updateAvailable(2))
            propagateGradient(outputGradient.getUpdate(2, true), true, true);
    }

    public void propagate() {
        Propagate.add(this);

        addFeedbackSteps();

        Counting.add(this);
        getInputLinks().forEach(l -> LinkCounting.add(l));

        TemplatePropagate.add(this);
        EntropyGradient.add(this);
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

        if(isFired())
            TemplatePropagate.add(this);
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

    public void updateBias(double u, boolean isFinalBias) {
        getNet().addAndTriggerUpdate(u);
    }

    public void addFeedbackSteps() {}

    public abstract byte getType();

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

    public Timestamp getFired() {
        return fired;
    }

    public boolean isFired() {
        return fired != NOT_SET;
    }

    public void setFired(Timestamp f) {
        fired = f;
    }

    public void setFired() {
        setFired(thought.getCurrentTimestamp());
    }

    public Thought getThought() {
        return thought;
    }

    public boolean isSelfRef(Activation iAct) {
        return bindingSignals.containsKey(iAct);
    }

    public abstract Range getRange();

    public Range getAbsoluteRange() {
        Range r = getRange();
        if(r == null) return null;
        return r.getAbsoluteRange(thought.getRange());
    }

    public void addSelfBindingSignal(byte scope) {
        addBindingSignal(new BindingSignal(null, this, this, scope, (byte) 0));
    }

    public void addBindingSignals(Stream<BindingSignal> bindingsSignals) {
        bindingsSignals.forEach(this::addBindingSignal);
    }

    public void addBindingSignal(BindingSignal bindingSignal) {
        if (checkIfBindingSignalExists(bindingSignal))
            return;

        bindingSignal.link();
        thought.registerBindingSignal(this, bindingSignal);
    }

    public boolean checkIfBindingSignalExists(BindingSignal bindingSignal) {
        BindingSignal existingBSScope = bindingSignals.get(bindingSignal.getBindingSignalAct());
        return existingBSScope != null && existingBSScope.getScope() <= bindingSignal.getScope();
    }

    protected void registerBindingSignal(Activation targetAct, BindingSignal bindingSignal) {
        reverseBindingSignals.put(targetAct, bindingSignal);

        Linking.add(targetAct, bindingSignal);
        TemplateLinking.add(targetAct, bindingSignal);
    }

    public Map<Activation<?>, BindingSignal> getBindingSignals() {
        return bindingSignals;
    }

    public Map<Activation<?>, BindingSignal> getPatternBindingSignals() {
        return bindingSignals.subMap(MIN_PATTERN_ACT, MAX_PATTERN_ACT);
    }

    public Map<Activation<?>, BindingSignal> getBranchBindingSignals() {
        return bindingSignals.subMap(MIN_BINDING_ACT, MAX_BINDING_ACT);
    }

    public Map<Activation<?>, BindingSignal> getReverseBindingSignals() {
        return reverseBindingSignals;
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
                        new Link(l.getSynapse(), l.getInput(), clonedAct, l.isSelfRef())
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

    public boolean templateLinkExists(Direction dir, Synapse ts) {
        return dir.getLinks(this)
                .map(Link::getSynapse)
                .anyMatch(s -> s.isOfTemplate(ts));
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

    public Link addLink(Synapse s, Activation input, boolean isSelfRef) {
        Link ol = getInputLink(s);
        assert ol == null;

        return new Link(
                s,
                input,
                this,
                isSelfRef
        );
    }


    public FieldOutput getOutputGradient() {
        return outputGradient;
    }

    public double getBranchProbability() {
        return 1.0;
    }

    public void updateEntropyGradient() {
        Range range = getAbsoluteRange();
        assert range != null;

        entropy.setAndTriggerUpdate(
                getNeuron().getSurprisal(
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

    public String toShortString() {
        return "Act id:" +
                getId() +
                " n:[" + getNeuron() + "]";
    }

    public String gradientsToString() {
        StringBuilder sb = new StringBuilder();

        inputLinks.values()
                .forEach(l -> {
                    sb.append(l.gradientsToString());
                    sb.append(" \n");
                });

        sb.append("\n");
        return sb.toString();
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean includeLink) {
        StringBuilder sb = new StringBuilder();
        sb.append("act ");
        sb.append(toShortString());
        sb.append(" value:" + value);
        sb.append(" net:" + net);

        if (includeLink) {
            sb.append("\n");
            getInputLinks().forEach(l ->
                    sb.append("   " + l.toDetailedString() + "\n")
            );
            sb.append("\n");
        }

        return sb.toString();
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
