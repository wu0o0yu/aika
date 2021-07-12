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
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;

/**
 * @author Lukas Molzberger
 */
public class InputBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {


    public InputBNSynapse(boolean recurrent) {
        super(recurrent);
    }

    public InputBNSynapse() {
        super();
    }

    public LinkVisitor transition(ActVisitor v, Synapse s, Link l) {
        Templates t = getModel().getTemplates();

        if (v.getStartDir() != v.getCurrentDir()) {
            if (!s.isOfTemplate(t.RELATED_RECURRENT_INPUT_TEMPLATE) && !s.isOfTemplate(t.RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE) && !s.isOfTemplate(t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE)) {
                return null;
            }
        } else {
            if (!s.isOfTemplate(t.PRIMARY_INPUT_SYNAPSE_TEMPLATE)) {
                return null;
            }
        }

        LinkVisitor nv = new LinkVisitor(v, s, l);
        nv.incrementPathLength();
        return nv;
    }

}
