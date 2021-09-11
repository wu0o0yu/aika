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
package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.Step;

import static network.aika.neuron.steps.activation.CheckIfFired.propagate;

/**
 * Check if there are positive recurrent links that have not been activated and thus need to be updated.
 *
 * @author Lukas Molzberger
 */
public class SetFinalMode extends UpdateNet {

    public static void add(Activation act) {
        if(act.isFinalMode())
            return;

        Step.add(new SetFinalMode(act));
    }

    private SetFinalMode(Activation act) {
        super(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.FINAL_LINKING;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process() {
        Activation act = getElement();

        act.setFinalMode(true);
        updateNet(act.getNeuron().getRecurrentBias());

        act.getInputLinks()
                .filter(l -> l.getSynapse().isRecurrent())
                .filter(l -> !l.isNegative())
                .forEach(l ->
                        l.getSynapse()
                                .propagateActValue(l, l.getInputValue(Sign.POS))
                );

        act.updateValue();

        if (!act.isFired() || act.getValue() > 0.0)
            return;

        act.setFired(null);
        propagate(act);
    }

    public String toString() {
        return "Act-Step: Set final Mode";
    }
}
