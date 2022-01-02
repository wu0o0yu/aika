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
package network.aika.steps.activation;

import network.aika.direction.Direction;
import network.aika.linker.AbstractLinker;
import network.aika.linker.TemplateTask;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.InhibitoryActivation;
import network.aika.neuron.activation.Timestamp;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.steps.StepType;
import network.aika.linker.LinkingTask;

import java.util.List;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;


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
public class Link extends Step<Activation> {

    public static void add(Activation act, BindingSignal bindingSignal, boolean template) {
        Step.add(new Link(act, bindingSignal, template));
    }

    private final AbstractLinker linker;
    private boolean template;

    private Link(Activation act, BindingSignal bindingSignal, boolean template) {
        super(act);

        this.template = template;
        this.linker = template ? new TemplateTask() : new LinkingTask();
        this.bindingSignal = bindingSignal;

        Timestamp bsFired = bindingSignal.getOriginActivation().getFired();
        if(act.getFired().compareTo(bsFired) >= 0)
            this.fired = bsFired;
    }

    protected final BindingSignal bindingSignal;

    protected List<Direction> getDirections() {
        if(getElement() instanceof InhibitoryActivation)
            return List.of(OUTPUT);

        return List.of(INPUT, OUTPUT);
    }

    @Override
    public Phase getPhase() {
        return template ? Phase.TEMPLATE : Phase.LINKING;
    }

    @Override
    public StepType getStepType() {
        return template ? StepType.TEMPLATE : StepType.INFERENCE;
    }

    public boolean checkIfQueued() {
        return false;
    }

    @Override
    public void process() {
        linker.link(
                getElement(),
                getDirections(),
                bindingSignal
        );
    }

    public String toString() {
        return "Act-Step: " + (template ? "Template-" : "") + "Link " + getElement().toShortString() + " Binding-Signal:" + bindingSignal;
    }
}
