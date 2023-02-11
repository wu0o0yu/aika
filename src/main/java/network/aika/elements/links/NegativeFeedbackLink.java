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
import network.aika.elements.activations.InhibitoryActivation;
import network.aika.fields.*;
import network.aika.elements.synapses.NegativeFeedbackSynapse;
import network.aika.visitor.Visitor;

import static network.aika.fields.FieldLink.link;

/**
 * @author Lukas Molzberger
 */
public class NegativeFeedbackLink extends FeedbackLink<NegativeFeedbackSynapse, InhibitoryActivation> {

    MinMaxField maxInput;

    public NegativeFeedbackLink(NegativeFeedbackSynapse s, InhibitoryActivation input, BindingActivation output) {
        super(s, input, output);

        input.getInputLinks().forEach(l ->
                input.connectFields((InhibitoryLink) l, this)
        );
    }

    @Override
    protected void initWeightInput() {
        maxInput = new MinMaxField(this, MinMax.MAX, "max-input-value");

        super.initWeightInput();
    }

    @Override
    protected FieldOutput initWeightedInput() {
        return Fields.mul(
                this,
                "annealing * iAct(id:" + getInput().getId() + ").value * weight",
                getThought().getAnnealing(),
                super.initWeightedInput()
        );
    }

    @Override
    public Field getInputValue() {
        return maxInput;
    }

    @Override
    public void bindingVisit(Visitor v) {
        // don't allow negative feedback links to create new links; i.d. do nothing
    }

    @Override
    public void connectWeightUpdate() {
        FieldLink.link(
                Fields.mul(
                        this,
                        "weight update",
                        getInput().getIsFired(),
                        getOutput().getNegUpdateValue()
                ),
                synapse.getWeight()
        );
    }

    public MinMaxField getMaxInput() {
        return maxInput;
    }
}
