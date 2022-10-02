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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.CategoryInputSynapse;
import network.aika.neuron.conjunctive.ConjunctiveNeuron;

import static network.aika.direction.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveActivation<N extends ConjunctiveNeuron<?, ?>> extends Activation<N> {

    public ConjunctiveActivation(int id, Thought t, N n) {
        super(id, t, n);

        if (!getNeuron().isNetworkInput() &&
                getConfig().isTrainingEnabled() &&
                n.getCategoryInputSynapse() != null)
            isFinal.addEventListener(() ->
                    instantiateTemplate()
            );
    }

    public abstract BindingSignal getAbstractBindingSignal();

    public void instantiateTemplate() {
        BindingSignal abstractBS = getAbstractBindingSignal();
        if(abstractBS == null)
            return;

        if(abstractBS.getActivation().getNeuron().getTemplate() == getNeuron())
            return;

        N n = (N) neuron.instantiateTemplate(true);
        ConjunctiveActivation<N> act = n.createActivation(thought);

        act.init(null, this);

        getInputLinks()
                .filter(l -> !(l.getSynapse() instanceof CategoryInputSynapse))
                .forEach(l ->
                        l.instantiateTemplate(abstractBS, act, INPUT)
                );
/*
        getOutputLinks()
                .forEach(l ->
                        l.instantiateTemplate(act, OUTPUT)
                );
 */

        CategoryInputSynapse cis = neuron.getCategoryInputSynapse();
        CategoryActivation cAct = cis.getInput().createActivation(thought);
    }
}
