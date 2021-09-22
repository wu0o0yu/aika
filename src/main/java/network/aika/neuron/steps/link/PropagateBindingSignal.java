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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.StepType;
import network.aika.neuron.steps.activation.Linking;
import network.aika.neuron.steps.activation.TemplateLinking;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class PropagateBindingSignal<A extends Activation> extends Step<Link> {

    protected Map<A, Byte> outputBindingSignals;


    public static void add(Link l, byte type) {
        Step.add(new PropagateBindingSignal(l, l.getInput().getBindingSignals(type)));
    }

    public static void add(Link l, Activation bindingSignal, Byte scope) {
        Step.add(new PropagateBindingSignal(l, Collections.singletonMap(bindingSignal, scope)));
    }

    protected PropagateBindingSignal(Link l, Map<A, Byte> inputBindingSignals) {
        super(l);
        transitionScopes(l, inputBindingSignals);
    }

    protected void transitionScopes(Link l, Map<A, Byte> inputBindingSignals) {
        this.outputBindingSignals = inputBindingSignals.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                e -> e.getKey(),
                                e -> l.getSynapse().transitionScope(e.getValue(), OUTPUT)
                        )
                );
    }

    @Override
    public void process() {
        Activation oAct = getElement().getOutput();

        oAct.addBindingSignals(outputBindingSignals);

        Linking.add(oAct, outputBindingSignals);
        TemplateLinking.add(oAct, outputBindingSignals, List.of(INPUT, OUTPUT));
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
}
