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
import network.aika.neuron.bindingsignal.Transition;

import java.util.List;

import static network.aika.neuron.bindingsignal.State.*;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternSynapse extends ConjunctiveSynapse<PatternSynapse, BindingNeuron, PatternNeuron, PatternLink, BindingActivation, PatternActivation> {

    private static List<Transition> PROPAGATE_TRANSITIONS = List.of(
            new Transition(SAME, SAME),
            new Transition(INPUT, INPUT),
            new Transition(BRANCH, BRANCH)
    );

    private static List<Transition> CHECK_TRANSITIONS = List.of(
            new Transition(SAME, SAME)
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
    public PatternLink createLink(BindingActivation input, PatternActivation output) {
        return new PatternLink(this, input, output);
    }

    @Override
    public List<Transition> getPropagateTransitions() {
        return PROPAGATE_TRANSITIONS;
    }

    @Override
    public List<Transition> getCheckTransitions() {
        return CHECK_TRANSITIONS;
    }

    @Override
    public boolean checkTemplateLinkingPreConditions(BindingActivation iAct, PatternActivation oAct) {
        if(iAct.getNeuron().isNetworkInput())
            return false;

        return super.checkTemplateLinkingPreConditions(iAct, oAct);
    }
}
