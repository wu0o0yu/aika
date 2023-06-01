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
import network.aika.elements.links.ConjunctiveLink;
import network.aika.elements.neurons.ConjunctiveNeuron;
import network.aika.elements.synapses.ConjunctiveSynapse;

import static network.aika.fields.FieldLink.linkAndConnect;
import static network.aika.fields.Fields.scale;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveActivation<N extends ConjunctiveNeuron<?>> extends Activation<N> {

    public ConjunctiveActivation(int id, Thought t, N n) {
        super(id, t, n);
    }

    @Override
    protected void instantiateBias(Activation<N> ti) {
        double optionalSynBiasSum = getInputLinksByType(ConjunctiveLink.class)
                .map(l -> (ConjunctiveSynapse) l.getSynapse())
                .filter(ConjunctiveSynapse::isOptional)
                .mapToDouble(s -> s.getSynapseBias().getUpdatedValue())
                .sum();
        ti.getNeuron().getSynapseBiasSum().receiveUpdate(optionalSynBiasSum, false);
    }

    @Override
    protected void connectWeightUpdate() {
        negUpdateValue = scale(
                this,
                "-updateValue",
                -1.0,
                updateValue
        );

        linkAndConnect(
                updateValue,
                getNeuron().getBias()
        );
    }

    @Override
    protected void initNet() {
        super.initNet();

        linkAndConnect(getNeuron().getSynapseBiasSum(), net)
                .setPropagateUpdates(false);
    }
}
