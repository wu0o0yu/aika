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
import network.aika.neuron.bindingsignal.PatternBindingSignal;
import network.aika.neuron.bindingsignal.PrimaryPatternBindingSignal;
import network.aika.neuron.bindingsignal.SecondaryPatternBindingSignal;

import static network.aika.neuron.bindingsignal.BranchBindingSignal.isSeparateBranch;

/**
 * The Same Pattern Binding Neuron Synapse is an inner synapse between two binding neurons of the same pattern.
 *
 * @author Lukas Molzberger
 */
public class SamePatternBNSynapse extends BindingNeuronSynapse<BindingNeuron, BindingActivation> {

    public PatternBindingSignal propagatePatternBindingSignal(Link l, PatternBindingSignal iBS) {
        if(iBS.getScope() > 0)
            return null;

        return iBS.next(l.getOutput(), false);
    }

    public boolean checkRelatedPatternBindingSignal(PatternBindingSignal iBS, PatternBindingSignal oBS) {
        return iBS instanceof PrimaryPatternBindingSignal &&
                oBS instanceof SecondaryPatternBindingSignal && oBS.getScope() == 2;
    }

    @Override
    public boolean checkCausalityAndBranchConsistency(Activation<?> iAct, Activation<?> oAct) {
        if(isSeparateBranch(iAct, oAct))
            return false;

        return super.checkCausalityAndBranchConsistency(iAct, oAct);
    }
}
