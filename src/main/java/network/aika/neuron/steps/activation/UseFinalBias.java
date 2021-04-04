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
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.steps.Phase;

import static network.aika.neuron.activation.Activation.TOLERANCE;

/**
 * Check if there are positive recurrent links that have not been activated and thus need to be updated.
 *
 * @author Lukas Molzberger
 */
public class UseFinalBias implements ActivationStep {

    @Override
    public Phase getPhase() {
        return Phase.FINAL_LINKING;
    }

    @Override
    public void process(Activation act) {
        double delta = act.updateValue(true);

        if(Math.abs(delta) >= TOLERANCE)
            QueueEntry.add(act, new PropagateChange(delta));
    }

    public String toString() {
        return "Act-Step: Use Final Bias";
    }
}
