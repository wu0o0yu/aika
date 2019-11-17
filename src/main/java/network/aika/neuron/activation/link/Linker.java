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
package network.aika.neuron.activation.link;

import network.aika.Document;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.Synapse;
import network.aika.neuron.meta.MetaSynapse;

import java.util.*;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.link.Direction.INPUT;

/**
 * The {@code Linker} class is responsible for for the linkage of neuron activations. These links mirror the synapses between
 * the neurons of these activations.
 *
 * @author Lukas Molzberger
 */
public class Linker {

    private Document doc;


    public Linker(Document doc) {
        this.doc = doc;
    }


    public Activation computeInputActivation(Synapse s, Activation iAct) {
        if(iAct.getType() == INHIBITORY && s instanceof MetaSynapse) {
            MetaSynapse ms = (MetaSynapse) s;
            if(ms.isMetaVariable) {
                Activation act = iAct.getInputLinks()
                        .map(l -> l.getInput())
                        .findAny()
                        .orElse(null);

                return act != null ? computeInputActivation(s, act) : null;
            }
        }
        return iAct;
    }


    public void link(Synapse s, Activation iAct, Activation oAct) {
        iAct = computeInputActivation(s, iAct);

        Link nl = new Link(s, iAct, oAct);
        if(oAct.getInputLink(nl) != null) {
            return;
        }

        nl.link();
    }

}
