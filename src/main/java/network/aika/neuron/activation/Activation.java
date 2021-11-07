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
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.activation.*;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.neuron.activation.BindingActivation.MAX_BINDING_ACT;
import static network.aika.neuron.activation.BindingActivation.MIN_BINDING_ACT;
import static network.aika.neuron.activation.PatternActivation.MAX_PATTERN_ACT;
import static network.aika.neuron.activation.PatternActivation.MIN_PATTERN_ACT;
import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.utils.Utils.logChange;

/**
 * @author Lukas Molzberger
 */
public abstract class Activation<N extends Neuron> extends Element<Activation> {

    public static double INITIAL_NET = -0.001;

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(Activation::getId);

    protected double value = 0.0;
    protected Double inputValue = null;
    protected double net;
    protected double lastNet = INITIAL_NET;

    protected Timestamp creationTimestamp = NOT_SET;
    protected Timestamp fired = NOT_SET;

    protected final int id;
    protected N neuron;
    protected Thought thought;

    Map<NeuronProvider, Link> inputLinks;
    NavigableMap<OutputKey, Link> outputLinks;

    protected SortedMap<Activation<?>, BindingSignal> bindingSignals = new TreeMap<>(
            Comparator.<Activation, Byte>comparing(act -> act.getType())
                    .thenComparing(Activation::getId)
    );
    protected Map<Activation<?>, BindingSignal> reverseBindingSignals = new TreeMap<>();

    public final static int OWN = 0;
    public final static int INCOMING = 1;

    private Gradient entropy = new Gradient();

    private Gradient ownInputGradient = new Gradient();
    private Gradient incomingInputGradient = new Gradient();

    private Gradient ownOutputGradient = new Gradient();
    private Gradient incomingOutputGradient = new Gradient();

 //   private double[] inputGradient = new double[2];

    /**
     * Accumulates all gradients in case a new link is added that needs be get informed about the gradient.
     */
 //   private double[] outputGradientSum;
 //  private double[] inputGradientSum;

    public boolean markedNetUpdateOccurred; // Temporary hack


    protected Activation(int id, N n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(int id, Thought t, N n) {
        this(id, n);
        this.thought = t;

        net = n.getInitialNet();

        thought.registerActivation(this);

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);
    }

    public void init(Synapse originSynapse, Activation originAct) {
        setCreationTimestamp();
        thought.onActivationCreationEvent(this, originSynapse, originAct);
    }

    public abstract byte getType();

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
/*
    public double[] getInputGradient() {
        return inputGradient;
    }

    public double[] getOutputGradientSum() {
        return outputGradientSum;
    }
*/
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
        Range range = getAbsoluteRange();
        assert range != null;

        entropy.set(getNeuron().getSurprisal(
                        Sign.getSign(this),
                        range
                ));

        entropy.propagate(ownInputGradient);
    }

    public double[] gradientsFromSumUpdate() {
        ActivationFunction actF = getActivationFunction();

        inputGradientSum = Utils.add(inputGradientSum, inputGradient);
        double[] g = inputGradient;
        inputGradient = new double[2];

        return Utils.scale(g, actF.outerGrad(net));

        outputGradient.set();
    }

    public double[] gradientsFromNetUpdate() {
        if(inputGradientSum == null)
            return null;

        ActivationFunction actF = getActivationFunction();

        double g = actF.outerGrad(net) - actF.outerGrad(lastNet);
        lastNet = net;

        return Utils.scale(inputGradientSum, g);

        outputGradient.set();
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
