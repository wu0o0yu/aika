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
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.PrimaryInputLink;
import network.aika.neuron.bindingsignal.PrimitiveTransition;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;
import java.util.stream.Stream;


import static network.aika.neuron.bindingsignal.FixedTerminal.fixed;
import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.State.*;
import static network.aika.neuron.bindingsignal.TransitionMode.*;
import static network.aika.neuron.bindingsignal.VariableTerminal.variable;

/**
 *
 * @author Lukas Molzberger
 */
public class PrimaryInputSynapse extends BindingNeuronSynapse<
        PrimaryInputSynapse,
        PatternNeuron,
        PrimaryInputLink,
        PatternActivation
        >
{

    public static PrimitiveTransition SAME_INPUT_TRANSITION = transition(
            fixed(SAME),
            fixed(INPUT),
            MATCH_AND_PROPAGATE,
            PrimaryInputSynapse.class
    );

    public static PrimitiveTransition SAME_RELATED_SAME_TRANSITION = transition(
            fixed(SAME),
            fixed(RELATED_SAME),
            MATCH_AND_PROPAGATE,
            PrimaryInputSynapse.class
    );

    public static PrimitiveTransition INPUT_RELATED_INPUT_TRANSITION = transition(
            variable(INPUT),
            variable(RELATED_INPUT),
            MATCH_AND_PROPAGATE,
            PrimaryInputSynapse.class
    );

    private static List<Transition> TRANSITIONS = List.of(
            SAME_INPUT_TRANSITION,
            SAME_RELATED_SAME_TRANSITION,
            INPUT_RELATED_INPUT_TRANSITION
    );

    @Override
    public PrimaryInputLink createLink(PatternActivation input, BindingActivation output) {
        return new PrimaryInputLink(this, input, output);
    }

    @Override
    public boolean propagateCheck(PatternActivation iAct) {
        return checkCandidateSynapse(iAct);
    }

    @Override
    public PrimitiveTransition getRelatedTransition() {
        return SAME_RELATED_SAME_TRANSITION;
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }
}
