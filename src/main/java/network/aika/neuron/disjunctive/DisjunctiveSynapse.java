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
package network.aika.neuron.disjunctive;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.DisjunctiveActivation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.axons.Axon;
import network.aika.neuron.bindingsignal.Transition;

import java.util.Collections;
import java.util.List;

import static network.aika.fields.Fields.mul;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class DisjunctiveSynapse<S extends DisjunctiveSynapse, I extends Neuron & Axon, O extends DisjunctiveNeuron<?, OA>, L extends Link<S, IA, OA>, IA extends Activation<?>, OA extends DisjunctiveActivation> extends Synapse<S,I,O,L,IA,OA> {

    @Override
    public List<Transition> getTransitions() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean propagatedAllowed(Activation act) {
        if(isTemplate() && act != null && act.isNetworkInput())
            return false;

        return true;
    }

    @Override
    public void setModified() {
        getInput().setModified();
    }

    @Override
    public void linkOutput() {
        if(!isTemplate())
            return;

        super.linkOutput();
    }
}
