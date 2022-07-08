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

import network.aika.direction.Direction;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.ReversePatternLink;
import network.aika.neuron.axons.PatternAxon;
import network.aika.neuron.bindingsignal.PrimitiveTransition;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.FixedTerminal.fixed;
import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.State.*;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_AND_PROPAGATE;

/**
 *
 * @author Lukas Molzberger
 */
public class ReversePatternSynapse<I extends Neuron & PatternAxon, IA extends Activation<?>> extends
        BindingNeuronSynapse<
                ReversePatternSynapse,
                I,
                ReversePatternLink<IA>,
                IA
                >
{

    public static PrimitiveTransition SAME_SAME_TRANSITION = transition(
            fixed(SAME),
            fixed(SAME),
            MATCH_AND_PROPAGATE
    );

    public static PrimitiveTransition INPUT_RELATED_SAME_TRANSITION = transition(
            fixed(INPUT),
            fixed(RELATED_SAME),
            MATCH_AND_PROPAGATE
    );

    private static List<Transition> TRANSITIONS = List.of(
            SAME_SAME_TRANSITION,
            INPUT_RELATED_SAME_TRANSITION
    );

    @Override
    public ReversePatternLink createLink(IA input, BindingActivation output) {
        return new ReversePatternLink(this, input, output);
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }

    @Override
    public boolean networkInputsAllowed(Direction dir) {
        return !isTemplate();
    }
}
