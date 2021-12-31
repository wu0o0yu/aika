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
package network.aika.steps.link;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.activation.Link;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.steps.StepType;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.BindingSignal.transitionBindingSignals;

/**
 * Propagates the binding signal to the next activation.
 *
 * @author Lukas Molzberger
 */
public class PropagateBindingSignal extends Step<Link> {

    protected Collection<BindingSignal> inputBindingSignals;


    public static void add(Link<?> l) {
        Step.add(new PropagateBindingSignal(l,
                l.getInput().getBindingSignals()
                        .stream()
                        .filter(bs -> bs.checkPropagate())
                        .collect(Collectors.toList()))
        );
    }

    public static void add(Activation<?> act, Stream<BindingSignal> bindingSignals) {
        Collection<BindingSignal> outgoingBindingSignals = bindingSignals
                .filter(bs -> bs.checkPropagate())
                .collect(Collectors.toList());

        if(outgoingBindingSignals.isEmpty())
            return;

        act.getOutputLinks().forEach(l ->
                Step.add(new PropagateBindingSignal(l, outgoingBindingSignals))
        );
    }

    protected PropagateBindingSignal(Link l, Collection<BindingSignal> inputBindingSignals) {
        super(l);
        fired = l.getInput().getFired();
        this.inputBindingSignals = inputBindingSignals;
    }

    @Override
    public void process() {
        Activation<?> oAct = getElement().getOutput();

        add(oAct, transitionBindingSignals(getElement(), inputBindingSignals)
                .map(bs -> oAct.addBindingSignal(bs))
                .filter(bs -> bs != null));
    }

    @Override
    public Phase getPhase() {
        return Phase.BINDING_SIGNAL;
    }

    @Override
    public StepType getStepType() {
        return StepType.INFERENCE;
    }

    public boolean checkIfQueued() {
        return true;
    }

    public String toString() {
        return "Link-Step: Propagate Binding Signal " + getElement().toShortString();
    }
}
