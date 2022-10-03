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

import network.aika.Model;
import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.fields.*;
import network.aika.neuron.*;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.BSKey;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.conjunctive.NegativeFeedbackSynapse;
import network.aika.sign.Sign;
import network.aika.steps.activation.Counting;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.direction.Direction.DIRECTIONS;
import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.*;
import static network.aika.fields.LinkSlotMode.MAX;
import static network.aika.fields.LinkSlotMode.MIN;
import static network.aika.fields.ThresholdOperator.Type.*;
import static network.aika.neuron.bindingsignal.BSKey.COMPARATOR;
import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.neuron.bindingsignal.State.ABSTRACT_SAME;

/**
 * @author Lukas Molzberger
 */
public abstract class Activation<N extends Neuron> implements Element, Comparable<Activation> {

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(Activation::getId);

    protected final int id;
    protected N neuron;
    protected Thought thought;

    protected Timestamp created = NOT_SET;
    protected Timestamp fired = NOT_SET;

    protected Field valueUB;
    protected Field valueLB;

    protected QueueField netUB;
    protected QueueField netLB;

    private FieldOutput netDiff;

    protected FieldOutput isFired;
    protected FieldOutput isFiredForWeight;
    protected FieldOutput isFiredForBias;

    protected FieldOutput isFinal;

    private FieldFunction entropy;
    protected FieldFunction netOuterGradient;
    protected QueueField ownInputGradient;
    protected QueueField backpropInputGradient;
    protected QueueField ownOutputGradient;
    protected QueueField backpropOutputGradient;
    protected FieldOutput outputGradient;
    protected FieldOutput updateValue;
    protected FieldOutput inductionThreshold;

    protected SlotField sameBSSlot = new SlotField(this, "sameBSSlot");


    protected Map<NeuronProvider, Link> inputLinks;
    protected NavigableMap<OutputKey, Link> outputLinks;

    protected SortedMap<BSKey, BindingSignal> bindingSignals = new TreeMap<>(COMPARATOR);

    private static final Comparator<Synapse> SYN_COMP = Comparator.comparing(s -> s.getInput().getId());
    protected Map<Synapse, LinkSlot> ubLinkSlots = new TreeMap<>(SYN_COMP);
    protected Map<Synapse, LinkSlot> lbLinkSlots = new TreeMap<>(SYN_COMP);


    public Activation(int id, Thought t, N n) {
        this.id = id;
        this.neuron = n;
        this.thought = t;
        setCreated(t.getCurrentTimestamp());

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);

        initNet();

        connect(getNeuron().getBias(), netUB);
        connect(getNeuron().getBias(), netLB);

        isFired = threshold(this, "isFired", 0.0, ABOVE, netUB);

        isFired.addEventListener(() -> {
                    fired = thought.getCurrentTimestamp();
                    Counting.add(this);
                }
        );

        isFiredForWeight = func(
                this,
                "(isFired * 2) - 1",
                isFired,
                x -> (x * 2.0) - 1.0
        );
        isFiredForBias = func(
                this,
                "(isFired * -1) + 1",
                isFired,
                x -> (x * -1.0) + 1.0
        );

        initFields();

        netDiff = sub(
                this,
                "netDiff",
                netUB,
                netLB
        );

        isFinal = threshold(
                this,
                "isFinal",
                0.01,
                BELOW,
                netDiff
        );

        if (!getNeuron().isNetworkInput() && getConfig().isTrainingEnabled())
            isFinal.addEventListener(() ->
                    initGradientFields()
            );

