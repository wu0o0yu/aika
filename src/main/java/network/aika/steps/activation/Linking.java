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
import network.aika.neuron.Linker;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.InhibitoryActivation;
import network.aika.neuron.activation.Timestamp;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import java.util.List;
import java.util.stream.Stream;

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
public class Linking extends Step<Activation> {

    public static void add(Activation act, BindingSignal bindingSignal) {
        Step.add(new Linking(act, bindingSignal, false));

        if(act.getConfig().isTemplatesEnabled())
            Step.add(new Linking(act, bindingSignal, true));
    }

    private boolean template;

    private Linking(Activation act, BindingSignal bindingSignal, boolean template) {
        super(act);

        this.template = template;

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
        return Phase.PROCESSING;
    }

    public boolean checkIfQueued() {
        return false;
    }

    @Override
    public void process() {
        Neuron<?, ?> fromN = getElement().getNeuron();
        getDirections().forEach(dir ->
                fromN.getTargetSynapses(dir, template)
                        .filter(ts ->
                                ts.checkBindingSignal(bindingSignal, dir)
                        )
                        .forEach(ts ->
                                link(ts, getElement(), dir, bindingSignal)
                        )
        );
    }

    public void link(Synapse targetSynapse, Activation<?> fromAct, Direction dir, BindingSignal<?> fromBindingSignal) {
        getRelatedBindingSignal(targetSynapse, fromBindingSignal)
                .filter(toBS -> fromBindingSignal != toBS)
                .filter(toBS ->
                        checkRelatedBindingSignal(targetSynapse, dir, fromBindingSignal, toBS)
                )
                .map(toBS -> toBS.getActivation())
                .filter(toAct -> fromAct != toAct)
                .forEach(toAct ->
                        link(targetSynapse, fromAct, toAct, dir)
                );
    }

    private Stream<? extends BindingSignal<?>> getRelatedBindingSignal(Synapse targetSynapse, BindingSignal<?> fromBindingSignal) {
        Activation originAct = fromBindingSignal.getOriginActivation();
        Stream<? extends BindingSignal<?>> relatedBindingSignals = originAct.getReverseBindingSignals();

        if(targetSynapse.allowLooseLinking()) {
            relatedBindingSignals = Stream.concat(
                    relatedBindingSignals,
                    originAct.getThought().getLooselyRelatedBindingSignals(fromBindingSignal, targetSynapse.getLooseLinkingRange())
            );
        }

        return relatedBindingSignals;
    }

    private boolean checkRelatedBindingSignal(Synapse targetSynapse, Direction dir, BindingSignal<?> fromBindingSignal, BindingSignal<?> toBindingSignal) {
        //           fromBindingSignal.checkRelatedBindingSignal(targetSynapse, toBindingSignal, dir)
        BindingSignal inputBS = dir.getInputBindingSignal(fromBindingSignal, toBindingSignal);
        BindingSignal outputBS = dir.getOutputBindingSignal(fromBindingSignal, toBindingSignal);
        return inputBS.checkRelatedBindingSignal(targetSynapse, outputBS);
    }

    private void link(Synapse targetSynapse, Activation<?> fromAct, Activation<?> toAct, Direction dir) {
        Activation iAct = dir.getInput(fromAct, toAct);
        Activation oAct = dir.getOutput(fromAct, toAct);

        if(!targetSynapse.checkLinkingPreConditions(iAct, oAct))
            return;

        targetSynapse.createLink(iAct, oAct);
    }

    public String toString() {
        return (template ? "Template " : " ")  + getElement() + " Binding-Signal:" + bindingSignal;
    }
}
