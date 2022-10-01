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
import network.aika.direction.Direction;
import network.aika.fields.Fields;
import network.aika.neuron.Neuron;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;

import java.util.List;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.State.ABSTRACT_SAME;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveActivation<N extends Neuron> extends Activation<N> {

    protected ConjunctiveActivation(int id, N n) {
        super(id, n);
    }

    public ConjunctiveActivation(int id, Thought t, N n) {
        super(id, t, n);
    }

    public abstract BindingSignal getAbstractBindingSignal();

    public void instantiateTemplate() {
        assert isTemplate();

        BindingSignal abstractBS = getAbstractBindingSignal();
        if(abstractBS == null)
            return;

        if(abstractBS.getActivation().getNeuron().getTemplate() == getNeuron())
            return;

        N n = (N) neuron.instantiateTemplate(true);
        ConjunctiveActivation<N> act = (ConjunctiveActivation<N>) n.createActivation(thought);

        act.init(null, this);

        getInputLinks()
                .forEach(l ->
                        l.instantiateTemplate(act, INPUT)
                );
/*
        getOutputLinks()
                .forEach(l ->
                        l.instantiateTemplate(act, OUTPUT)
                );
 */
    }
}
