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

import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.StepType;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.neuron.steps.activation.CheckIfFired.propagate;

/**
 * Check if there are positive recurrent links that have not been activated and thus need to be updated.
 *
 * @author Lukas Molzberger
 */
public class SetFinalMode extends Step<BindingActivation> {

    public static void add(BindingActivation act) {
        if(act.isFinalMode())
            return;

        Step.add(new SetFinalMode(act));
    }

    private SetFinalMode(BindingActivation act) {
        super(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.FINAL_LINKING;
    }

    @Override
    public StepType getStepType() {
        return StepType.INFERENCE;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process() {
        BindingActivation act = getElement();

        act.setFinalMode(true);

        double assumedActiveSum = act.getNeuron().getAssumedActiveSum().getNewValueAndAcknowledgePropagated();
        act.getNet().addAndTriggerUpdate(assumedActiveSum - computeForwardLinkedRecurrentInputs(act));

        getPositiveRecurrentInputLinks(act)
                .filter(l -> !l.isForward())
                .forEach(l ->
                        l.updateNetByInputValue(l.getInputValue(Sign.POS))
                );

        act.setFinalTimestamp();

        if (!act.isFired() || act.getNet().getOldValue() > 0.0)
            return;

        act.setFired(NOT_SET);
        propagate(act);
    }

    private double computeForwardLinkedRecurrentInputs(BindingActivation act) {
        return getPositiveRecurrentInputLinks(act)
                .filter(l -> l.isForward())
                .mapToDouble(l -> l.getSynapse().getWeight().getOldValue())
                .sum();
    }

    private Stream<Link> getPositiveRecurrentInputLinks(BindingActivation act) {
        return act.getInputLinks()
                .filter(l -> l.getSynapse().isRecurrent())
                .filter(l -> !l.isNegative());
    }

    public String toString() {
        return "Act-Step: Set final Mode " + getElement().toShortString();
    }
}