        thought.register(this);
        neuron.register(this);
    }

    protected void initNet() {
        netUB = new ValueSortedQueueField(this, "net UB");
        netLB = new ValueSortedQueueField(this, "net LB");
    }

    public void setNet(double v) {
        netUB.setValue(v);
        netUB.process();
        netLB.setValue(v);
        netLB.process();
    }

    public Map<Synapse, LinkSlot> getLinkSlots(boolean upperBound) {
        return upperBound ? ubLinkSlots : lbLinkSlots;
    }

    public LinkSlot lookupLinkSlot(Synapse syn, boolean upperBound) {
        return getLinkSlots(upperBound).computeIfAbsent(syn, s -> {
            LinkSlot ls = new LinkSlot(
                    s,
                    syn instanceof NegativeFeedbackSynapse ? MIN : MAX,
                    "link slot " + (upperBound ? "ub" : "lb")
            );
            connect(ls, getNet(upperBound));
            return ls;
        });
    }

    protected void initGradientFields() {
        ownInputGradient = new QueueField(this, "Own-Input-Gradient");
        backpropInputGradient = new QueueField(this, "Backprop-Input-Gradient", 0.0);
        ownOutputGradient = new QueueField(this, "Own-Output-Gradient");
        backpropOutputGradient = new QueueField(this, "Backprop-Output-Gradient");

        entropy = func(
                this,
                "Entropy",
                netUB,
                x ->
                        getNeuron().getSurprisal(
                                Sign.getSign(x),
                                getAbsoluteRange(),
                                true
                        ),
                ownInputGradient
        );

        netOuterGradient =
                func(
                        this,
                        "f'(net)",
                        netUB,
                        x -> getNeuron().getActivationFunction().outerGrad(x)
        );

        mul(
                this,
                "ig * f'(net)",
                ownInputGradient,
                netOuterGradient,
                ownOutputGradient
        );

        mul(
                this,
                "ig * f'(net)",
                backpropInputGradient,
                netOuterGradient,
                backpropOutputGradient
        );

        outputGradient = add(
                this,
                "ownOG + backpropOG",
                ownOutputGradient,
                backpropOutputGradient
        );

        updateValue = scale(
                this,
                "learn-rate * og",
                getConfig().getLearnRate(),
                outputGradient
        );
        connect(updateValue, getNeuron().getBias());

        inductionThreshold = threshold(
                this,
                "induction threshold",
                getConfig().getInductionThreshold(),
                ABOVE_ABS,
                outputGradient
        );
    }

    public FieldOutput getIsFired() {
        return isFired;
    }

    public FieldOutput getIsFiredForWeight() {
        return isFiredForWeight;
    }

    public FieldOutput getIsFiredForBias() {
        return isFiredForBias;
    }

    public FieldOutput getNetDiff() {
        return netDiff;
    }

    public FieldOutput getIsFinal() {
        return isFinal;
    }

    protected void initFields() {
        valueUB = func(
                this,
                "value = f(netUB)",
                netUB,
                x -> getActivationFunction().f(x)
        );
        valueLB = func(
                this,
                "value = f(netLB)",
                netLB,
                x -> getActivationFunction().f(x)
        );
    }

    public FieldFunction getNetOuterGradient() {
        return netOuterGradient;
    }

    public void init(Synapse originSynapse, Activation originAct) {
        initFixedTransitionEvents();
        thought.onActivationCreationEvent(this, originSynapse, originAct);
    }

    public SlotField getSlot(State s) {
        return switch(s) {
            case SAME -> sameBSSlot;
            default -> null;
        };
    }

    public void receiveBindingSignal(BindingSignal bs) {
        notifyVariableTransitions(bs);
    }

    private void notifyVariableTransitions(BindingSignal bs) {
        Neuron<?, ?> n = getNeuron();

        for(Direction dir: DIRECTIONS)
            n.getTargetSynapses(dir)
                    .forEach(s ->
                            s.notifyVariableTransitions(
                                    bs,
                                    dir.invert()
                            )
                    );
    }

    private void initFixedTransitionEvents() {
        Neuron<?, ?> n = getNeuron();

        for(Direction dir: DIRECTIONS)
            n.getTargetSynapses(dir)
                    .forEach(s ->
                            s.initFixedTransitions(
                                    this,
                                    dir.invert()
                            )
                    );
    }

    public FieldOutput getEntropy() {
        return entropy;
    }

    public Field getOwnInputGradient() {
        return ownInputGradient;
    }

    public Field getBackpropInputGradient() {
        return backpropInputGradient;
    }

    public FieldOutput getOwnOutputGradient() {
        return ownOutputGradient;
    }

    public FieldOutput getBackpropOutputGradient() {
        return backpropOutputGradient;
    }

    public FieldOutput getOutputGradient() {
        return outputGradient;
    }

    public FieldOutput getUpdateValue() {
        return updateValue;
    }

    public FieldOutput getInductionThreshold() {
        return inductionThreshold;
    }

    public int getId() {
        return id;
    }

    public FieldOutput getValue(boolean upperBound) {
        return upperBound ? valueUB : valueLB;
    }

    public FieldOutput getValueUB() {
        return valueUB;
    }

    public FieldOutput getValueLB() {
        return valueLB;
    }

    public boolean isInput() {
        return false;
    }

    public Field getNet(boolean upperBound) {
        return upperBound ? netUB : netLB;
    }

    public Field getNetUB() {
        return netUB;
    }

    public Field getNetLB() {
        return netLB;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp ts) {
        this.created = ts;
    }

    public Timestamp getFired() {
        return fired;
    }

    public boolean isFired() {
        return fired != NOT_SET;
    }

    public void instantiateTemplate() {
        Activation<N> act = neuron.instantiateTemplate(true)
                .createActivation(thought);

        act.init(null, this);
    }

    public Thought getThought() {
        return thought;
    }

    public abstract Range getRange();

    public Range getAbsoluteRange() {
        Range r = getRange();
        if(r == null) return null;
        return r.getAbsoluteRange(thought.getRange());
    }

    public void initNeuronLabel(BindingSignal bs) {
        getNeuron().setLabel(
                getConfig().getLabel(bs)
        );
    }

    public Stream<BindingSignal> getBindingSignals() {
        return getPatternBindingSignals().values().stream();
    }

    public void propagateBindingSignal(BindingSignal fromBS) {
        getOutputLinks().forEach(l ->
                fromBS.propagate(l)
        );
    }

    public void registerBindingSignal(BindingSignal bs) {
        bindingSignals.put(BSKey.createKey(bs), bs);
        bs.getOnArrived().setValue(1.0);
    }

    public Map<BSKey, BindingSignal> getPatternBindingSignals() {
        return bindingSignals;
    }

    public BindingSignal getBindingSignal(BSKey bsKey) {
        return bindingSignals.get(bsKey);
    }

    public BindingSignal getBindingSignal() {
        return getBindingSignals()
                .findFirst()
                .orElse(null);
    }

    public BindingSignal getBindingSignal(State s) {
        return getBindingSignals(s)
                .findFirst()
                .orElse(null);
    }

    public BindingSignal getBindingSignal(Activation originAct, State s) {
        return getBindingSignals(s)
                .filter(bs ->
                        bs.getOriginActivation() == originAct
                ).findFirst()
                .orElse(null);
    }

    public Stream<BindingSignal> getBindingSignals(State s) {
        return bindingSignals.values()
                .stream()
                .filter(bs -> bs.getState() == s);
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

    public <M extends Model> M getModel() {
        return (M) neuron.getModel();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
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

    public Stream<Link> getOutputLinks(Synapse s) {
        return outputLinks
                .subMap(
                        new OutputKey(s.getOutput().getProvider(), Integer.MIN_VALUE),
                        true,
                        new OutputKey(s.getOutput().getProvider(), MAX_VALUE),
                        true
                ).values()
                .stream()
                .filter(l -> l.getSynapse() == s);
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

    public void disconnect() {
        FieldOutput[] fields = new FieldOutput[] {
                netUB,
                netLB,
                valueUB,
                valueLB,
                isFired,
                isFiredForWeight,
                isFiredForBias,
                isFinal,
                entropy,
                netOuterGradient,
                ownInputGradient,
                backpropInputGradient,
                ownOutputGradient,
                backpropOutputGradient,
                outputGradient
        };

        for(FieldOutput f: fields) {
            if(f == null)
                continue;
            f.disconnect();
        }
    }

    public Stream<Link> getInputLinks() {
        return inputLinks.values().stream();
    }

    public Stream<Link> getOutputLinks() {
        return outputLinks.values().stream();
    }

    public String toString() {
        return getClass().getSimpleName() + " " + toKeyString();
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
