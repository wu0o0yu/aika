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
package network.aika.steps.activation;

import network.aika.neuron.activation.BindingActivation;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.steps.StepType;

/**
 * Check if there are positive recurrent links that have not been activated and thus need to be updated.
 *
 * @author Lukas Molzberger
 */
public class SetFinalMode extends Step<BindingActivation> {

    public static void add(BindingActivation act) {
        if(act.isFinalMode())
            return;

        Step.add(new SetFinalMode(act));
    }

    private SetFinalMode(BindingActivation act) {
        super(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.FINAL_LINKING;
    }

    @Override
    public StepType getStepType() {
        return StepType.INFERENCE;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process() {
        getElement()
                .setFinalMode();
    }

    public String toString() {
        return "Act-Step: Set final Mode " + getElement().toShortString();
    }
}
