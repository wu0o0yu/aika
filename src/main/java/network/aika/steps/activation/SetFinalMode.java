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
import network.aika.neuron.activation.PatternActivation;
import network.aika.steps.Phase;
import network.aika.steps.Step;

/**
 * Check if there are positive recurrent links that have not been activated and thus need to be updated.
 *
 * @author Lukas Molzberger
 */
public class SetFinalMode extends Step<PatternActivation> {

    public static void add(PatternActivation act) {
        if(act.isFinalMode())
            return;

        Step.add(new SetFinalMode(act));
    }

    private SetFinalMode(PatternActivation act) {
        super(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.PROCESSING;
    }

    @Override
    public void process() {
        getElement()
                .setFinalMode();
    }
}
