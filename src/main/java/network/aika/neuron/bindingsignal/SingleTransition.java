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
import network.aika.neuron.activation.Activation;

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
        propagate(ts, fromBS, dir);
    }

    public void link(Synapse ts, BindingSignal fromBS, Direction dir) {
        Stream<BindingSignal<?>> bsStream = ts.getRelatedBindingSignal(fromBS, dir);

        bsStream.filter(toBS -> fromBS != toBS)
                .forEach(toBS ->
                        link(ts, fromBS, toBS, dir)
                );
    }

    protected void propagate(Synapse ts, BindingSignal<?> fromBS, Direction dir) {
        if (dir == INPUT)
            return;

        double tsWeight = ts.getWeight().getCurrentValue();
        double tnBias = ts.getOutput().getBias().getCurrentValue();

        if(tsWeight + tnBias > 0.0) {
            ts.propagate(fromBS, null);
        } else if(tsWeight + ts.getSumOfLowerWeights() > 0.0) {
            latentLinking(ts, fromBS, dir);
        }
    }

    private static void latentLinking(Synapse ts, BindingSignal<?> fromBS, Direction dir) {
        Neuron<?, ?> toNeuron = dir.getNeuron(ts);

        boolean templateEnabled = fromBS.getConfig().isTemplatesEnabled();
        toNeuron.getTargetSynapses(INPUT, templateEnabled)
                .filter(relatedTS -> ts != relatedTS)
                .forEach(relatedTS -> {
                    latentLinking(fromBS, ts, relatedTS);
                    latentLinking(fromBS, relatedTS, ts);
                });
    }

    private static void latentLinking(BindingSignal<?> fromBS, Synapse ts, Synapse latentTS) {
        fromBS.getRelatedBindingSignal(latentTS.getInput())
                .filter(relInputBS -> fromBS != relInputBS)
                .filter(relInputBS ->
                        isTrue(relInputBS.getOnArrivedFired())
                )
                .map(relInputBS ->
                        relInputBS.propagate(latentTS)
                )
                .filter(toBS ->
                        toBS != null && match(ts, fromBS, toBS, OUTPUT)
                )
                .forEach(toBS ->
                        ts.propagate(fromBS, toBS)
                );
    }

    public void link(Synapse ts, BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        if(!match(ts, fromBS, toBS, dir))
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
    public Stream<Terminal> getOutputTerminals() {
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

        if(!dir.getFromTerminal(this).linkCheck(ts, fromBS, toBS))
            return false;

        if(!dir.getTerminal(this).linkCheck(ts, toBS, fromBS))
            return false;

        return true;
    }

    public String toString() {
        return "Input:" + input +
                " Output:" + output +
                " Mode:" + mode;
    }
}
