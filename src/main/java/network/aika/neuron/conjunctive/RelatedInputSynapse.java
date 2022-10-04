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
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.LatentRelationActivation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.RelatedInputLink;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.PrimitiveTransition;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.BiTransition.biTransition;
import static network.aika.neuron.bindingsignal.PrimitiveTransition.transition;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_AND_PROPAGATE;


/**
 *
 * @author Lukas Molzberger
 */
public class RelatedInputSynapse extends BindingNeuronSynapse<
        RelatedInputSynapse,
        LatentRelationNeuron,
        RelatedInputLink,
        BindingActivation
        >
{

    private static PrimitiveTransition INPUT_TRANSITION = transition(
            LatentRelationNeuron.INPUT_OUT,
            BindingNeuron.RELATED_INPUT_IN,
            MATCH_AND_PROPAGATE,
            RelatedInputSynapse.class
    );

    private static PrimitiveTransition SAME_TRANSITION = transition(
            LatentRelationNeuron.SAME_OUT,
            BindingNeuron.RELATED_SAME_IN,
            MATCH_AND_PROPAGATE,
            RelatedInputSynapse.class
    );

    private static List<Transition> TRANSITIONS = List.of(
            biTransition(
                    INPUT_TRANSITION,
                    SAME_TRANSITION,
                    true,
                    true
            )
    );

    @Override
    public RelatedInputLink createLink(BindingActivation input, BindingActivation output) {
        return new RelatedInputLink(this, input, output);
    }

    public void linkAndPropagate(Transition t, Direction dir, BindingSignal... fromBSs) {
        if (dir == Direction.INPUT) {
            latentBackwardsPropagation(fromBSs);
        }
        super.linkAndPropagate(t, dir, fromBSs);
    }

    private void latentBackwardsPropagation(BindingSignal[] fromBSs) {
        BindingSignal relatedInputBS = fromBSs[0];
        BindingSignal relatedSameBS = fromBSs[1];

        LatentRelationActivation latentRelAct = getInput().createOrLookupLatentActivation(
                relatedInputBS.getOriginActivation(),
                INPUT_TRANSITION.getInput().getState(),
                relatedSameBS.getOriginActivation(),
                SAME_TRANSITION.getInput().getState()
        );

        Link l = createLink(latentRelAct, (BindingActivation) relatedSameBS.getActivation());

        INPUT_TRANSITION.propagate(INPUT_TRANSITION.getOutput(), relatedInputBS, l, latentRelAct);
        SAME_TRANSITION.propagate(SAME_TRANSITION.getOutput(), relatedSameBS, l, latentRelAct);
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }
}
