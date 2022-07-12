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
import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.neuron.disjunctive.InhibitoryNeuron;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.BiTransition.biTransition;
import static network.aika.neuron.bindingsignal.FixedTerminal.fixed;
import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.State.INPUT;
import static network.aika.neuron.bindingsignal.State.SAME;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_AND_PROPAGATE;


/**
 *
 * @author Lukas Molzberger
 */
public class NegativeFeedbackSynapse extends BindingNeuronSynapse<
        NegativeFeedbackSynapse,
        InhibitoryNeuron,
        NegativeFeedbackLink,
        InhibitoryActivation
        >
{

    private static List<Transition> TRANSITIONS = List.of(
            biTransition(
                    transition(
                            fixed(INPUT),
                            fixed(INPUT),
                            MATCH_AND_PROPAGATE
                    ),
                    transition(
                            fixed(SAME),
                            fixed(SAME),
                            MATCH_AND_PROPAGATE
                    )
            )
    );

    @Override
    public NegativeFeedbackLink createLink(InhibitoryActivation input, BindingActivation output) {
        return new NegativeFeedbackLink(this, input, output);
    }

    @Override
    public boolean propagateCheck(InhibitoryActivation iAct) {
        return true;
    }

    @Override
    public boolean linkCheck(BindingSignal iBS, BindingSignal oBS) {
        return !isTemplate() || iBS.isSelfRef(oBS);
    }

    @Override
    public void setWeight(double w) {
        weight.receiveUpdate(w);
    }

    @Override
    public boolean isRecurrent() {
        return true;
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
