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
package network.aika.neuron.bindingsignal;

import network.aika.direction.Direction;
import network.aika.fields.FieldOutput;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_ONLY;
import static network.aika.neuron.bindingsignal.TransitionMode.PROPAGATE_ONLY;

/**
 * @author Lukas Molzberger
 */
public class SingleTransition<I extends SingleTerminal, O extends SingleTerminal> implements Transition {

    private static final Logger log = LoggerFactory.getLogger(SingleTransition.class);

    protected I input;
    protected O output;
    protected TransitionMode mode;

    protected SingleTransition(I input, O output, TransitionMode mode) {
        this.input = input;
        this.output = output;
        this.mode = mode;
        input.setType(INPUT);
        output.setType(OUTPUT);

        input.setTransition(this);
        output.setTransition(this);
    }

    public static <I extends SingleTerminal, O extends SingleTerminal> SingleTransition<I, O> transition(I input, O output, TransitionMode transitionMode) {
        return new SingleTransition(input, output, transitionMode);
    }

    public void linkAndPropagate(Synapse ts, BindingSignal fromBS, Direction dir) {
        link(ts, fromBS, dir);
        latentLinking(this, ts, fromBS, dir);
        propagate(this, ts, fromBS, dir);
    }

    public void link(Synapse ts, BindingSignal fromBS, Direction dir) {
        Stream<BindingSignal<?>> bsStream = ts.getRelatedBindingSignals(fromBS, dir);

        bsStream
                .filter(toBS -> fromBS != toBS)
                .forEach(toBS ->
                        link(this, ts, fromBS, toBS, dir)
                );
    }

    public static void propagate(SingleTransition t, Synapse ts, BindingSignal<?> fromBS, Direction dir) {
        if (dir != OUTPUT)
            return;

        if(!ts.isPropagate())
            return;

        ts.propagate(fromBS, null);
    }

    public static void latentLinking(SingleTransition t, Synapse synA, BindingSignal<?> fromBS, Direction dir) {
        if(dir != OUTPUT)
            return;

        Neuron<?, ?> toNeuron = dir.getNeuron(synA);

        boolean templateEnabled = fromBS.getConfig().isTemplatesEnabled();
        toNeuron.getTargetSynapses(INPUT, templateEnabled)
                .filter(synB -> synA != synB)
                .filter(synB -> synA.isLatentLinking() || synB.isLatentLinking())
                .filter(synB -> synB.hasOutputTerminal(t.getOutput().getState()))
                .forEach(synB ->
                        latentLinking(t, fromBS, synA, synB)
                );
    }

    private static void latentLinking(SingleTransition tA, BindingSignal bsA, Synapse synA, Synapse synB) {
        log.info("Check latent-link synA:" + synA + "  synB:" + synB);

        Stream<BindingSignal> bsStream = synB.getInput().getRelatedBindingSignals(bsA);
        bsStream.filter(bsB -> bsA != bsB)
                .filter(bsB ->
                        isTrue(bsB.getOnArrivedFired())
                )
                .forEach(bsB -> {
                    Stream<SingleTransition> relTrans = synB.getRelatedTransitions(bsB, tA);
                    relTrans.forEach(tB -> {
                        latentLinking(synA, bsA, tA, synB, bsB, tB);
                        latentLinking(synB, bsB, tB, synA, bsA, tA);
                    });
                });
    }

    private static void latentLinking(
            Synapse targetSyn,
            BindingSignal<?> fromBS,
            SingleTransition matchingTransition,
            Synapse latentSyn,
            BindingSignal<?> relBS,
            SingleTransition propagateTransition
    ) {
        if(!targetSyn.isLatentLinking())
            return;

        if (!propagateTransition.getInput().linkCheck(latentSyn, relBS))
            return;

        BindingSignal toBS = propagateTransition.propagate(relBS);
        if (toBS == null)
            return;

        if (!matchingTransition.match(targetSyn, fromBS, toBS, OUTPUT))
            return;

        targetSyn.propagate(fromBS, toBS);
    }

    public static void link(SingleTransition t, Synapse ts, BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        if(!t.match(ts, fromBS, toBS, dir))
            return;

        BindingSignal inputBS = dir.getInput(fromBS, toBS);
        BindingSignal outputBS = dir.getOutput(fromBS, toBS);

        ts.link(inputBS, outputBS);
    }

    @Override
    public Stream<Terminal> getInputTerminals() {
        return Stream.of(input);
    }

    @Override
    public Stream<SingleTerminal> getOutputTerminals() {
        return Stream.of(output);
    }

    public I getInput() {
        return input;
    }

    public O getOutput() {
        return output;
    }

    public State next(Direction dir) {
        return (dir == OUTPUT ? output : input).getState();
    }

    public TransitionMode getMode() {
        return mode;
    }

    public BindingSignal propagate(BindingSignal bs) {
        if(mode == MATCH_ONLY)
            return null;

        return bs.next(this);
    }

    public boolean match(Synapse ts, BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        if(mode == PROPAGATE_ONLY)
            return false;

        FieldOutput linkingEvent = ts.getLinkingEvent(toBS.getActivation(), dir.invert());
        if (linkingEvent != null && !isTrue(linkingEvent))
            return false;

        if(!dir.getFromTerminal(this).linkCheck(ts, fromBS))
            return false;

        if(!dir.getTerminal(this).linkCheck(ts, toBS))
            return false;

        return true;
    }

    public String toString() {
        return "Input:" + input +
                " Output:" + output +
                " Mode:" + mode;
    }
}
