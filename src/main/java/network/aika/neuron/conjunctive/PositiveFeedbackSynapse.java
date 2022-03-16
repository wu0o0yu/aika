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

import network.aika.fields.Field;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.PositiveFeedbackLink;
import network.aika.neuron.axons.PatternAxon;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackSynapse<I extends Neuron & PatternAxon, IA extends Activation> extends BindingNeuronSynapse<PositiveFeedbackSynapse, I, PositiveFeedbackLink<IA>, IA> {

    private static List<Transition> TRANSITIONS = List.of(
            new Transition(State.BRANCH, State.BRANCH),
            new Transition(State.SAME, State.SAME)
    );


    private Field feedbackWeight = new Field("feedbackWeight");
    private Field feedbackBias = new Field("feedbackBias");

    public PositiveFeedbackLink createLink(IA input, BindingActivation output) {
        return new PositiveFeedbackLink(this, input, output);
    }

    protected void initFromTemplate(PositiveFeedbackSynapse s) {
        s.feedbackWeight.setAndTriggerUpdate(feedbackWeight.getCurrentValue());
        s.feedbackBias.setAndTriggerUpdate(feedbackBias.getCurrentValue());
        super.initFromTemplate(s);
    }

    public Field getFeedbackWeight() {
        return feedbackWeight;
    }

    public Field getFeedbackBias() {
        return feedbackBias;
    }

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    public List<Transition> getPropagateTransitions() {
        return TRANSITIONS;
    }

    @Override
    public List<Transition> getCheckTransitions() {
        return TRANSITIONS;
    }

    @Override
    public boolean checkLinkingPreConditions(IA iAct, BindingActivation oAct) {
        // Skip BindingNeuronSynapse.checkLinkingPreConditions
        // --> Do not check Link.isForward(iAct, oAct) and
        // --> iAct.isFired() since the positive feedback synapse is initially assumed to be active.
        return checkCommonLinkingPreConditions(iAct, oAct);
    }
}
