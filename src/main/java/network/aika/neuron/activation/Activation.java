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

import network.aika.FieldObject;
import network.aika.Model;
import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.fields.*;
import network.aika.neuron.*;
import network.aika.neuron.visitor.DownVisitor;
import network.aika.neuron.visitor.selfref.SelfRefDownVisitor;
import network.aika.neuron.visitor.UpVisitor;
import network.aika.steps.activation.Counting;
import network.aika.steps.activation.LinkingOut;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.callbacks.EventType.CREATE;
import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.*;
import static network.aika.neuron.activation.Timestamp.NOT_SET;

/**
 * @author Lukas Molzberger
 */
public abstract class Activation<N extends Neuron> extends FieldObject implements Element, Comparable<Activation> {

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(Activation::getId);

    protected final int id;
    protected N neuron;
    protected Thought thought;

    protected Timestamp created = NOT_SET;
    protected Timestamp fired = NOT_SET;

    protected FieldOutput valueUB;
    protected FieldOutput valueLB;

    protected SumField netUB;
    protected SumField netLB;

    private FieldOutput netDiff;

    protected FieldOutput isFired;
    protected FieldOutput isFiredForWeight;
    protected FieldOutput isFiredForBias;

    protected FieldOutput isFinal;

    protected FieldOutput isFinalAndFired;

    protected FieldFunction netOuterGradient;
    protected SumField forwardsGradient;
    protected SumField backwardsGradientIn;
    protected SumField backwardsGradientOut;
    protected FieldOutput updateValue;

    protected Map<NeuronProvider, Link> inputLinks;
    protected NavigableMap<OutputKey, Link> outputLinks;

    public boolean instantiationIsQueued;

    protected Range range;
    protected Integer tokenPos;

    protected Consumer<Integer> onTokenPosUpdate;

    public Activation(int id, Thought t, N n) {
        this.id = id;
        this.neuron = n;
        this.thought = t;
        setCreated(t.getCurrentTimestamp());

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);

        initNet();

        FieldLink.link(getNeuron().getBias(), netUB);
        FieldLink.link(getNeuron().getBias(), netLB);

        isFired = threshold(this, "isFired", 0.0, ABOVE, netUB);

        isFired.addEventListener(() -> {
                    fired = thought.getCurrentTimestamp();
                    LinkingOut.add(this);
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
                true,
                netDiff
        );

        isFinalAndFired = mul(
                this,
                "final and fired",
                isFinal,
                isFired
        );

        forwardsGradient = new QueueSumField(this, "Forwards-Gradient");
        backwardsGradientIn = (SumField) new QueueSumField(this, "Backwards-Gradient-In")
                .setInitialValue(0.0);

        backwardsGradientOut = new QueueSumField(this, "Backwards-Gradient-Out");

        if (getConfig().isTrainingEnabled())
            isFinal.addEventListener(() -> {
                        connectGradientFields();
                        connectWeightUpdate();
                    }
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
        netLB.setValue(v);
    }

    public boolean isSelfRef(BindingActivation oAct) {
        SelfRefDownVisitor v = new SelfRefDownVisitor(oAct);
        v.start(this);
        return v.isSelfRef();
    }

    public Activation<N> resolveAbstractInputActivation() {
        return this;
    }

    public void bindingVisitDown(DownVisitor v, Link lastLink) {
        v.next(this);
    }

    public void bindingVisitUp(UpVisitor v, Link lastLink) {
        v.check(lastLink, this);
        v.next(this);
    }

    public void patternVisitDown(DownVisitor v, Link lastLink) {
        v.next(this);
    }

    public void patternVisitUp(UpVisitor v, Link lastLink) {
        v.check(lastLink, this);
        v.next(this);
    }

    public void inhibVisitDown(DownVisitor v, Link lastLink) {
        v.next(this);
    }

    public void inhibVisitUp(UpVisitor v, Link lastLink) {
        v.check(lastLink, this);
        v.next(this);
    }

    public void selfRefVisitDown(DownVisitor v, Link lastLink) {
        v.next(this);
    }


    protected void connectGradientFields() {
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
                backwardsGradientIn,
                netOuterGradient,
                backwardsGradientOut
        );
    }

    protected void connectWeightUpdate() {
        updateValue = scale(
                this,
                "learn-rate * og",
                getConfig().getLearnRate(),
                getOutputGradient()
        );

        FieldLink.link(
                updateValue,
                getNeuron().getBias()
        );
    }

    public abstract FieldOutput getOutputGradient();

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

    public FieldOutput getIsFinalAndFired() {
        return isFinalAndFired;
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

    public SumField getForwardsGradient() {
        return forwardsGradient;
    }

    public FieldOutput getBackwardsGradientIn() {
        return backwardsGradientIn;
    }

    public FieldOutput getBackwardsGradientOut() {
        return backwardsGradientOut;
    }

    public FieldOutput getUpdateValue() {
        return updateValue;
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

    public SumField getNet(boolean upperBound) {
        return upperBound ? netUB : netLB;
    }

    public SumField getNetUB() {
        return netUB;
    }

    public SumField getNetLB() {
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

    public Activation getTemplateInstance() {
        return null;
    }

    public void instantiateTemplate() {
    }

    public Thought getThought() {
        return thought;
    }

    public Range getRange() {
        return range;
    }

    public Integer getTokenPos() {
        return tokenPos;
    }

    public void updateRangeAndTokenPos(Range r, Integer tp) {
        Range newRange = Range.join(range, r);
        Integer newTokenPos = tokenPos != null ? Integer.min(tokenPos, tp) : tp;

        if(!r.equals(range) || tokenPos.intValue() != newTokenPos.intValue()) {
            propagateRangeAndTokenPosition();
        }

        this.range = newRange;
        this.tokenPos = newTokenPos;
        if(onTokenPosUpdate != null)
            onTokenPosUpdate.accept(newTokenPos);
    }

    protected void propagateRangeAndTokenPosition() {
        outputLinks.values().forEach(l ->
                l.propagateRangeOrTokenPos(range, tokenPos)
        );
    }

    public void setOnTokenPosUpdate(Consumer<Integer> onTokenPosUpdate) {
        this.onTokenPosUpdate = onTokenPosUpdate;
    }

    public Range getAbsoluteRange() {
        Range r = getRange();
        if(r == null) return null;
        return r.getAbsoluteRange(thought.getRange());
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

    public void setTemplate(Activation template) {
    }

    public void initFromTemplate(Activation template) {
        setTemplate(template);
        template.copyState(this);
        connect(Direction.INPUT, false, false);
        template.getThought().onElementEvent(CREATE, this);
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

    @Override
    public void disconnect() {
        super.disconnect();

        getInputLinks().forEach(l ->
                l.disconnect()
        );
    }

    public Stream<Link> getInputLinks() {
        return new ArrayList<>(inputLinks.values())
                .stream();
    }

    public Stream<Link> getOutputLinks() {
        return new ArrayList<>(outputLinks.values())
                .stream();
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
