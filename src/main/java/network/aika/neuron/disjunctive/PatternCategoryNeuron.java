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

import network.aika.Thought;
import network.aika.neuron.activation.CategoryActivation;
import network.aika.neuron.bindingsignal.PrimitiveTerminal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.conjunctive.ConjunctiveNeuronType;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.VariableTerminal.variable;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternCategoryNeuron extends CategoryNeuron<PatternCategorySynapse, CategoryActivation> {

    public static PrimitiveTerminal INPUT_IN = variable(State.INPUT, INPUT, PatternCategoryNeuron.class);
    public static PrimitiveTerminal INPUT_OUT = variable(State.INPUT, OUTPUT, PatternCategoryNeuron.class);


    public PatternCategoryNeuron() {
        super(ConjunctiveNeuronType.PATTERN);
    }


    @Override
    public CategoryActivation createActivation(Thought t) {
        return new CategoryActivation(t.createActivationId(), t, this);
    }

    @Override
    public CategoryNeuron instantiateTemplate(boolean addProvider) {
        CategoryNeuron n = new PatternCategoryNeuron();
        if(addProvider)
            n.addProvider(getModel());

        initFromTemplate(n);
        return n;
    }
}
