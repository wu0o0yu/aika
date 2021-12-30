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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.steps.StepType;
import network.aika.steps.LinkerStep;
import network.aika.linker.TemplateTask;

/**
 * Uses the Template Network defined in the {@link network.aika.neuron.Templates} to induce new template
 * activations and links.
 *
 * @author Lukas Molzberger
 */
public class TemplateLinking extends LinkerStep<Activation, TemplateTask> {


    public static void add(Activation act, BindingSignal bindingSignal) {
        Step.add(new TemplateLinking(act, bindingSignal));
    }

    private TemplateLinking(Activation act, BindingSignal bindingSignal) {
        super(act, bindingSignal, new TemplateTask());
    }

    @Override
    public Phase getPhase() {
        return Phase.TEMPLATE;
    }

    @Override
    public StepType getStepType() {
        return StepType.TEMPLATE;
    }

    @Override
    public void process() {
        task.link(getElement(), getDirections(), bindingSignal);
    }

    public boolean checkIfQueued() {
        return false;
    }

    public String toString() {
        return "Act-Step: Template-Linking " + getElement().toShortString() + " Binding-Signal:" + bindingSignal;
    }
}
