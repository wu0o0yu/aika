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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.bindingsignal.BranchBindingSignal;
import network.aika.neuron.bindingsignal.PatternBindingSignal;

import static network.aika.neuron.bindingsignal.Scope.RELATED;
import static network.aika.neuron.bindingsignal.Scope.SAME;

/**
 *
 * @author Lukas Molzberger
 */
public class RelatedInputBNSynapse extends InputBNSynapse<BindingNeuron, BindingActivation> {

    @Override
    public PatternBindingSignal propagatePatternBindingSignal(Link l, PatternBindingSignal iBS) {
        if(iBS.getScope() == SAME || iBS.getScope() == RELATED)
            return null;

        return iBS.next(l.getOutput(), true);
    }

    @Override
    public BranchBindingSignal propagateBranchBindingSignal(Link l, BranchBindingSignal iBS) {
        return null;
    }

    @Override
    public boolean checkRelatedPatternBindingSignal(PatternBindingSignal iBS, PatternBindingSignal oBS, BindingActivation iAct, BindingActivation oAct) {
        if(iBS.getScope() == RELATED || oBS.getScope() == RELATED)
            return false;

        return super.checkRelatedPatternBindingSignal(iBS, oBS, iAct, oAct);
    }
}
