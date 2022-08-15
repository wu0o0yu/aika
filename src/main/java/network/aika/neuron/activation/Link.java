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
import network.aika.fields.*;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.sign.Sign;
import network.aika.steps.link.Cleanup;
import network.aika.steps.link.LinkCounting;
import static network.aika.fields.ConstantField.ZERO;
import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;
import static network.aika.neuron.activation.Timestamp.NOT_SET;
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
    protected AbstractBiFunction weightedInputUB;
    protected AbstractBiFunction weightedInputLB;
    protected AbstractBiFunction backPropGradient;

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
                initGradients();
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
        if(isTemplate())
            induce();

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
        weightedInputUB = initWeightedInput(
                true,
                getOutput().lookupLinkSlot(synapse, true)
        );
        weightedInputLB = initWeightedInput(
                false,
                getOutput().lookupLinkSlot(synapse, false)
        );
    }

    protected Multiplication initWeightedInput(boolean upperBound, LinkSlot ls) {
        return mul(
                this,
                "iAct(id:" + getInput().getId() + ").value" + (upperBound ? "UB" : "LB") + " * s.weight",
                input.getValue(upperBound),
                synapse.getWeight(),
                ls
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

    public AbstractBiFunction getWeightedInput(boolean upperBound) {
        return upperBound ? weightedInputUB : weightedInputLB;
    }

    public AbstractBiFunction getWeightedInputUB() {
        return weightedInputUB;
    }

    public AbstractBiFunction getWeightedInputLB() {
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

    public static boolean templateLinkExists(Synapse ts, Activation iAct, Activation oAct) {
        Link l = oAct.getInputLink(iAct.getNeuron());
        if(l == null)
            return false;
        return l.getSynapse().isOfTemplate(ts);
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

    public boolean isTemplate() {
        return getSynapse().isTemplate();
    }

    public boolean isRecurrent() {
        return synapse.isRecurrent();
    }

    public boolean isCausal() {
        return input == null || isCausal(input, output);
    }

    public static boolean isCausal(Activation iAct, Activation oAct) {
        return FIRED_COMPARATOR.compare(iAct.getFired(), oAct.getFired()) < 0;
    }

    public void induce() {
        assert isTemplate();

        synapse = (S) synapse
                .instantiateTemplate(
                        this,
                        input.getNeuron(),
                        output.getNeuron()
                );

        synapse.linkOutput();

        if(weightedInputLB != null)
            reconnect(weightedInputLB.getInput2(), synapse.getWeight());

        if(backPropGradient != null)
            reconnect(backPropGradient.getInput2(), synapse.getWeight());

        Cleanup.add(this);
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
        output.inputLinks.put(input != null ? input.getNeuronProvider() : synapse.getPInput(), this);
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
        return (isTemplate() ? "Template-" : "") + getClass().getSimpleName() +
                " in:[" + getInputKeyString() + "] " +
                "--> " +
                "out:[" + getOutputKeyString() + "]";
    }
}
