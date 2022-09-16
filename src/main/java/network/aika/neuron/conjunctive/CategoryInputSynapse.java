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

import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.BindingCategoryActivation;
import network.aika.neuron.activation.CategoryInputLink;
import network.aika.neuron.bindingsignal.PrimitiveTransition;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.neuron.disjunctive.BindingCategoryNeuron;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.FixedTerminal.fixed;
import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.State.*;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_AND_PROPAGATE;
import static network.aika.neuron.bindingsignal.VariableTerminal.variable;

/**
 * The Same Pattern Binding Neuron Synapse is an inner synapse between two binding neurons of the same pattern.
 *
 * @author Lukas Molzberger
 */
public class CategoryInputSynapse extends BindingNeuronSynapse<
        CategoryInputSynapse,
        BindingCategoryNeuron,
        CategoryInputLink,
        BindingCategoryActivation
        >
{
    private static PrimitiveTransition INPUT_TRANSITION = transition(
            fixed(INPUT),
            fixed(INPUT),
            MATCH_AND_PROPAGATE,
            CategoryInputSynapse.class
    );

    private static PrimitiveTransition SAME_TRANSITION = transition(
            fixed(SAME),
            fixed(SAME),
            MATCH_AND_PROPAGATE,
            CategoryInputSynapse.class
    );

    private static PrimitiveTransition RELATED_INPUT_TRANSITION = transition(
            variable(RELATED_INPUT),
            variable(RELATED_INPUT),
            MATCH_AND_PROPAGATE,
            CategoryInputSynapse.class
    );

    private static PrimitiveTransition RELATED_SAME_TRANSITION = transition(
            fixed(RELATED_SAME),
            fixed(RELATED_SAME),
            MATCH_AND_PROPAGATE,
            CategoryInputSynapse.class
    );

    private static List<Transition> TRANSITIONS = List.of(
            INPUT_TRANSITION,
            SAME_TRANSITION,
            RELATED_INPUT_TRANSITION,
            RELATED_SAME_TRANSITION
    );

    @Override
    public CategoryInputLink createLink(BindingCategoryActivation input, BindingActivation output) {
        return new CategoryInputLink(this, input, output);
    }

    @Override
    public PrimitiveTransition getRelatedTransition() {
        return RELATED_SAME_TRANSITION;
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }
}
