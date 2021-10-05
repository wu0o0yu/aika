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
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.steps.*;
import network.aika.neuron.linker.LinkingTask;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;


/**
 * The job of the linking phase is to propagate information through the network by creating the required activations and links.
 * Each activation and each link have an corresponding neuron or synapse respectively. Depending on the data set in the
 * document, a neuron might have several activations associated with it. During propagation an input activation
 * causes the creating of a link in one or more output synapses and the creation of an output activation. Initially the value
 * of the input activation and the weight of the synapse might not suffice to activate the output activation. But that might
 * change later on as more input links are added to the activation. New input links are added by the closeCycle method. This
 * method is called by the visitor which follows the links in the activation network to check that both input and output
 * activation of a new link refer to the same object in the input data set.
 *
 * @author Lukas Molzberger
 */
public class Propagate extends TaskStep<Activation, LinkingTask> {


    public static void add(Activation act) {
        Step.add(new Propagate(act));
    }

    private Propagate(Activation act) {
        super(act, new LinkingTask());
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    @Override
    public StepType getStepType() {
        return StepType.INFERENCE;
    }

    @Override
    public void process() {
        if(!getElement().isFired())
            return;

        task.propagate(getElement());
    }

    public boolean checkIfQueued() {
        return true;
    }

    public String toString() {
        return "Act-Step: Propagate " + getElement().toShortString();
    }
}
