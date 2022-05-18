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
import network.aika.fields.Field;
import network.aika.fields.FieldOutput;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.PositiveFeedbackLink;
import network.aika.neuron.axons.PatternAxon;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_ONLY;
import static network.aika.neuron.bindingsignal.TransitionMode.PROPAGATE_ONLY;
import static network.aika.neuron.bindingsignal.State.BRANCH;
import static network.aika.neuron.bindingsignal.State.SAME;
import static network.aika.neuron.bindingsignal.Transition.transition;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackSynapse<I extends Neuron & PatternAxon, IA extends Activation<?>> extends BindingNeuronSynapse<PositiveFeedbackSynapse, I, PositiveFeedbackLink<IA>, IA> {

    private static List<Transition> TRANSITIONS = List.of(
            transition(BRANCH, BRANCH, MATCH_ONLY),
            transition(SAME, SAME, PROPAGATE_ONLY)
    );

    private Field feedbackBias = new Field(this, "feedbackBias");

    public PositiveFeedbackLink createLink(IA input, BindingActivation output) {
        return new PositiveFeedbackLink(this, input, output);
    }

    protected void initFromTemplate(PositiveFeedbackSynapse s) {
        s.feedbackBias.set(feedbackBias.getCurrentValue());
        super.initFromTemplate(s);
    }

    @Override
    public FieldOutput getLinkingEvent(BindingSignal bs, Direction dir) {
        return bs.getEvent(false, isTemplate());
    }

    public Field getFeedbackBias() {
        return feedbackBias;
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
