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

import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.BindingCategoryActivation;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.ConjunctiveNeuronType;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_AND_PROPAGATE;

/**
 *
 * @author Lukas Molzberger
 */
public class BindingCategorySynapse extends CategorySynapse<BindingCategorySynapse, BindingNeuron, BindingCategoryNeuron, BindingActivation, BindingCategoryActivation> {


    public static Transition SAME_TRANSITION = transition(
            BindingNeuron.SAME_OUT,
            CategoryNeuron.SAME_IN,
            MATCH_AND_PROPAGATE,
            CategorySynapse.class
    );

    public static Transition INPUT_TRANSITION = transition(
            BindingNeuron.INPUT_OUT,
            BindingCategoryNeuron.INPUT_IN,
            MATCH_AND_PROPAGATE,
            CategorySynapse.class
    );

    public BindingCategorySynapse() {
        super(ConjunctiveNeuronType.BINDING);
    }

    @Override
    public Stream<Transition> getTransitions() {
        return List.of(
                SAME_TRANSITION,
                INPUT_TRANSITION
        ).stream();
    }
}
