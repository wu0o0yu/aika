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
import network.aika.neuron.steps.PropagateGradients;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.StepType;

/**
 * Propagates the gradient of this activation backwards to all its input-links.
 *
 * @author Lukas Molzberger
 */
public class PropagateGradientsNet extends PropagateGradients {

    public static void add(Activation act) {
        if(act.markedNetUpdateOccurred)
            return;

        Step.add(new PropagateGradientsNet(act));
    }

    private PropagateGradientsNet(Activation act) {
        super(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.FINAL_LINKING;
    }

    @Override
    public StepType getStepType() {
        return StepType.TRAINING;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process() {
        Activation act = getElement();
        double[] g = act.gradientsFromNetUpdate();
        if(g == null)
            return;

        propagateGradientsOut(act, g);

        act.markedNetUpdateOccurred = true;
    }

    public String toString() {
        return "Act-Step: Propagate Gradients from Net Update " + getElement().toShortString();
    }
}
