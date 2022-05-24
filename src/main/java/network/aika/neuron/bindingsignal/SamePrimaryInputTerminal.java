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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;


/**
 * @author Lukas Molzberger
 */
public class SamePrimaryInputTerminal extends FixedTerminal {


    public SamePrimaryInputTerminal(State state) {
        super(state);
    }

    public static SamePrimaryInputTerminal fixedSamePrimaryInput(State s) {
        return new SamePrimaryInputTerminal(s);
    }
/*
    @Override
    public boolean transitionCheck(Synapse ts, BindingSignal bs, Direction dir) {
        if(!super.transitionCheck(ts, bs, dir))
            return false;

        if(dir == Direction.INPUT && !(bs.getLink() instanceof PrimaryInputLink))
            return false;

        if(dir == Direction.OUTPUT && !verifySamePrimaryInput(bs, (BindingNeuron) ts.getOutput()))
            return false;

        return true;
    }
*/
    private boolean verifySamePrimaryInput(BindingSignal refBS, BindingNeuron on) {
        Activation originAct = refBS.getOriginActivation();
        PrimaryInputSynapse primaryInputSyn = on.getPrimaryInputSynapse();
        if(primaryInputSyn == null)
            return false;

        return originAct.getReverseBindingSignals(primaryInputSyn.getInput())
                .findAny()
                .isPresent();
    }

    public String toString() {
        return "SPI" + super.toString();
    }
}
