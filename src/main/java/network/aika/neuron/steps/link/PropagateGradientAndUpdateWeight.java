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
package network.aika.neuron.steps.link;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.steps.Phase;
import network.aika.utils.Utils;
import network.aika.neuron.activation.Link;

import static network.aika.neuron.activation.Activation.INCOMING;
import static network.aika.neuron.activation.Activation.OWN;
import static network.aika.neuron.steps.activation.ActivationStep.UPDATE_SYNAPSE_INPUT_LINKS;

/**
 * Propagate the gradient backwards through the network.
 *
 * @author Lukas Molzberger
 */
public class PropagateGradientAndUpdateWeight implements LinkStep {

    private double[] gradient;

    public PropagateGradientAndUpdateWeight(double[] gradient) {
        this.gradient = gradient;
    }

    public boolean checkIfQueued() {
        return false;
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    @Override
    public void process(Link l) {
        if(l.getSynapse().isAllowTraining()) {
            double g = gradient[OWN] + gradient[INCOMING];
            double weightDelta = l.getConfig().getLearnRate() * g;
            Synapse s = l.getSynapse();

            s.updateSynapse(l, weightDelta);

            QueueEntry.add(l.getOutput(), UPDATE_SYNAPSE_INPUT_LINKS);
        }

        l.propagateGradient(gradient[OWN]);
    }

    public String toString() {
        return "Link-Step: Propagate Gradient (Own:" + Utils.round(gradient[OWN]) + ", Incoming:" + Utils.round(gradient[INCOMING]) + ")";
    }
}
