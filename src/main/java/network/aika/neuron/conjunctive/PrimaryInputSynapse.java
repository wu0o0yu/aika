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

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.PrimaryInputLink;
import network.aika.neuron.axons.PatternAxon;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.neuron.bindingsignal.TransitionMode;

import java.util.List;
import java.util.stream.Stream;


import static network.aika.neuron.bindingsignal.State.INPUT;
import static network.aika.neuron.bindingsignal.State.SAME;
import static network.aika.neuron.bindingsignal.Transition.transition;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_AND_PROPAGATE;


/**
 *
 * @author Lukas Molzberger
 */
public class PrimaryInputSynapse<I extends Neuron & PatternAxon, IA extends Activation<?>> extends BindingNeuronSynapse<PrimaryInputSynapse, I, PrimaryInputLink<IA>, IA> {

    private static List<Transition> TRANSITIONS = List.of(
            transition(SAME, INPUT, MATCH_AND_PROPAGATE)
    );

    //           transition(State.INPUT, State.INPUT)
    //          .setPropagateBSOnly(false)
    //                   .setPropagateBS(false)  // check=false: Rel. Pre. Entity special case

    public PrimaryInputLink createLink(IA input, BindingActivation output) {
        return new PrimaryInputLink(this, input, output);
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }
}
