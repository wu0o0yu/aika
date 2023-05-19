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
package network.aika.elements.activations;

import network.aika.Thought;
import network.aika.elements.links.AbstractInhibitoryLink;
import network.aika.elements.links.NegativeFeedbackLink;
import network.aika.elements.neurons.InhibitoryNeuron;


/**
 *
 * @author Lukas Molzberger
 */
public class InhibitoryActivation extends DisjunctiveActivation<InhibitoryNeuron> {


    public InhibitoryActivation(int id, Thought t, InhibitoryNeuron neuron) {
        super(id, t, neuron);
    }


    @Override
    public boolean isActiveTemplateInstance() {
        return true;
    }

    public void connectOutgoingLinks(AbstractInhibitoryLink il) {
        getOutputLinksByType(NegativeFeedbackLink.class)
                .forEach(ol ->
                        il.connectFields(ol)
                );
    }

    public void connectIncomingLinks(NegativeFeedbackLink ol) {
        getInputLinksByType(AbstractInhibitoryLink.class)
                .forEach(il ->
                        il.connectFields(ol)
                );
    }
}
