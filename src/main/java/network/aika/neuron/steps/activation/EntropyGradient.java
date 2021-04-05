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

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.steps.Phase;

/**
 * Computes the gradient of the entropy function for this activation.
 *
 * @see <a href="https://aika.network/training.html">Aika Training</a>
 *
 * @author Lukas Molzberger
 */
public class EntropyGradient implements ActivationStep {

    @Override
    public Phase getPhase() {
        return Phase.INIT;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process(Activation act) {
        Neuron n = act.getNeuron();

        if(n.isTemplate())
            return;

        act.initEntropyGradient();

        if(!act.gradientIsZero())
            QueueEntry.add(act, PROPAGATE_GRADIENTS_SUM);
    }

    public String toString() {
        return "Act-Step: Entropy Gradient";
    }
}
