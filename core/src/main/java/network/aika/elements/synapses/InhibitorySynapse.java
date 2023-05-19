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

import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.InhibitoryActivation;
import network.aika.elements.links.InhibitoryLink;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.neurons.InhibitoryNeuron;

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends AbstractInhibitorySynapse<InhibitorySynapse, BindingNeuron, InhibitoryLink, BindingActivation> {

    public InhibitorySynapse(Scope type) {
        super(type);
    }

    @Override
    public InhibitoryLink createLink(BindingActivation input, InhibitoryActivation output) {
        return new InhibitoryLink(this, input, output);
    }

    @Override
    public InhibitorySynapse instantiateTemplate(BindingNeuron input, InhibitoryNeuron output) {
        InhibitorySynapse s = new InhibitorySynapse(getType());
        s.initFromTemplate(input, output, this);
        return s;
    }
}
