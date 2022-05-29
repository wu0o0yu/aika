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
import static network.aika.fields.Fields.mul;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_ONLY;


/**
 * @author Lukas Molzberger
 */
public class BiTransition implements Transition {

    protected SingleTransition activeTransition;
    protected SingleTransition<FixedTerminal, FixedTerminal> passiveTransition;

    protected BiTransition(SingleTransition activeTransition, SingleTransition<FixedTerminal, FixedTerminal> passiveTransition) {
        this.activeTransition = activeTransition;
        this.passiveTransition = passiveTransition;

        activeTransition.setTerminalTransition(this);
        passiveTransition.setTerminalTransition(this);
    }

    public static BiTransition biTransition(SingleTransition activeTransition, SingleTransition passiveTransition) {
        return new BiTransition(activeTransition, passiveTransition);
    }

    @Override
    public Stream<FixedTerminal> getFixedTerminals(Synapse ts, Activation act, Direction dir) {
        return activeTransition.getFixedTerminals(ts, act, dir);
    }

    @Override
    public Stream<VariableTerminal> getVariableTerminals(Synapse ts, BindingSignal bs, Direction dir) {
        return activeTransition.getVariableTerminals(ts, bs, dir);
    }

    private FixedTerminal getPassiveTerminal(Terminal t, Synapse ts, Activation act) {
        FixedTerminal passiveTerminal = passiveTransition.getFixedTerminals(ts, act, t.getType().invert())
                .findFirst()
                .orElse(null);
        return passiveTerminal;
    }

    @Override
    public void notify(Terminal t, Synapse ts, BindingSignal bs) {
        if(t.getType() == INPUT) {
            Activation act = bs.getActivation();

            initTransitionEvent(
                    ts,
                    act,
                    t.getType().invert(),
                    bs.getOnArrived(),
                    getPassiveTerminal(t, ts, act).getBSEvent(act)
            );
        } else {
            link(ts, bs.getOnArrived(), INPUT);
        }
    }

    @Override
    public void registerTransitionEvent(FixedTerminal t, Synapse ts, Activation act, FieldOutput bsEvent) {
        initTransitionEvent(
                ts,
                act,
                t.getType().invert(),
                bsEvent,
                getPassiveTerminal(t, ts, act).getBSEvent(act)
        );
    }

    private void initTransitionEvent(Synapse ts, Activation act, Direction dir, FieldOutput activeBSEvent, FieldOutput passiveBSEvent) {
        FieldOutput inputEvent = mul(
                "input bi transition event",
                activeBSEvent,
                passiveBSEvent
        );

        FieldOutput transitionEvent = getTransitionEvent(ts, act, dir, inputEvent);
        transitionEvent.addEventListener(() ->
                link(ts, activeBSEvent, dir)
        );
    }

    @Override
    public Stream<SingleTransition> getBSPropagateTransitions(State s) {
        return Stream.concat(
                activeTransition.getBSPropagateTransitions(s),
                passiveTransition.getBSPropagateTransitions(s)
        );
    }

    @Override
    public TransitionMode getMode() {
        return MATCH_ONLY;
    }

    protected void link(Synapse ts, FieldOutput activeBSEvent, Direction dir) {
        Terminal activeTerminal = dir.getFromTerminal(activeTransition);
        BindingSignal activeBS = activeTerminal.getBindingSignal(activeBSEvent);

        FixedTerminal passiveTerminal = (FixedTerminal) dir.getTerminal(passiveTransition);
        BindingSignal passiveBS = passiveTerminal.getBindingSignal(activeBS.getActivation());

        link(activeTransition, ts, activeBS, passiveBS, dir);
        link(passiveTransition, ts, passiveBS, activeBS, dir);

        activeTransition.propagate(ts, activeBS, dir);
        passiveTransition.propagate(ts, passiveBS, dir);
    }

    private void link(SingleTransition t, Synapse ts, BindingSignal fromBS, BindingSignal relatedFromBindingsSignal, Direction dir) {
        if(fromBS == null)
            return;

        Stream<BindingSignal<?>> bsStream = ts.getRelatedBindingSignal(fromBS, dir);

        bsStream
                .filter(toBS -> fromBS != toBS)
                .filter(toBS -> checkRelated(
                                getRelatedTransition(t),
                                relatedFromBindingsSignal,
                                toBS.getActivation(),
                                dir
                        )
                )
                .forEach(toBS ->
                        t.link(ts, fromBS, toBS, dir)
                );
    }

    private SingleTransition getRelatedTransition(SingleTransition t) {
        return t == activeTransition ?
                passiveTransition :
                activeTransition;
    }

    private static boolean checkRelated(SingleTransition relTransition, BindingSignal relFromBS, Activation toAct, Direction dir) {
        BindingSignal relToBS = toAct.getBindingSignal(dir.getFromTerminal(relTransition).getState());

        if(relToBS == null)
            return dir == OUTPUT ;

        if(relFromBS == null)
            return dir == INPUT;

        return relFromBS.getOriginActivation() == relToBS.getOriginActivation();
    }

    public String toString() {
        return "BiTr: active:<" + activeTransition + ">  passive:<" + passiveTransition + ">";
    }
}
