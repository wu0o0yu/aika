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
import network.aika.fields.FieldLink;
import network.aika.fields.FieldOutput;
import network.aika.fields.Multiplication;
import network.aika.visitor.Visitor;

import static network.aika.callbacks.EventType.CREATE;
import static network.aika.fields.ConstantField.ONE;
import static network.aika.fields.ConstantField.ZERO;
import static network.aika.fields.Fields.mul;
import static network.aika.fields.Fields.scale;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackLink extends FeedbackLink<PositiveFeedbackSynapse, PatternActivation> {

    public PositiveFeedbackLink(PositiveFeedbackSynapse s, PatternActivation input, BindingActivation output) {
        super(s, input, output);
    }

    public void relinkInput(PatternActivation in) {
        if(input != null) {
            assert input == in;
            return;
        }

        input = in;

        linkInput();

        FieldLink il = inputValue.getInputLinkByArg(0);
        il.relinkInput(getInputValue());

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

        if(input == null)
            return;

        scale(
                this,
                "updateValue = lr * in.grad * f'(out.net)",
                getConfig().getLearnRate(output.getNeuron().isAbstract()),
                mul(
                        this,
                        "in.gradient * f'(out.net)",
                        input.getGradient(),
                        output.getNetOuterGradient()
                ),
                output.getUpdateValue()
        );
    }

    @Override
    public FieldOutput getInputIsFired() {
        if(input == null)
            return ONE;

        return input.getIsFired();
    }

    @Override
    public FieldOutput getNegInputValue() {
        if(input == null)
            return ZERO;

        return input.getNegValue();
    }

    @Override
    public void addInputLinkingStep() {
    }

    @Override
    protected boolean incrementRound() {
        return true;
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
