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
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.StepType;
import network.aika.utils.Utils;

/**
 * Creates a new untrained neuron from a template activation.
 *
 * @author Lukas Molzberger
 */
public class Induction extends Step<Activation> {

    public static void add(Activation act) {
        Step.add(new Induction(act));
    }

    private Induction(Activation act) {
        super(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    @Override
    public StepType getStepType() {
        return StepType.TEMPLATE;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process() {
        Activation act = getElement();

        assert act.getNeuron().isTemplate();

        Neuron inducedNeuron = act.getNeuron().instantiateTemplate(true);
        inducedNeuron.setLabel(act.getConfig().getLabel(act));

        act.unlink();

        act.setNeuron(inducedNeuron);

        act.link();

        if(Utils.belowTolerance(inducedNeuron.getBias().getOldValue()))
            return;
    }

    public String toString() {
        return "Act-Step: Induction " + getElement().toShortString();
    }
}
