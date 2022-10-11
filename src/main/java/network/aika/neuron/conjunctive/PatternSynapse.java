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

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.TransitionMode.*;
import static network.aika.neuron.conjunctive.ConjunctiveNeuronType.PATTERN;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternSynapse extends ConjunctiveSynapse<
        PatternSynapse,
        BindingNeuron,
        PatternNeuron,
        PatternLink,
        BindingActivation,
        PatternActivation
        >
{


    public static PrimitiveTransition SAME_SAME_TRANSITION = transition(
            BindingNeuron.SAME_OUT,
            PatternNeuron.SAME_IN,
            MATCH_ONLY,
            PatternSynapse.class
    );

    public static PrimitiveTransition INPUT_INPUT_TRANSITION = transition(
            BindingNeuron.INPUT_OUT,
            PatternNeuron.INPUT_IN,
            MATCH_AND_PROPAGATE,
            PatternSynapse.class
    );

    private static List<Transition> TRANSITIONS = List.of(
            SAME_SAME_TRANSITION,
            INPUT_INPUT_TRANSITION
    );

    public PatternSynapse() {
        super(PATTERN);
    }

    @Override
    public boolean isPropagate() {
        return true;
    }

    @Override
    public boolean propagateCheck(BindingActivation iAct) {
        return true;
    }

    @Override
    public PatternLink createLink(BindingActivation input, PatternActivation output) {
        return new PatternLink(this, input, output);
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }
}
