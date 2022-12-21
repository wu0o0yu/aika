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
import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.fields.*;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.visitor.DownVisitor;
import network.aika.neuron.visitor.UpVisitor;
import network.aika.neuron.visitor.selfref.SelfRefDownVisitor;
import network.aika.steps.link.LinkingIn;

import static network.aika.callbacks.EventType.CREATE;
import static network.aika.fields.FieldLink.link;
import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;
import static network.aika.neuron.activation.Timestamp.FIRED_COMPARATOR;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Link<S extends Synapse, I extends Activation<?>, O extends Activation> extends FieldObject implements Element {

    protected S synapse;

    protected final I input;
    protected O output;

    protected FieldOutput weightedInputUB;
    protected FieldOutput weightedInputLB;

    protected SumField forwardsGradient;
    protected AbstractFunction backwardsGradient;

    protected ThresholdOperator onTransparent;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        init();

        if(input != null && output != null) {
            initOnTransparent();
            initWeightInputUB();
            initWeightInputLB();

            if (getConfig().isTrainingEnabled() && getSynapse().isAllowTraining())
                output.isFinal.addEventListener(() -> {
                            connectGradientFields();
                            connectWeightUpdate();
                        }
                );
        }

        getThought().onElementEvent(CREATE, this);

        propagateRangeOrTokenPos(
                input.getRange(),
                input.getTokenPos()
        );
    }

    public void addInputLinkingStep() {
        LinkingIn.add(this);
    }

    public void selfRefVisitDown(SelfRefDownVisitor v) {
        v.next(this);
    }

    public void bindingVisitDown(DownVisitor v) {
        v.next(this);
    }

    public void bindingVisitUp(UpVisitor v) {
        v.next(this);
    }

    public void patternVisitDown(DownVisitor v) {
    }

    public void patternVisitUp(UpVisitor v) {
    }

    public void inhibVisitDown(DownVisitor v) {
        v.next(this);
    }

    public void inhibVisitUp(UpVisitor v) {
        v.next(this);
    }


    protected void initOnTransparent() {
        AbstractFunction weightIfIsFired = mul(
                this,
                "isFired * weight",
                synapse.getWeight(),
                input.isFired
        );

        onTransparent = threshold(
                this,
                "onTransparent",
                0.0,
                ABOVE,
                weightIfIsFired
        );
    }

    protected void connectGradientFields() {
    }

    protected void initForwardsGradient() {
        forwardsGradient = new SumField(this, "Forwards-Gradient");
        link(input.forwardsGradient, forwardsGradient);
        link(forwardsGradient, output.forwardsGradient);
    }

    protected S instantiateTemplate(I iAct, O oAct) {
        if(iAct == null || oAct == null)
            return null;

        Link l = oAct.getInputLink(iAct.getNeuron());
        if(l != null)
            return (S) l.getSynapse();

        S s = (S) synapse.instantiateTemplate(
                iAct.getNeuron(),
                oAct.getNeuron()
        );
        synapse.copyState(s);
        s.connect(Direction.INPUT, false, false);
        s.connect(Direction.OUTPUT, false, true);

        return s;
    }

    public void initFromTemplate(Link template) {
        template.copyState(this);
        connect(Direction.INPUT, false, false);
        connect(Direction.OUTPUT, false, true);
    }

    public abstract void connectWeightUpdate();

    protected void initWeightInputUB() {
        weightedInputUB = initWeightedInput(true);
        link(weightedInputUB, getOutput().getNet(true));
    }

    protected void initWeightInputLB() {
        weightedInputLB = initWeightedInput(false);
        link(weightedInputLB, getOutput().getNet(false));
    }

    protected FieldOutput initWeightedInput(boolean upperBound) {
        Multiplication weightedInput = mul(
                this,
                "iAct(id:" + getInput().getId() + ").value" + (upperBound ? "UB" : "LB") + " * s.weight",
                getInputValue(upperBound),
                synapse.getWeight()
        );

        return weightedInput;
    }

    public void init() {
        if(getInput() != null)
            linkInput();

        if(getOutput() != null)
            linkOutput();
    }

    public FieldOutput getOnTransparent() {
        return onTransparent;
    }

    public FieldOutput getWeightedInput(boolean upperBound) {
        return upperBound ? weightedInputUB : weightedInputLB;
    }

    public FieldOutput getWeightedInputUB() {
        return weightedInputUB;
    }

    public FieldOutput getWeightedInputLB() {
        return weightedInputLB;
    }

    public Field getForwardsGradient() {
        return forwardsGradient;
    }
    public FieldOutput getBackwardsGradient() {
        return backwardsGradient;
    }

    @Override
    public Timestamp getFired() {
        return isCausal() ? input.getFired() : output.getFired();
    }

    @Override
    public Timestamp getCreated() {
        return isCausal() ? input.getCreated() : output.getCreated();
    }

    public FieldOutput getInputValue(boolean upperBound) {
        return input.getValue(upperBound);
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

    public void linkInput() {
        if(input == null)
            return;

        Link el = input.outputLinks.put(
                new OutputKey(output.getNeuronProvider(), output.getId()),
                this
        );

        assert el == null;
    }

    public void linkOutput() {
        Link el = (Link) output.inputLinks.put(
                input != null ? input.getNeuronProvider() : synapse.getPInput(),
                this
        );
        assert el == null;
    }

    public void unlinkInput() {
        OutputKey ok = output.getOutputKey();
        input.outputLinks.remove(ok, this);
    }

    public void unlinkOutput() {
        output.inputLinks.remove(input.getNeuronProvider(), this);
    }

    public void propagateRangeOrTokenPos(Range r, Integer tokenPos) {
        if(r != null || tokenPos != null)
            output.updateRangeAndTokenPos(r, tokenPos);
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
