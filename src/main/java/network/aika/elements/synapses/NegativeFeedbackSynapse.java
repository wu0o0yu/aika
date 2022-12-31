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
package network.aika.elements.synapses;

import network.aika.elements.activations.Activation;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.InhibitoryActivation;
import network.aika.elements.links.NegativeFeedbackLink;
import network.aika.elements.neurons.InhibitoryNeuron;
import network.aika.visitor.linking.LinkingOperator;
import network.aika.visitor.linking.binding.BindingDownVisitor;

/**
 *
 * @author Lukas Molzberger
 */
public class NegativeFeedbackSynapse extends FeedbackSynapse<
        NegativeFeedbackSynapse,
        InhibitoryNeuron,
        NegativeFeedbackLink,
        InhibitoryActivation
        >
{
    public NegativeFeedbackSynapse() {
        super(Scope.INPUT);
    }

    @Override
    public void startVisitor(LinkingOperator c, Activation bs) {
        new BindingDownVisitor(bs.getThought(), c)
                .start(bs);
    }

    @Override
    public NegativeFeedbackLink createLink(InhibitoryActivation input, BindingActivation output) {
        return new NegativeFeedbackLink(this, input, output);
    }

    @Override
    public void linkAndPropagateOut(InhibitoryActivation bs) {
        getOutput()
                .linkOutgoing(this, bs);
    }

    @Override
    public void setWeight(double w) {
        weight.receiveUpdate(w);
    }

    @Override
    public boolean checkLinkingEvent(Activation act) {
        return true;
    }

    @Override
    public double getPropagatePreNetUB(InhibitoryActivation iAct) {
        return weight.getCurrentValue();
    }
}
