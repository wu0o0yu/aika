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

import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.neuron.conjunctive.BindingNeuron;
import org.apache.commons.math3.ode.UnknownParameterException;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_AND_PROPAGATE;

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends DisjunctiveSynapse<
        InhibitorySynapse,
        BindingNeuron,
        InhibitoryNeuron,
        InhibitoryLink,
        BindingActivation,
        InhibitoryActivation
        >
{

    private State type;

    public InhibitorySynapse(State type) {
        this.type = type;
    }

    public State getType() {
        return type;
    }


    private static List<Transition> TRANSITIONS_SAME = List.of(
            transition(
                    BindingNeuron.SAME_OUT,
                    InhibitoryNeuron.SAME_IN,
                    MATCH_AND_PROPAGATE,
                    InhibitorySynapse.class
            )
    );

    private static List<Transition> TRANSITIONS_INPUT = List.of(
            transition(
                    BindingNeuron.INPUT_OUT,
                    InhibitoryNeuron.INPUT_IN,
                    MATCH_AND_PROPAGATE,
                    InhibitorySynapse.class
            )
    );

    @Override
    public InhibitorySynapse instantiateTemplate() {
        InhibitorySynapse s = new InhibitorySynapse(type);
        initNewInstance(s);
        return s;
    }

    @Override
    public InhibitoryLink createLink(BindingActivation input, InhibitoryActivation output) {
        return new InhibitoryLink(this, input, output);
    }

    @Override
    public Stream<Transition> getTransitions() {
        return switch(type) {
            case SAME -> TRANSITIONS_SAME.stream();
            case INPUT -> TRANSITIONS_INPUT.stream();
            default -> throw new UnknownParameterException(type.name());
        };
    }
}
