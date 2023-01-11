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
import static network.aika.fields.FieldLink.link;
import static network.aika.fields.Fields.*;
import static network.aika.elements.activations.Timestamp.FIRED_COMPARATOR;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Link<S extends Synapse, I extends Activation<?>, O extends Activation> extends FieldObject implements Element {

    protected S synapse;

    protected final I input;
    protected O output;

    protected FieldOutput weightedInput;

    protected SumField forwardsGradient;
    protected AbstractFunction backwardsGradient;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        init();

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

    protected void connectGradientFields() {
    }

    protected void initForwardsGradient() {
        forwardsGradient = new SumField(this, "Forwards-Gradient");
        link(input.getForwardsGradient(), forwardsGradient);
        link(forwardsGradient, output.getForwardsGradient());
    }

    public S instantiateTemplate(I iAct, O oAct) {
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

    protected void initWeightInput() {
        weightedInput = initWeightedInput();
        link(weightedInput, getOutput().getNet());
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
        if(getInput() != null)
            linkInput();

        if(getOutput() != null)
            linkOutput();
    }

    public FieldOutput getWeightedInput() {
        return weightedInput;
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

    public FieldOutput getInputValue() {
        return input.getValue();
    }

    public FieldOutput getNegInputValue() {
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
