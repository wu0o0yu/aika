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

import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.synapses.PositiveFeedbackSynapse;
import network.aika.fields.*;
import network.aika.visitor.Visitor;

import static network.aika.callbacks.EventType.CREATE;
import static network.aika.fields.FieldLink.linkAndConnect;
import static network.aika.fields.Fields.mul;
import static network.aika.fields.Fields.scale;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackLink extends FeedbackLink<PositiveFeedbackSynapse, PatternActivation> {

    protected AbstractFunction inputGradient;

    public PositiveFeedbackLink(PositiveFeedbackSynapse s, PatternActivation input, BindingActivation output) {
        super(s, input, output);
    }

    @Override
    protected void initInputValue() {
        inputValue = new FeedbackFunction(this, "input value");
        inputValue.setInitialValue(1.0);
    }

    public void relinkInput(PatternActivation in) {
        if(input != null) {
            assert input == in;
            return;
        }

        input = in;

        linkInput();

        linkAndConnect(input.getValue(), 0, inputValue);
        linkAndConnect(getThought().getAnnealing(), 1, inputValue);
        linkAndConnect(input.getGradient(), 0, inputGradient);

        getThought().onElementEvent(CREATE, this);
    }

    @Override
    public void initFromTemplate(Link template) {
        super.initFromTemplate(template);
        synapse.initDummyLink(output);
    }

    @Override
    protected void connectGradientFields() {
        super.connectGradientFields();

        inputGradient = new IdentityFunction(this, "input gradient");

        scale(
                this,
                "updateValue = lr * in.grad * f'(out.net)",
                getConfig().getLearnRate(output.getNeuron().isAbstract()),
                mul(
                        this,
                        "in.gradient * f'(out.net)",
                        inputGradient,
                        output.getNetOuterGradient()
                ),
                output.getUpdateValue()
        );

        if(input != null)
            linkAndConnect(input.getGradient(), 0, inputGradient);
    }

    @Override
    public void addInputLinkingStep() {
    }

    @Override
    public void bindingVisit(Visitor v) {
        if(v.isDown())
            super.bindingVisit(v);
    }

    @Override
    public void patternVisit(Visitor v) {
    }

    @Override
    public void inhibVisit(Visitor v) {
    }
}
