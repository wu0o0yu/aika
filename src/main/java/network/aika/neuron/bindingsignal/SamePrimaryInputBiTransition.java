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
package network.aika.neuron.bindingsignal;

import network.aika.direction.Direction;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.PrimaryInputLink;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;


/**
 * @author Lukas Molzberger
 */
public class SamePrimaryInputBiTransition extends BiTransition {


    protected SamePrimaryInputBiTransition(State input, State output, TransitionMode transitionMode) {
        super(input, output, transitionMode);
    }

    public static SamePrimaryInputBiTransition samePrimaryInputBiTransition(State input, State output, TransitionMode transitionMode) {
        return new SamePrimaryInputBiTransition(input, output, transitionMode);
    }

    public boolean linkCheck(Synapse ts, BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        if(!super.linkCheck(ts, fromBS, toBS, dir))
            return false;

        BindingSignal iBS = dir.getInput(fromBS, toBS);
        BindingSignal<BindingActivation> oBS = dir.getOutput(fromBS, toBS);

        if(!(oBS.getLink() instanceof PrimaryInputLink))
            return false;

        if(!verifySamePrimaryInput(
                iBS,
                (BindingNeuron) ts.getOutput()
        ))
            return false;

        return true;
    }

    private boolean verifySamePrimaryInput(BindingSignal refBS, BindingNeuron on) {
        Activation originAct = refBS.getOriginActivation();
        PrimaryInputSynapse primaryInputSyn = on.getPrimaryInputSynapse();
        if(primaryInputSyn == null)
            return false;

        return originAct.getReverseBindingSignals(primaryInputSyn.getInput())
                .findAny()
                .isPresent();
    }
}
