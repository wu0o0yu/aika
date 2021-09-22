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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.StepType;
import network.aika.neuron.steps.activation.PostTraining;
import network.aika.utils.Utils;

import static network.aika.neuron.activation.Activation.INCOMING;
import static network.aika.neuron.activation.Activation.OWN;

/**
 * Propagate the gradient backwards through the network.
 *
 * @author Lukas Molzberger
 */
public class PropagateGradientAndUpdateWeight extends Step<Link> {

    private final double[] gradient;

    public static void add(Link l, double[] gradient) {
        if (Utils.belowTolerance(gradient))
            return;

        Step.add(new PropagateGradientAndUpdateWeight(l, gradient));
    }

    public static void addInputs(Activation<?> act, double[] gradient) {
        if(act.getNeuron().isInputNeuron())
            return;

        act.getInputLinks().forEach(l -> PropagateGradientAndUpdateWeight.add(l, gradient));
    }

    private PropagateGradientAndUpdateWeight(Link l, double[] gradient) {
        super(l);
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
    public StepType getStepType() {
        return StepType.TRAINING;
    }

    @Override
    public void process() {
        Link l = getElement();
        Synapse s = l.getSynapse();

        if(!l.getSynapse().isAllowTraining())
            return;

        double g = gradient[OWN] + gradient[INCOMING];
        double weightDelta = l.getConfig().getLearnRate() * g;
        boolean oldWeightIsZero = s.isZero();

        s.updateSynapse(l, weightDelta);

        if (oldWeightIsZero && !s.isZero() && l.getInput().isFired()) {
            PropagatePatternBindingSignal.add(l);
        }
        PostTraining.add(l.getOutput());

        s.propagateGradient(l, gradient);
    }

    public String toString() {
        return "Link-Step: Propagate Gradient (Own:" + Utils.round(gradient[OWN]) + ", Incoming:" + Utils.round(gradient[INCOMING]) + ")";
    }
}
