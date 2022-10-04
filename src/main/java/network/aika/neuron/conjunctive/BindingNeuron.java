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
package network.aika.neuron.conjunctive;

import network.aika.Thought;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.bindingsignal.PrimitiveTerminal;
import network.aika.neuron.bindingsignal.State;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.FixedTerminal.fixed;
import static network.aika.neuron.bindingsignal.State.*;
import static network.aika.neuron.bindingsignal.VariableTerminal.variable;
import static network.aika.neuron.conjunctive.ConjunctiveNeuronType.BINDING;


/**
 * @author Lukas Molzberger
 */
public class BindingNeuron extends ConjunctiveNeuron<ConjunctiveSynapse, BindingActivation> {

    public static PrimitiveTerminal INPUT_IN = fixed(State.INPUT, INPUT, BindingNeuron.class);
    public static PrimitiveTerminal RELATED_SAME_IN = fixed(RELATED_SAME, INPUT, BindingNeuron.class);
    public static PrimitiveTerminal RELATED_INPUT_IN = variable(RELATED_INPUT, INPUT, BindingNeuron.class);

    public static PrimitiveTerminal INPUT_OUT = fixed(State.INPUT, OUTPUT, BindingNeuron.class);
    public static PrimitiveTerminal RELATED_SAME_OUT = fixed(RELATED_SAME, OUTPUT, BindingNeuron.class);
    public static PrimitiveTerminal RELATED_INPUT_OUT = variable(RELATED_INPUT, OUTPUT, BindingNeuron.class);


    public BindingNeuron() {
        super(BINDING);
    }

    @Override
    public BindingActivation createActivation(Thought t) {
        return new BindingActivation(t.createActivationId(), t, this);
    }

    @Override
    public BindingNeuron instantiateTemplate(boolean addProvider) {
        BindingNeuron n = new BindingNeuron();
        if(addProvider)
            n.addProvider(getModel());

        initFromTemplate(n);

        return n;
    }
}
