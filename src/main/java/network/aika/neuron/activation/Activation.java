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
import network.aika.neuron.ActivationFunction;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.activation.*;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.activation.PatternActivation.MAX_PATTERN_ACT;
import static network.aika.neuron.activation.PatternActivation.MIN_PATTERN_ACT;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.utils.Utils.logChange;

/**
 * @author Lukas Molzberger
 */
public abstract class Activation<N extends Neuron> extends Element<Activation> {

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(Activation::getId);

    protected double value = 0.0;
    protected Double inputValue = null;
    protected double net;
    protected double lastNet = 0.0;
    protected Fired fired = NOT_FIRED;

    protected final int id;
    protected N neuron;
    protected Thought thought;

    Map<NeuronProvider, Link> inputLinks;
    NavigableMap<OutputKey, Link> outputLinks;

    private Reference reference;

    protected SortedMap<Activation, Byte> bindingSignals = new TreeMap<>(
            Comparator.<Activation, Byte>comparing(act -> act.getType())
                    .thenComparing(Activation::getId)
    );
    protected Map<Activation, Byte> reverseBindingSignals = new TreeMap<>();

    public final static int OWN = 0;
    public final static int INCOMING = 1;

    private double lastEntropyGradient = 0.0;
    private double[] inputGradient = new double[2];

    /**
     * Accumulates all gradients in case a new link is added that needs be get informed about the gradient.
     */
    private double[] outputGradientSum;
    private double[] inputGradientSum;

    public boolean markedNetUpdateOccurred; // Temporary hack


    protected Activation(int id, N n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(int id, Thought t, N n) {
        this(id, n);
        this.thought = t;

        lastNet = n.isTemplate() || n.isInputNeuron() ? 0.0 : -1.0;
        net = n.getInitialNet();

        thought.registerActivation(this);

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);
    }

    public abstract byte getType();

    public void initInput(Reference ref) {
        setReference(ref);

        setInputValue(1.0);
        setFired(ref.getRelativeBegin());

        updateValue();

        EntropyGradient.add(this);

        CheckIfFired.propagate(this);
    }

    public void addFeedbackSteps() {}

    public int getId() {
        return id;
    }

    public double getValue() {
        return value;
    }

    public double getNet() {
        return net;
    }

    public double[] getInputGradient() {
        return inputGradient;
    }

    public double[] getOutputGradientSum() {
        return outputGradientSum;
    }

    public Fired getFired() {
        return fired;
    }

    public boolean isFired() {
        return fired != NOT_FIRED;
    }

    public void setFired(int inputTimestamp) {
        setFired(new Fired(inputTimestamp, 0));
    }

    public void setFired(Fired f) {
        fired = f;
    }

    public void setFired() {
        Fired latestFired = inputLinks.values().stream()
                .map(il -> il.getInput().getFired())
                .max(Fired.COMPARATOR)
                .orElse(null);

        setFired(neuron.incrementFired(latestFired));
    }

    public Thought getThought() {
        return thought;
    }

    public boolean isSelfRef(Activation iAct) {
        return false;
    }


    public void addBindingSignal(Activation bindingSignal, Byte scope) {
        bindingSignals.put(bindingSignal, scope);
        bindingSignal.reverseBindingSignals.put(this, scope);
    }

    public void addBindingSignals(Map<Activation, Byte> bindingsSignals) {
        bindingSignals.putAll(bindingsSignals);
        bindingsSignals.entrySet().stream().forEach(e ->
                e.getKey().reverseBindingSignals.put(this, e.getValue())
        );
    }


    public Stream<Map.Entry<Activation, Byte>> getPatternBindingSignals() {
        return bindingSignals.subMap(MIN_PATTERN_ACT, MAX_PATTERN_ACT).entrySet().stream();
    }

    public Stream<Map.Entry<Activation, Byte>> getBranchBindingSignals() {
        return bindingSignals.subMap(MIN_PATTERN_ACT, MAX_PATTERN_ACT).entrySet().stream();
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

    public <R extends Reference> R getReference() {
        return (R) reference;
    }

    public void setReference(Reference ref) {
        assert ref.getThought() == getThought();
        this.reference = ref;
    }

    public void propagateReference(Reference ref) {
        setReference(ref);
        getModel().linkInputRelations(this, INPUT);
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

    public Activation clone(Synapse excludedSyn) {
        if (!isFired())
            return this;

        Activation clonedAct = newInstance();

        replaceElement(clonedAct);

        linkClone(clonedAct, excludedSyn);
        thought.onActivationCreationEvent(clonedAct, this);

        return clonedAct;
    }

    protected abstract Activation newInstance();

    protected void linkClone(Activation clonedAct, Synapse excludedSyn) {
        inputLinks
                .values()
                .stream()
                .filter(l -> l.getSynapse() != excludedSyn)
                .forEach(l -> {
                            Link nl = new Link(l.getSynapse(), l.getInput(), clonedAct, l.isSelfRef());
                            clonedAct.updateNet(nl.getInputValue(POS));
                        }
                );
    }

    public void setInputValue(double v) {
        inputValue = v;
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
        Link nl = new Link(
                s,
                input,
                this,
                isSelfRef
        );

        nl.updateNetByInputValue(
                Link.getInputValueDelta(POS, nl, ol)
        );

        return nl;
    }

    public void updateNet(double netDelta) {
        double oldNet = net;
        net += netDelta;
        logChange(neuron, oldNet, net, "updateNet: net");
    }

    protected double computeValue() {
        return getActivationFunction().f(net);
    }

    public void updateValue() {
        double oldValue = value;

        value = inputValue != null ?
                inputValue :
                computeValue();

        logChange(neuron, oldValue, value, "updateValue: value");

        PropagateValueChange.add(this, value - oldValue);
    }

    public void initEntropyGradient() {
        double g = getNeuron().getSurprisal(
                        Sign.getSign(this),
                        getReference()
                );

        inputGradient[OWN] += g - lastEntropyGradient;
        lastEntropyGradient = g;
    }

    public double[] gradientsFromSumUpdate() {
        ActivationFunction actF = getActivationFunction();

        inputGradientSum = Utils.add(inputGradientSum, inputGradient);
        double[] g = inputGradient;
        inputGradient = new double[2];

        return Utils.scale(g, actF.outerGrad(lastNet));
    }

    public double[] gradientsFromNetUpdate() {
        if(inputGradientSum == null)
            return null;

        ActivationFunction actF = getActivationFunction();

        double g = actF.outerGrad(net) - actF.outerGrad(lastNet);
        lastNet = net;

        return Utils.scale(inputGradientSum, g);
    }

    public void updateOutputGradientSum(double[] g) {
        outputGradientSum = Utils.add(outputGradientSum, g);
    }

    public void propagateGradientIn(double g) {
        inputGradient[INCOMING] += g;

        if(Utils.belowTolerance(inputGradient))
            return;

        PropagateGradientsSum.add(this);
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
        sb.append(" value:" + Utils.round(value, 10000.0));
        sb.append(" net:" + Utils.round(net));

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
