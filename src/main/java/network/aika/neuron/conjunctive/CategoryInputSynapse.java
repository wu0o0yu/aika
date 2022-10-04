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

import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.PrimitiveTransition;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.neuron.disjunctive.BindingCategoryNeuron;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.neuron.disjunctive.PatternCategoryNeuron;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_AND_PROPAGATE;

/**
 * The Same Pattern Binding Neuron Synapse is an inner synapse between two binding neurons of the same pattern.
 *
 * @author Lukas Molzberger
 */
public class CategoryInputSynapse extends ConjunctiveSynapse<
        CategoryInputSynapse,
        CategoryNeuron,
        ConjunctiveNeuron<CategoryInputSynapse, ConjunctiveActivation>,
        CategoryInputLink,
        CategoryActivation<?>,
        ConjunctiveActivation
        >
{

    private static PrimitiveTransition SAME_TRANSITION = transition(
            CategoryNeuron.SAME_OUT,
            ConjunctiveNeuron.ABSTRACT_SAME_IN,
            MATCH_AND_PROPAGATE,
            CategoryInputSynapse.class
    );

    private static PrimitiveTransition PATTERN_INPUT_TRANSITION = transition(
            PatternCategoryNeuron.INPUT_OUT,
            ConjunctiveNeuron.ABSTRACT_INPUT_IN,
            MATCH_AND_PROPAGATE,
            CategoryInputSynapse.class
    );

    private static PrimitiveTransition BINDING_INPUT_TRANSITION = transition(
            BindingCategoryNeuron.INPUT_OUT,
            ConjunctiveNeuron.ABSTRACT_INPUT_IN,
            MATCH_AND_PROPAGATE,
            CategoryInputSynapse.class
    );

    private Transition getInputTransition() {
        return type == ConjunctiveNeuronType.PATTERN ?
                PATTERN_INPUT_TRANSITION :
                BINDING_INPUT_TRANSITION;
    }

    public CategoryInputSynapse(ConjunctiveNeuronType type) {
        super(type);
    }

    @Override
    public CategoryInputLink createLink(CategoryActivation input, ConjunctiveActivation output) {
        return new CategoryInputLink(this, input, output);
    }

    @Override
    protected boolean checkCausal(CategoryActivation iAct, ConjunctiveActivation oAct) {
        return true; // Workaround
    }

    @Override
    public Stream<Transition> getTransitions() {
        return List.of(
                SAME_TRANSITION,
                getInputTransition()
        ).stream();
    }
}
