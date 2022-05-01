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
import network.aika.fields.FieldOutput;
import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.neuron.disjunctive.InhibitoryNeuron;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.Transition.transition;


/**
 *
 * @author Lukas Molzberger
 */
public class NegativeFeedbackSynapse extends BindingNeuronSynapse<NegativeFeedbackSynapse, InhibitoryNeuron, NegativeFeedbackLink, InhibitoryActivation> {

    private static List<Transition> TRANSITIONS = List.of(
            transition(State.SAME, State.INPUT)
                    .setPropagate(Integer.MAX_VALUE),

            transition(State.INPUT, State.INPUT)
                    .setCheck(true)
                    .setCheckSelfRef(true)
                    .setCheckSamePrimaryInput(true)
                    .setCheckBoundToSamePattern(true)
                    .setPropagate(0)//,

//            transition(State.BRANCH, State.BRANCH)
//                    .setCheck(true)
//                    .setPropagate(1)
    );

    @Override
    public NegativeFeedbackLink createLink(BindingSignal<InhibitoryActivation> input, BindingSignal<BindingActivation> output) {
        return new NegativeFeedbackLink(this, input, output);
    }

    @Override
    public void setWeight(double w) {
        weight.receiveUpdate(0, w);
    }

    @Override
    public boolean linkExists(BindingSignal<InhibitoryActivation> iBS, BindingSignal<BindingActivation> oBS) {
        if(super.linkExists(iBS, oBS))
            return true;

        return false; // TODO: oBS.getActivation().getBranches().stream()
             //   .anyMatch(bAct -> super.linkExists(iBS.getActivation(), bAct));
    }

    @Override
    public FieldOutput getLinkingEvent(BindingSignal bs, Transition t, Direction dir) {
        if(dir == OUTPUT) {
            return isTemplate() ?
                    bs.getOnArrivedFiredFinal() :
                    bs.getOnArrivedFired();
        }

        return super.getLinkingEvent(bs, t, dir);
    }

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }
}
