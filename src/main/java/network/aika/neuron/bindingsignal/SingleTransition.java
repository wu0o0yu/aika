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
public class SingleTransition<I extends Terminal, O extends Terminal> implements Transition {

    protected I input;
    protected O output;
    protected TransitionMode mode;

    protected SingleTransition(I input, O output, TransitionMode mode) {
        this.input = input;
        this.output = output;
        this.mode = mode;
        input.setType(INPUT);
        output.setType(OUTPUT);
        setTerminalTransition(this);
    }

    public void setTerminalTransition(Transition t) {
        input.setTransition(t);
        output.setTransition(t);
    }

    public static <I extends Terminal, O extends Terminal> SingleTransition<I, O> transition(I input, O output, TransitionMode transitionMode) {
        return new SingleTransition(input, output, transitionMode);
    }

    @Override
    public void notify(Terminal t, Synapse ts, BindingSignal bs) {
        linkAndPropagate(t, ts, bs);
    }

    @Override
    public void registerTransitionEvent(FixedTerminal t, Synapse ts, Activation act, FieldOutput bsEvent) {
        FieldOutput transitionEvent = getTransitionEvent(
                ts,
                act,
                t.getType().invert(),
                bsEvent
        );

        transitionEvent.addEventListener(() ->
                linkAndPropagate(
                        t,
                        ts,
                        t.getBindingSignal(bsEvent)
                )
        );
    }

    private void linkAndPropagate(Terminal t, Synapse ts, BindingSignal fromBS) {
        link(ts, fromBS, t.getType().invert());
        propagate(ts, fromBS, t.getType().invert());
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

        ts.propagate(fromBS);
    }

    public void link(Synapse ts, BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        if(!linkCheck(ts, fromBS, toBS, dir))
            return;

        BindingSignal inputBS = dir.getInput(fromBS, toBS);
        BindingSignal outputBS = dir.getOutput(fromBS, toBS);

        ts.link(inputBS, outputBS);
    }

    @Override
    public Stream<VariableTerminal> getVariableTerminals(Synapse ts, BindingSignal bs, Direction dir) {
        if(mode == PROPAGATE_ONLY)
            return Stream.empty();

        Terminal term = dir.getFromTerminal(this);

        if(term.getState() != bs.getState())
            return Stream.empty();

        if(!(term instanceof VariableTerminal))
            return Stream.empty();

        return Stream.of((VariableTerminal) term);
    }

    @Override
    public Stream<FixedTerminal> getFixedTerminals(Synapse ts, Activation act, Direction dir) {
        if(mode == PROPAGATE_ONLY)
            return Stream.empty();

        Terminal term = dir.getFromTerminal(this);

        if(!(term instanceof FixedTerminal))
            return Stream.empty();

        return Stream.of((FixedTerminal) term);
    }

    @Override
    public Stream<SingleTransition> getBSPropagateTransitions(State s) {
        if(mode == MATCH_ONLY)
            return Stream.empty();

        if(s != input.getState())
            return Stream.empty();

        return Stream.of(this);
    }

    public Terminal getInput() {
        return input;
    }

    public Terminal getOutput() {
        return output;
    }

    public State next(Direction dir) {
        return (dir == OUTPUT ? output : input).getState();
    }

    public TransitionMode getMode() {
        return mode;
    }

    public boolean linkCheck(Synapse ts, BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        if(mode == PROPAGATE_ONLY)
            return false;

//        if (!isTrue(ts.getLinkingEvent(toBS, dir))) TODO
//            return false;

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
