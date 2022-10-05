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

import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.fields.*;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.conjunctive.ConjunctiveNeuron;
import network.aika.sign.Sign;
import network.aika.steps.link.LinkCounting;

import java.util.stream.Stream;

import static network.aika.fields.ConstantField.ZERO;
import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;
import static network.aika.neuron.activation.Timestamp.FIRED_COMPARATOR;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Link<S extends Synapse, I extends Activation<?>, O extends Activation> implements Element {

    protected S synapse;

    protected final I input;
    protected O output;

    private BiFunction igGradient;
    protected AbstractFunction weightedInputUB;
    protected AbstractFunction weightedInputLB;
    protected AbstractFunction backPropGradient;

    protected ThresholdOperator onTransparent;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        init();

        if(input != null && output != null) {
            initOnTransparent();

            onTransparent.addEventListener(() ->
                   propagateAllBindingSignals()
            );

            initWeightInput();

            if (getConfig().isTrainingEnabled() && getSynapse().isAllowTraining())
                output.isFinal.addEventListener(() ->
                        initGradients()
                );
        }

        getThought().onLinkCreationEvent(this);
    }

    protected void initOnTransparent() {
        onTransparent = threshold(
                this,
                "onTransparent",
                0.0,
                ABOVE,
                mul(
                        this,
                        "isFired * weight",
                        synapse.getWeight(),
                        input.isFired
                )
        );
    }

    private void initGradients() {
        igGradient = func(
                this,
                "Information-Gain",
                input.netUB,
                output.netUB,
                (x1, x2) ->
                        getRelativeSurprisal(
                                Sign.getSign(x1),
                                Sign.getSign(x2),
                                input.getAbsoluteRange()
                        ),
                output.ownInputGradient
        );

        initBackpropGradient();

        initWeightUpdate();
    }

    protected void initBackpropGradient() {
        if(output.ownOutputGradient == null)
            return;

        backPropGradient = mul(
                this,
                "oAct.ownOutputGradient * s.weight",
                output.ownOutputGradient,
                synapse.getWeight(),
                input.backpropInputGradient
        );
    }

    public abstract void initWeightUpdate();

    protected void initWeightInput() {
        weightedInputUB = initWeightedInput(true);
        weightedInputLB = initWeightedInput(false);

        connect(weightedInputUB, getOutput().lookupLinkSlot(synapse, true));
        connect(weightedInputLB, getOutput().lookupLinkSlot(synapse, false));
    }

    protected Multiplication initWeightedInput(boolean upperBound) {
        return mul(
                this,
                "iAct(id:" + getInput().getId() + ").value" + (upperBound ? "UB" : "LB") + " * s.weight",
                input.getValue(upperBound),
                synapse.getWeight()
        );
    }

    public void init() {
        if(getInput() != null)
            linkInput();

        if(getOutput() != null)
            linkOutput();

        if(getConfig().isCountingEnabled())
            LinkCounting.add(this);
    }

    public void propagateAllBindingSignals() {
        input.getBindingSignals()
                .forEach(fromBS ->
                        fromBS.propagate(this)
                );
    }

    public FieldOutput getOnTransparent() {
        return onTransparent;
    }

    public FieldOutput getInformationGainGradient() {
        return igGradient;
    }

    public AbstractFunction getWeightedInput(boolean upperBound) {
        return upperBound ? weightedInputUB : weightedInputLB;
    }

    public AbstractFunction getWeightedInputUB() {
        return weightedInputUB;
    }

    public AbstractFunction getWeightedInputLB() {
        return weightedInputLB;
    }

    public FieldOutput getBackPropGradient() {
        return backPropGradient;
    }

    @Override
    public Timestamp getFired() {
        return isCausal() ? input.getFired() : output.getFired();
    }

    @Override
    public Timestamp getCreated() {
        return isCausal() ? input.getCreated() : output.getCreated();
    }

    public double getRelativeSurprisal(Sign si, Sign so, Range range) {
        double s = synapse.getSurprisal(si, so, range, true);
        s -= input.getNeuron().getSurprisal(si, range, true);
        s -= output.getNeuron().getSurprisal(so, range, true);
        return s;
    }

    public FieldOutput getInputValue(Sign s, boolean upperBound) {
        return s.getValue(input != null ? input.getValue(upperBound) : ZERO);
    }

    public S getSynapse() {
        return synapse;
    }

    public void setSynapse(S synapse) {
        this.synapse = synapse;
    }

    public I getInput() {
        return input;
    }

    public O getOutput() {
        return output;
    }

    public boolean isCausal() {
        return input == null || isCausal(input, output);
    }

    public static boolean isCausal(Activation iAct, Activation oAct) {
        return FIRED_COMPARATOR.compare(iAct.getFired(), oAct.getFired()) < 0;
    }

    public void instantiateTemplate(BindingSignal abstractBS, ConjunctiveNeuron on, Direction dir) {
        I iAct = resolveAbstractInputActivation(abstractBS);

        S instSyn = (S) synapse
                .instantiateTemplate(
                        this,
                        iAct.getNeuron(),
                        on
                );
        instSyn.linkOutput();
        instSyn.registerTerminals(getThought());
/*
        Link l = instSyn.createLink(
                dir.getInput(act, iAct),
                dir.getOutput(act, iAct)
        );

        Cleanup.add(l);l
 */
    }

    private I resolveAbstractInputActivation(BindingSignal abstractBS) {
        return getAbstractInputBS(abstractBS)
                .flatMap(bs -> getConcreteActivation(bs.getOriginActivation()))
                .findAny()
                .orElse(input);
    }

    private Stream<I> getConcreteActivation(PatternActivation origin) {
        if(origin.getNeuron().getTemplate() == input.getNeuron())
            return Stream.of((I) origin);

        return origin.getReverseBindingSignals()
                .filter(bs -> bs.getState() == State.SAME || bs.getState() == State.INPUT)
                .filter(bs -> bs.getActivation().getNeuron().getTemplate() == input.getNeuron())
                .map(bs -> (I) bs.getActivation());
    }

    private Stream<BindingSignal> getAbstractInputBS(BindingSignal abstractBS) {
        return abstractBS.getParents().values().stream()
                .filter(bs -> bs.getActivation() == input);
    }

    public void linkInput() {
        if(input == null)
            return;

        input.outputLinks.put(
                new OutputKey(output.getNeuronProvider(), output.getId()),
                this
        );
    }

    public void linkOutput() {
        output.inputLinks.put(
                input != null ? input.getNeuronProvider() : synapse.getPInput(),
                this
        );
    }

    public void unlinkInput() {
        OutputKey ok = output.getOutputKey();
        input.outputLinks.remove(ok, this);
    }

    public void unlinkOutput() {
        output.inputLinks.remove(input.getNeuronProvider(), this);
    }

    public boolean isNegative() {
        return synapse.isNegative();
    }

    @Override
    public Thought getThought() {
        return output.getThought();
    }

    private String getInputKeyString() {
        return (input != null ? input.toKeyString() : "id:X n:[" + synapse.getInput() + "]");
    }

    private String getOutputKeyString() {
        return (output != null ? output.toKeyString() : "id:X n:[" + synapse.getOutput() + "]");
    }

    public String toString() {
        return getClass().getSimpleName() +
                " in:[" + getInputKeyString() + "] " +
                "--> " +
                "out:[" + getOutputKeyString() + "]";
    }
}
