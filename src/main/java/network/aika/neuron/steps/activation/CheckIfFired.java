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
import network.aika.neuron.steps.link.LinkCounting;
import network.aika.neuron.steps.link.PropagateBindingSignal;
import network.aika.utils.Utils;

import java.util.List;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 * Checks whether the net value exceeds the threshold of 0. If that is the case, the neuron will be fired.
 *
 * @author Lukas Molzberger
 */
public class CheckIfFired extends Step<Activation> {

    public static void add(Activation act) {
        Step.add(new CheckIfFired(act));
    }

    private CheckIfFired(Activation act) {
        super(act);
    }

    @Override
    public void process() {
        Activation act = getElement();

        act.updateValue();

        if (act.isFired() || act.getValue() <= 0.0)
            return;

        act.setFired();
        propagate(act);
        EntropyGradient.add(this);
    }

    public static void propagate(Activation<?> act) {
        Propagate.add(act);

        act.addFeedbackSteps();

        Counting.add(act);
        act.getInputLinks().forEach(l -> LinkCounting.add(l));

        if(Utils.belowTolerance(act.getOutputGradientSum()))
            return;

        TemplatePropagate.add(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    @Override
    public StepType getStepType() {
        return StepType.INFERENCE;
    }

    public boolean checkIfQueued() {
        return true;
    }

    public String toString() {
        return "Act-Step: CheckIfFired " + getElement().toShortString();
    }
}
