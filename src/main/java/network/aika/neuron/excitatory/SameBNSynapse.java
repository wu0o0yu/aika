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
package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.activation.visitor.Visitor;

import static network.aika.neuron.activation.direction.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class SameBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public SameBNSynapse() {
    }

    public SameBNSynapse(boolean isRecurrent) {
        this.isRecurrent = isRecurrent;
    }

    public LinkVisitor transition(ActVisitor v, Synapse s, Link l) {
        Templates t = getModel().getTemplates();
        Synapse ts = s.getTemplate();

        if(v.getStartDir() != v.getCurrentDir()) {
            if(ts != t.PRIMARY_INPUT_SYNAPSE_TEMPLATE) {
                return null;
            }
        } else {
            if(ts == t.NEGATIVE_SYNAPSE_TEMPLATE || ts == t.RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE || ts == t.SAME_PATTERN_SYNAPSE_TEMPLATE) {
                return null;
            }
        }

        LinkVisitor nv = new LinkVisitor(v, s, l);
        nv.incrementPathLength();
        return nv;
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        if(super.checkTemplatePropagate(v, act))
            return true;

        if (v.getCurrentDir() == INPUT)
            return !act.getNeuron().isInputNeuron() && isRecurrent;

        return false;
    }
}
