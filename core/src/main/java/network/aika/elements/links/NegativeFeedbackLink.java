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

import static network.aika.fields.FieldLink.linkAndConnect;
import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public class NegativeFeedbackLink extends FeedbackLink<NegativeFeedbackSynapse, InhibitoryActivation> {

    Field weightUpdate;

    public NegativeFeedbackLink(NegativeFeedbackSynapse s, InhibitoryActivation input, BindingActivation output) {
        super(s, input, output);

        if(input == null)
            return;

        input.connectIncomingLinks(this);
    }

    @Override
    protected void initInputValue() {
        inputValue = new MaxField(this, "max-input-value");
    }

    @Override
    protected void connectInputValue() {
    }

    @Override
    protected Multiplication initWeightedInput() {
        return mul(
                this,
                "annealing * iAct(" + getInputKeyString() + ").value * weight",
                getThought().getAnnealing(),
                super.initWeightedInput()
        );
    }

    @Override
    public void bindingVisit(Visitor v) {
        // don't allow negative feedback links to create new links; i.d. do nothing
    }

    @Override
    public void connectWeightUpdate() {
        weightUpdate = mul(
                this,
                "weight update",
                getInputIsFired(),
                getOutput().getNegUpdateValue()
        );

        linkAndConnect(
                weightUpdate,
                synapse.getWeight()
        );
    }

    @Override
    public void disconnect() {
        super.disconnect();

        if(weightUpdate != null)
            weightUpdate.disconnectAndUnlinkOutputs(false);
    }
}
