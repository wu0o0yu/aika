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
import network.aika.elements.synapses.InhibitorySynapse;
import network.aika.fields.Field;
import network.aika.fields.FieldOutput;
import network.aika.fields.Fields;
import network.aika.fields.QueueSumField;

import java.util.stream.Stream;

import static network.aika.elements.activations.Activation.isSelfRef;
import static network.aika.fields.FieldLink.linkAndConnect;
import static network.aika.steps.Phase.NEGATIVE_FEEDBACK;
import static network.aika.utils.Utils.TOLERANCE;

/**
 * @author Lukas Molzberger
 */
public class InhibitoryLink extends AbstractInhibitoryLink<InhibitorySynapse, BindingActivation> {

    protected FieldOutput value;

    protected Field net;

    public InhibitoryLink(InhibitorySynapse inhibitorySynapse, BindingActivation input, InhibitoryActivation output) {
        super(inhibitorySynapse, input, output);
    }

    @Override
    public void connectFields(NegativeFeedbackLink out) {
        if(isSelfRef(getInput(), out.getOutput()))
            return;

        linkAndConnect(getNet(), out.getInputValue());
    }

    @Override
    public Stream<InhibitoryLink> getInhibitoryLinks() {
        return Stream.of(this);
    }

    @Override
    protected void initFields() {
        net = new QueueSumField(this, NEGATIVE_FEEDBACK, "net", null);
        linkAndConnect(weightedInput, net);
        linkAndConnect(output.getNeuron().getBias(), net)
                .setPropagateUpdates(false);

        value = Fields.func(
                this,
                "value = f(net)",
                TOLERANCE,
                net,
                x -> output.getActivationFunction().f(x)
        );

        InhibitoryActivation.connectFields(
                Stream.of(this),
                output.getNegativeFeedbackLinks()
        );
    }

    @Override
    public void disconnect() {
        super.disconnect();
        net.disconnectAndUnlinkInputs(false);
    }

    public FieldOutput getValue() {
        return value;
    }

    public FieldOutput getNet() {
        return net;
    }
}
