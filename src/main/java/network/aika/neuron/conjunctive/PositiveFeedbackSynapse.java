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
import network.aika.fields.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.mul;
import static network.aika.neuron.bindingsignal.BiTransition.biTransition;
import static network.aika.neuron.bindingsignal.FixedTerminal.fixed;
import static network.aika.neuron.bindingsignal.State.INPUT;
import static network.aika.neuron.bindingsignal.State.SAME;
import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.TransitionMode.*;
import static network.aika.neuron.bindingsignal.VariableTerminal.variable;
import static network.aika.neuron.conjunctive.PatternNeuron.INPUT_OUT;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackSynapse extends BindingNeuronSynapse<
        PositiveFeedbackSynapse,
        PatternNeuron,
        PositiveFeedbackLink,
        PatternActivation
        >
{

    private static List<Transition> TRANSITIONS = List.of(
            biTransition(
                    transition(
                            PatternNeuron.INPUT_OUT,
                            BindingNeuron.INPUT_IN,
                            MATCH_ONLY,
                            PositiveFeedbackSynapse.class
                    ),
                    transition(
                            PatternNeuron.SAME_OUT,
                            BindingNeuron.SAME_IN,
                            MATCH_AND_PROPAGATE,
                            PositiveFeedbackSynapse.class
                    ),
                    true,
                    false
            )
    );

    public PositiveFeedbackLink createLink(PatternActivation input, BindingActivation output) {
        return new PositiveFeedbackLink(this, input, output);
    }

    public void initDummyLink(BindingActivation oAct) {
        Multiplication dummyWeight = mul(
                oAct,
                 "pos-dummy-weight-" + getInput().getId(),
                 oAct.getIsOpen(),
                 getWeight()
         );

        LinkSlot ls = oAct.lookupLinkSlot(this, true);
        connect(dummyWeight, -1, ls);
    }

    @Override
    public boolean propagateCheck(PatternActivation iAct) {
        return false;
    }

    @Override
    public FieldOutput getLinkingEvent(Activation act, Direction dir) {
        return null;
    }

    @Override
    protected boolean checkCausal(PatternActivation iAct, BindingActivation oAct) {
        return true;
    }

    @Override
    public boolean isPropagate() {
        return false;
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }
}
