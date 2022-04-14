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
import network.aika.neuron.activation.InhibitoryActivation;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.Timestamp;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.NegativeFeedbackSynapse;
import network.aika.neuron.conjunctive.PatternSynapse;
import network.aika.neuron.conjunctive.PositiveFeedbackSynapse;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    private static void addInternal(BindingSignal bs, Direction dir, Timestamp fired, String linkingType, Predicate<Synapse> filter) {
        if (bs.isOrigin())
            return;

        Linking step = new Linking(bs, dir, fired,  linkingType, filter);

        if (step.hasTargetSynapses())
            Step.add(step);
    }

    public static void addPreFired(BindingSignal bs) {
        addInternal(bs, INPUT, bs.getActivation().getFired(), "", s -> true);
    }

    public static void addPostFired(BindingSignal bs) {
        addInternal(bs, OUTPUT, bs.getActivation().getFired(), "", s -> true);
    }

    public static void addUnboundLinking(BindingSignal bs) {
        addInternal(bs, OUTPUT, bs.getActivation().getFired(), "PATTERN-SYN", s -> s instanceof PatternSynapse);
    }

    public static void addPosFeedback(BindingSignal<PatternActivation> bs) {
        addInternal(bs, OUTPUT, bs.getOriginActivation().getFired(), "POS-FEEDBACK", s -> s instanceof PositiveFeedbackSynapse);
    }
    public static void addNegFeedback(BindingSignal<InhibitoryActivation> bs) {
        addInternal(bs, OUTPUT, bs.getOriginActivation().getFired(), "NEG-FEEDBACK", s -> s instanceof NegativeFeedbackSynapse);
    }

    private Direction direction;
    private final BindingSignal<?> bindingSignal;
    private List<Synapse> targetSynapses;


    private Linking(BindingSignal<?> bindingSignal, Direction dir, Timestamp fired, String linkingType, Predicate<Synapse> filter) {
        super(bindingSignal.getActivation());

        this.direction = dir;
        this.bindingSignal = bindingSignal;
        this.fired = fired;

        Neuron<?, ?> n = getElement().getNeuron();

        Stream<? extends Synapse> targetSynapsesStream = n.getTargetSynapses(direction, false);
        if(bindingSignal.getActivation().getConfig().isTemplatesEnabled())
            targetSynapsesStream = Stream.concat(targetSynapsesStream, n.getTargetSynapses(direction, true));

        this.targetSynapses = targetSynapsesStream
                .filter(filter)
                .collect(Collectors.toList());
    }

    private boolean hasTargetSynapses() {
        return targetSynapses != null && !targetSynapses.isEmpty();
    }

    @Override
    public Phase getPhase() {
        return Phase.PROCESSING;
    }

    @Override
    public void process() {
        targetSynapses.forEach(ts ->
                        link(ts)
                );
    }

    private void link(Synapse ts) {
        Neuron toNeuron = direction.getNeuron(ts);

        getRelatedBindingSignal(ts, bindingSignal, toNeuron)
                .filter(toBS -> bindingSignal != toBS)
                .forEach(toBS ->
                        link(ts, bindingSignal, toBS)
                );
    }

    private Stream<BindingSignal<?>> getRelatedBindingSignal(Synapse targetSynapse, BindingSignal fromBindingSignal, Neuron toNeuron) {
        Activation originAct = fromBindingSignal.getOriginActivation();
        Stream<BindingSignal<?>> relatedBindingSignals = originAct.getReverseBindingSignals(toNeuron);

        if(targetSynapse.allowLooseLinking()) {
            relatedBindingSignals = Stream.concat(
                    relatedBindingSignals,
                    originAct.getThought().getLooselyRelatedBindingSignals(fromBindingSignal, targetSynapse.getLooseLinkingRange(), toNeuron)
            );
        }

        return relatedBindingSignals;
    }

    private void link(Synapse targetSynapse, BindingSignal fromBS, BindingSignal toBS) {
        BindingSignal inputBS = direction.getInput(fromBS, toBS);
        BindingSignal outputBS = direction.getOutput(fromBS, toBS);

        if(!targetSynapse.linkingCheck(inputBS, outputBS))
            return;

        targetSynapse.createLink(inputBS, outputBS);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(direction + " " + getElement() + " - " + bindingSignal.getClass().getSimpleName() + ": " + bindingSignal);
        targetSynapses.forEach(ts ->
                sb.append("\n    " + ts)
        );
        return sb.toString();
    }
}
