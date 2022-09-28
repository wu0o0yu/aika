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
import network.aika.neuron.disjunctive.CategoryNeuron;

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
public class PCategoryInputSynapse extends PatternNeuronSynapse<
        PCategoryInputSynapse,
        CategoryNeuron,
        PCategoryInputLink,
        CategoryActivation
        >
{

    private static PrimitiveTransition SAME_TRANSITION = transition(
            fixed(SAME),
            fixed(ABSTRACT_SAME),
            MATCH_AND_PROPAGATE,
            PCategoryInputSynapse.class
    );


    private static List<Transition> TRANSITIONS = List.of(
            SAME_TRANSITION
    );

    @Override
    public PCategoryInputLink createLink(CategoryActivation input, PatternActivation output) {
        return new PCategoryInputLink(this, input, output);
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }
}
