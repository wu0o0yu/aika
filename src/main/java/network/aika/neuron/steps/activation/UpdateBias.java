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
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.StepType;
import network.aika.neuron.steps.UpdateNet;
import network.aika.utils.Utils;

/**
 *
 * @author Lukas Molzberger
 */
public class UpdateBias extends Step<Activation> {

    public static void add(Activation act, double biasDelta) {
        if (!act.getNeuron().isAllowTraining())
            return;

        if(Utils.belowTolerance(biasDelta))
            return;

        Step.add(new UpdateBias(act, biasDelta));
    }

    public UpdateBias(Activation act, double biasDelta) {
        super(act);
        this.biasDelta = biasDelta;
    }

    private final double biasDelta;

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    @Override
    public StepType getStepType() {
        return StepType.TRAINING;
    }

    public boolean checkIfQueued() {
        return false;
    }

    @Override
    public void process() {
        Activation act = getElement();

        act.getNeuron()
                .addBias(biasDelta);

        if(biasDelta <= 0.0)
            return;

        UpdateNet.updateNet(act, biasDelta);

        PostTraining.add(act);
    }

    public String toString() {
        return "Act-Step: Update Bias " + getElement().toShortString() + " (Delta:" + Utils.round(biasDelta) + ")";
    }
}
