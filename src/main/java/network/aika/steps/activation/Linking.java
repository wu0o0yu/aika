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
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.PositiveFeedbackSynapse;
import network.aika.steps.LinkingOrder;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static void add(Activation act, BindingSignal bindingSignal, Direction dir, LinkingOrder linkingOrder, boolean template) {
        if(template && !act.getConfig().isTemplatesEnabled())
            return;

        if(bindingSignal.isOrigin())
            return;

        Linking step = new Linking(act, bindingSignal, dir, linkingOrder, template);

        if(step.hasTargetSynapses())
            Step.add(step);
    }

    private Direction direction;
    private LinkingOrder linkingOrder;
    private boolean template;

    private Linking(Activation act, BindingSignal<?> bindingSignal, Direction dir, LinkingOrder linkingOrder, boolean template) {
        super(act);

        this.linkingOrder = linkingOrder;
        this.direction = dir;
        this.template = template;
        this.bindingSignal = bindingSignal;

        Stream<? extends Synapse> tsyns = getElement().getNeuron()
                .getTargetSynapses(direction, template);

        if(dir == Direction.OUTPUT && linkingOrder == LinkingOrder.PRE_FIRED) {
            tsyns = tsyns.filter(ts -> ts instanceof PositiveFeedbackSynapse);
        }

        this.targetSynapses =  tsyns.collect(Collectors.toList());
    }

    private boolean hasTargetSynapses() {
        return targetSynapses != null && !targetSynapses.isEmpty();
    }

    @Override
    public LinkingOrder getLinkingOrder() {
        return linkingOrder;
    }

    private final BindingSignal<?> bindingSignal;
    private List<Synapse> targetSynapses;

    @Override
    public Phase getPhase() {
        return Phase.PROCESSING;
    }

    public boolean checkIfQueued() {
        return false;
    }

    @Override
    public void process() {
        targetSynapses.forEach(ts ->
                        link(ts)
                );
    }

    private void link(Synapse ts) {
        Activation fromAct = getElement();
        Neuron toNeuron = direction.getNeuron(ts);

        getRelatedBindingSignal(ts, bindingSignal, toNeuron)
                .filter(toBS -> bindingSignal != toBS)
                .filter(toBS ->
                        checkRelatedBindingSignal(ts, bindingSignal, toBS)
                )
                .map(toBS -> toBS.getActivation())
                .filter(toAct -> fromAct != toAct)
                .forEach(toAct ->
                        link(ts, fromAct, toAct, direction)
                );
    }

    private Stream<? extends BindingSignal> getRelatedBindingSignal(Synapse targetSynapse, BindingSignal fromBindingSignal, Neuron toNeuron) {
        Activation originAct = fromBindingSignal.getOriginActivation();
        Stream<? extends BindingSignal> relatedBindingSignals = originAct.getReverseBindingSignals(toNeuron);

        if(targetSynapse.allowLooseLinking()) {
            relatedBindingSignals = Stream.concat(
                    relatedBindingSignals,
                    originAct.getThought().getLooselyRelatedBindingSignals(fromBindingSignal, targetSynapse.getLooseLinkingRange(), toNeuron)
            );
        }

        return relatedBindingSignals;
    }

    private boolean checkRelatedBindingSignal(Synapse targetSynapse, BindingSignal fromBindingSignal, BindingSignal toBindingSignal) {
        BindingSignal inputBS = direction.getInput(fromBindingSignal, toBindingSignal);
        BindingSignal outputBS = direction.getOutput(fromBindingSignal, toBindingSignal);
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
        StringBuilder sb = new StringBuilder();
        sb.append(linkingOrder + " " + direction + " " + (template ? "Template " : "")  + getElement() + " - " + bindingSignal.getClass().getSimpleName() + ": " + bindingSignal);
        targetSynapses.forEach(ts ->
                sb.append("\n    " + ts)
        );
        return sb.toString();
    }
}
