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
import static network.aika.neuron.bindingsignal.SingleTransition.*;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_ONLY;


/**
 * @author Lukas Molzberger
 */
public class BiTransition implements Transition {

    protected BiTerminal inputTerminal;

    protected SingleTransition<FixedTerminal, ?> activeTransition;
    protected SingleTransition<FixedTerminal, FixedTerminal> passiveTransition;

    protected BiTransition(SingleTransition<FixedTerminal, ?> activeTransition, SingleTransition<FixedTerminal, FixedTerminal> passiveTransition) {
        this.activeTransition = activeTransition;
        this.passiveTransition = passiveTransition;

        inputTerminal = new BiTerminal(this, activeTransition.getInput(), passiveTransition.getInput());
    }

    public static BiTransition biTransition(SingleTransition activeTransition, SingleTransition passiveTransition) {
        return new BiTransition(activeTransition, passiveTransition);
    }

    @Override
    public Stream<Terminal> getInputTerminals() {
        return Stream.of(inputTerminal);
    }

    @Override
    public Stream<SingleTerminal> getOutputTerminals() {
        return Stream.of(activeTransition.getOutput(), passiveTransition.getOutput());
    }

    @Override
    public TransitionMode getMode() {
        return MATCH_ONLY;
    }

    public void linkAndPropagate(Synapse ts, FieldOutput activeBSEvent, Direction dir) {
        SingleTerminal activeTerminal = dir.getFromTerminal(activeTransition);
        BindingSignal activeBS = activeTerminal.getBindingSignal(activeBSEvent);

        FixedTerminal passiveTerminal = (FixedTerminal) dir.getFromTerminal(passiveTransition);
        BindingSignal passiveBS = passiveTerminal.getBindingSignal(activeBS.getActivation());

        biLink(activeTransition, ts, activeBS, passiveBS, dir);
        biLink(passiveTransition, ts, passiveBS, activeBS, dir);

        latentLinking(activeTransition, ts, activeBS, dir);
        latentLinking(passiveTransition, ts, passiveBS, dir);

        propagate(activeTransition, ts, activeBS, dir);
        propagate(passiveTransition, ts, passiveBS, dir);
    }

    private void biLink(SingleTransition t, Synapse ts, BindingSignal fromBS, BindingSignal relatedFromBindingSignal, Direction dir) {
        if(fromBS == null)
            return;

        Stream<BindingSignal<?>> bsStream = ts.getRelatedBindingSignals(fromBS, dir);

        bsStream
                .filter(toBS -> fromBS != toBS)
                .filter(toBS -> checkRelated(
                                getRelatedTransition(t),
                                relatedFromBindingSignal,
                                toBS.getActivation(),
                                dir
                        )
                )
                .forEach(toBS ->
                        link(t, ts, fromBS, toBS, dir)
                );
    }

    private SingleTransition getRelatedTransition(SingleTransition t) {
        return t == activeTransition ?
                passiveTransition :
                activeTransition;
    }

    private static boolean checkRelated(SingleTransition relTransition, BindingSignal relFromBS, Activation toAct, Direction dir) {
        BindingSignal relToBS = toAct.getBindingSignal(dir.getTerminal(relTransition).getState());

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
