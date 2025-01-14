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
package network.aika.elements.links;

import network.aika.Thought;
import network.aika.elements.Element;
import network.aika.elements.activations.Activation;
import network.aika.elements.Timestamp;
import network.aika.enums.Scope;
import network.aika.fields.*;
import network.aika.elements.synapses.Synapse;
import network.aika.steps.link.LinkingIn;
import network.aika.visitor.binding.BindingVisitor;
import network.aika.visitor.inhibitory.InhibitoryVisitor;
import network.aika.visitor.pattern.PatternCategoryVisitor;
import network.aika.visitor.pattern.PatternVisitor;

import static network.aika.debugger.EventType.CREATE;
import static network.aika.fields.FieldLink.linkAndConnect;
import static network.aika.fields.Fields.*;
import static network.aika.elements.Timestamp.FIRED_COMPARATOR;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Link<S extends Synapse, I extends Activation<?>, O extends Activation> implements Element {

    protected S synapse;

    protected I input;
    protected O output;

    protected Field inputValue;
    protected AbstractFunction inputIsFired;
    protected AbstractFunction negInputIsFired;
    protected Multiplication weightedInput;

    protected MultiInputField gradient;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        link();

        if(output != null) {
            initWeightInput();

            if (getConfig().isTrainingEnabled() && getSynapse().isTrainingAllowed()) {
                connectGradientFields();
                connectWeightUpdate();
            }
        }

        propagateRangeOrTokenPos();
        getThought().onElementEvent(CREATE, this);
    }

    public void addInputLinkingStep() {
        LinkingIn.add(this);
    }


    public void bindingVisit(BindingVisitor v, int depth) {
        v.next(this, depth);
    }

    public void patternVisit(PatternVisitor v, int depth) {
        v.next(this, depth);
    }

    public void inhibVisit(InhibitoryVisitor v, int depth) {
        Scope s = synapse.getScope();
        if(s != null && s != v.getIdentityRef())
            return;

        v.next(this, depth);
    }

    public void patternCatVisit(PatternCategoryVisitor v, int depth) {
        v.next(this, depth);
    }

    protected void connectGradientFields() {
    }

    @Override
    public void disconnect() {
        weightedInput.disconnectAndUnlinkInputs(false);
    }

    public void instantiateTemplate(I iAct, O oAct) {
        if(iAct == null || oAct == null)
            return;

        Link l = oAct.getInputLink(iAct.getNeuron());

        if(l != null)
            return;

        S s = (S) synapse.instantiateTemplate(
                iAct.getNeuron(),
                oAct.getNeuron()
        );

        s.createLinkFromTemplate(iAct, oAct, this);
    }


    public abstract void connectWeightUpdate();

    protected void initWeightInput() {
        initInputValue();

        inputIsFired = threshold(this, "inputIsFired", 0.0, ABOVE, inputValue);
        negInputIsFired = invert(this,"!inputIsFired", inputIsFired);

        weightedInput = initWeightedInput();
        linkAndConnect(weightedInput, getOutput().getNet());

        if(input != null)
            connectInputValue();
    }

    protected void initInputValue() {
        inputValue = new IdentityFunction(this, "input value");
    }

    protected void connectInputValue() {
        linkAndConnect(input.getValue(), 0, inputValue);
    }

    protected Multiplication initWeightedInput() {
        weightedInput = new Multiplication(this, "iAct(" + getInputKeyString() + ").value * s.weight");

        FieldLink.link(inputValue, 0, weightedInput);

        FieldLink.link(synapse.getWeight(), 1, weightedInput)
                .setPropagateUpdates(false);

        weightedInput.connectInputs(true);
        return weightedInput;
    }

    public void init() {
        if(input != null)
            addInputLinkingStep();
    }

    public void initFromTemplate(Link template) {
    }

    public void link() {
        if(getInput() != null)
            linkInput();

        if(getOutput() != null)
            linkOutput();
    }

    public FieldOutput getWeightedInput() {
        return weightedInput;
    }

    public Field getGradient() {
        return gradient;
    }

    @Override
    public Timestamp getFired() {
        return input != null && isCausal() ? input.getFired() : output.getFired();
    }

    @Override
    public Timestamp getCreated() {
        return input != null && isCausal() ? input.getCreated() : output.getCreated();
    }

    public Field getInputValue() {
        return inputValue;
    }

    public FieldOutput getInputIsFired() {
        return inputIsFired;
    }

    public FieldOutput getNegInputIsFired() {
        return negInputIsFired;
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
        if(input != null)
            input.linkOutputLink(this);
    }

    public void linkOutput() {
        output.linkInputLink(this);
    }

    public void propagateRangeOrTokenPos() {
        if(input == null)
            return;

        if(input.getRange() != null || input.getTokenPos() != null)
            output.updateRangeAndTokenPos(
                    input.getRange(),
                    input.getTokenPos()
            );
    }

    public boolean isNegative() {
        return synapse.isNegative();
    }

    @Override
    public Thought getThought() {
        return output.getThought();
    }

    protected String getInputKeyString() {
        return (input != null ? input.toKeyString() : "id:X n:[" + synapse.getInput() + "]");
    }

    protected String getOutputKeyString() {
        return (output != null ? output.toKeyString() : "id:X n:[" + synapse.getOutput() + "]");
    }

    public String toString() {
        return getClass().getSimpleName() +
                " in:[" + getInputKeyString() + "] " +
                "--> " +
                "out:[" + getOutputKeyString() + "]";
    }
}
