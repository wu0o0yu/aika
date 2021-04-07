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
import network.aika.neuron.steps.Phase;
import network.aika.utils.Utils;

/**
 * During the initial linking process all positive recurrent synapses are assumed to be
 * active. If that is not the case, updates of the affected activations are required.
 *
 * @author Lukas Molzberger
 */
public class PropagateChange implements ActivationStep {

    private double valueDelta;

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    public boolean checkIfQueued() {
        return false;
    }

    public PropagateChange(double valueDelta) {
        this.valueDelta = valueDelta;
    }

    @Override
    public void process(Activation act) {
        act.updateOutgoingLinks(valueDelta);
    }

    public String toString() {
        return "Act-Step: Propagate Change (" + Utils.round(valueDelta) + ")";
    }
}
