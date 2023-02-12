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

import network.aika.FieldObject;
import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.elements.Element;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.Timestamp;
import network.aika.fields.*;
import network.aika.elements.synapses.Synapse;
import network.aika.visitor.Visitor;
import network.aika.visitor.selfref.SelfRefDownVisitor;
import network.aika.steps.link.LinkingIn;

import static network.aika.callbacks.EventType.CREATE;
import static network.aika.fields.ConstantField.ONE;
import static network.aika.fields.ConstantField.ZERO;
import static network.aika.fields.FieldLink.linkAndConnect;
import static network.aika.fields.Fields.*;
import static network.aika.elements.activations.Timestamp.FIRED_COMPARATOR;
import static network.aika.utils.Utils.TOLERANCE;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Link<S extends Synapse, I extends Activation<?>, O extends Activation> extends FieldObject implements Element {

    protected S synapse;

    protected final I input;
    protected O output;

    protected FieldOutput weightedInput;

    protected SumField gradient;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        link();

        if(input != null && output != null) {
            initWeightInput();

            if (getConfig().isTrainingEnabled() && getSynapse().isTrainingAllowed()) {
                connectGradientFields();
                connectWeightUpdate();
            }
        }

        getThought().onElementEvent(CREATE, this);

        propagateRangeOrTokenPos();
    }

    public void addInputLinkingStep() {
        LinkingIn.add(this);
    }

    public void selfRefVisit(SelfRefDownVisitor v) {
        v.next(this);
    }

    public void bindingVisit(Visitor v) {
        v.next(this);
    }

    public void patternVisit(Visitor v) {
        v.next(this);
    }

    public void inhibVisit(Visitor v) {
        v.next(this);
    }

    public void patternCatVisit(Visitor v) {
        v.next(this);
    }

    protected void connectGradientFields() {
    }

    protected void initGradient() {
        gradient = new SumField(this, "Gradient", TOLERANCE);
        linkAndConnect(input.getGradient(), gradient);
        linkAndConnect(gradient, output.getGradient());
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
        weightedInput = initWeightedInput();
        linkAndConnect(weightedInput, getOutput().getNet());
    }

    protected FieldOutput initWeightedInput() {
        Multiplication weightedInput = mul(
                this,
                "iAct(id:" + getInput().getId() + ").value * s.weight",
                getInputValue(),
                synapse.getWeight()
        );

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

    public FieldOutput getInputValue() {
        if(input == null)
            return ZERO;

        return input.getValue();
    }

    public FieldOutput getNegInputValue() {
        if(input == null)
            return ONE;

        return input.getNegValue();
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

    public void unlinkInput() {
        input.unlinkOutputLink(this);
    }

    public void unlinkOutput() {
        output.unlinkInputLink(this);
    }

    public void propagateRangeOrTokenPos() {
        if(input == null)
            return;

        if(input.getRange() != null || input.getTokenPos() != null)
            output.updateRangeAndTokenPos(input.getRange(), input.getTokenPos());
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
