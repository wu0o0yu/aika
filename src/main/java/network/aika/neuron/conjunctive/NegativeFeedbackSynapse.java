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
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.neuron.disjunctive.InhibitoryNeuron;

import java.util.Collections;
import java.util.List;


/**
 *
 * @author Lukas Molzberger
 */
public class NegativeFeedbackSynapse extends BindingNeuronSynapse<NegativeFeedbackSynapse, InhibitoryNeuron, NegativeFeedbackLink, InhibitoryActivation> {

    private static List<Transition> CHECK_TRANSITIONS = List.of(new Transition(State.INPUT, State.INPUT));


    @Override
    public NegativeFeedbackLink createLink(InhibitoryActivation input, BindingActivation output) {
        return new NegativeFeedbackLink(this, input, output);
    }

    @Override
    public void setWeight(double w) {
        weight.addAndTriggerUpdate(w);
    }

    @Override
    public void updateWeight(NegativeFeedbackLink l, double delta) {
        if(l.getInput().isFired() && l.isSelfRef()) {
            weight.addAndTriggerUpdate(-delta);
        }
    }

    @Override
    public boolean linkExists(InhibitoryActivation iAct, BindingActivation oAct) {
        if(super.linkExists(iAct, oAct))
            return true;

        return oAct.getBranches().stream()
                .anyMatch(bAct -> super.linkExists(iAct, bAct));
    }

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    protected void checkConstraints() {
        assert isNegative();
    }

    @Override
    public boolean checkLinkingPreConditions(InhibitoryActivation iAct, BindingActivation oAct) {
        if(oAct.isSeparateBranch(iAct))
            return false;

        if(!iAct.isFired())
            return false;

        if(isTemplate() && !oAct.isSelfRef(iAct))
            return false;

        // Skip BindingNeuronSynapse.checkLinkingPreConditions
        // --> Do not check Link.isForward(iAct, oAct)
        return checkCommonLinkingPreConditions(iAct, oAct);
    }

    @Override
    public boolean checkTemplateLinkingPreConditions(InhibitoryActivation iAct, BindingActivation oAct) {
        if(iAct.getNeuron().getInputSynapse(oAct.getNeuronProvider()) == null) // TODO: should this be selfRef?
            return false;

        return super.checkTemplateLinkingPreConditions(iAct, oAct);
    }

    @Override
    public List<Transition> getPropagateTransitions() {
        return Collections.emptyList();
    }

    @Override
    public List<Transition> getCheckTransitions() {
        return CHECK_TRANSITIONS;
    }
}
