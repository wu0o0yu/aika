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
import network.aika.neuron.activation.BindingSignal;
import network.aika.neuron.activation.Link;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.steps.StepType;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.direction.Direction.OUTPUT;

/**
 * Propagates the binding signal to the next activation.
 *
 * @author Lukas Molzberger
 */
public class PropagateBindingSignal extends Step<Link> {

    protected Collection<BindingSignal> inputBindingSignals;


    public static void add(Link l) {
        Step.add(new PropagateBindingSignal(l, l.getInput().getBindingSignals().values()));
    }

    public static void add(Link l, Collection<BindingSignal> inputBindingSignals) {
        Step.add(new PropagateBindingSignal(l, inputBindingSignals));
    }

    protected PropagateBindingSignal(Link l, Collection<BindingSignal> inputBindingSignals) {
        super(l);
        fired = l.getInput().getFired();
        this.inputBindingSignals = inputBindingSignals;
    }

    protected Stream<BindingSignal> transitionScopes(Link l, Collection<BindingSignal> bindingSignals) {
        return bindingSignals.stream()
                .filter(bs -> // Block Binding-Signal from propagating too far.
                        bs.getCurrentAct() == l.getInput() ||
                                bs.getBindingSignalAct().getType() != l.getInput().getType()
                )
                .map(bs -> propagateBindingSignal(l, bs))
                .filter(e -> e != null);
    }

    private BindingSignal propagateBindingSignal(Link l, BindingSignal bs) {
        Byte oScope = l.getSynapse().transitionScope(bs.getScope(), OUTPUT);
        if(oScope == null)
            return null;

        return new BindingSignal(
                bs,
                bs.getBindingSignalAct(),
                l.getOutput(),
                oScope,
                (byte) (bs.getDepth() + 1)
        );
    }

    @Override
    public void process() {
        Activation<?> oAct = getElement().getOutput();

        List<BindingSignal> outputBindingSignals = transitionScopes(getElement(), inputBindingSignals)
                .filter(bs -> !oAct.checkIfBindingSignalExists(bs))
                .collect(Collectors.toList());

        if(outputBindingSignals.isEmpty())
            return;

        oAct.addBindingSignals(outputBindingSignals.stream());

        oAct.getOutputLinks().forEach(l ->
                add(l, outputBindingSignals)
        );
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
