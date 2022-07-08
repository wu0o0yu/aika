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
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;

import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.LatentLinking.latentLinking;
import static network.aika.neuron.bindingsignal.PrimitiveTransition.*;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_ONLY;


/**
 * @author Lukas Molzberger
 */
public class BiTransition implements Transition {

    protected BiTerminal inputTerminal;
    protected BiTerminal outputTerminal;

    protected PrimitiveTransition<FixedTerminal, ?> activeTransition;
    protected PrimitiveTransition<FixedTerminal, FixedTerminal> passiveTransition;

    protected BiTransition(PrimitiveTransition<FixedTerminal, ?> activeTransition, PrimitiveTransition<FixedTerminal, FixedTerminal> passiveTransition) {
        this.activeTransition = activeTransition;
        this.passiveTransition = passiveTransition;

        inputTerminal = new FixedBiTerminal(INPUT, this, activeTransition.getInput(), passiveTransition.getInput());
        outputTerminal = BiTerminal.biTerminal(OUTPUT, this, activeTransition.getOutput(), passiveTransition.getOutput());
    }

    public static BiTransition biTransition(PrimitiveTransition activeTransition, PrimitiveTransition passiveTransition) {
        return new BiTransition(activeTransition, passiveTransition);
    }

    @Override
    public Stream<Terminal> getInputTerminals() {
        return Stream.of(inputTerminal);
    }

    @Override
    public Stream<Terminal> getOutputTerminals() {
        return Stream.of(outputTerminal);
    }

    @Override
    public TransitionMode getMode() {
        return MATCH_ONLY;
    }

    public void linkAndPropagate(Synapse ts, BindingSignal activeBS, Direction dir) {
        FixedTerminal passiveTerminal = (FixedTerminal) dir.getFromTerminal(passiveTransition);
        BindingSignal passiveBS = passiveTerminal.getBindingSignal(activeBS.getActivation());

        biLink(activeTransition, ts, activeBS, passiveBS, dir);
        biLink(passiveTransition, ts, passiveBS, activeBS, dir);

        if (dir != OUTPUT)
            return;

        latentLinking(activeTransition, ts, activeBS);
        latentLinking(passiveTransition, ts, passiveBS);

        propagate(activeTransition, ts, activeBS);
        propagate(passiveTransition, ts, passiveBS);
    }

    private void biLink(PrimitiveTransition t, Synapse ts, BindingSignal fromBS, BindingSignal relatedFromBindingSignal, Direction dir) {
        if(fromBS == null)
            return;

        Stream<BindingSignal> bsStream = ts.getRelatedBindingSignals(fromBS.getOriginActivation(), t, dir);

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

    private PrimitiveTransition getRelatedTransition(PrimitiveTransition t) {
        return t == activeTransition ?
                passiveTransition :
                activeTransition;
    }

    private static boolean checkRelated(PrimitiveTransition relTransition, BindingSignal relFromBS, Activation toAct, Direction dir) {
        State relToState = dir.getTerminal(relTransition).getState();
        BindingSignal relToBS = toAct.getBindingSignal(relToState);

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
