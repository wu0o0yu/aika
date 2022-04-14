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

import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;

import static network.aika.neuron.bindingsignal.State.*;
import static network.aika.neuron.bindingsignal.Transition.transition;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternSynapse extends ConjunctiveSynapse<PatternSynapse, BindingNeuron, PatternNeuron, PatternLink, BindingActivation, PatternActivation> {

    private static List<Transition> TRANSITIONS = List.of(
            transition(SAME, SAME, true, Integer.MAX_VALUE),
            transition(INPUT, INPUT, false, Integer.MAX_VALUE),
            transition(BRANCH, BRANCH, false, Integer.MAX_VALUE)
    );

    public PatternSynapse() {
        allowPropagate = true;
    }

    @Override
    public boolean allowPropagate(Activation act) {
        if(isTemplate() && act != null && act.isNetworkInput())
            return false;

        return super.allowPropagate(act);
    }

    @Override
    public PatternLink createLink(BindingSignal<BindingActivation> input, BindingSignal<PatternActivation> output) {
        return new PatternLink(this, input, output);
    }

    @Override
    public List<Transition> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public boolean checkTemplateLinkingPreConditions(BindingSignal<BindingActivation> iBS, BindingSignal<PatternActivation> oBS) {
        if(iBS.getActivation().getNeuron().isNetworkInput())
            return false;

        return super.checkTemplateLinkingPreConditions(iBS, oBS);
    }
}
