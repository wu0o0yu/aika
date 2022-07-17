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

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_ONLY;


/**
 * @author Lukas Molzberger
 */
public class BiTransition implements Transition {

    protected List<Terminal> inputTerminals;
    protected List<Terminal> outputTerminals;

    protected PrimitiveTransition<FixedTerminal, ?> firstTransition;
    protected PrimitiveTransition<FixedTerminal, FixedTerminal> secondTransition;

    protected BiTransition(PrimitiveTransition<FixedTerminal, ?> firstTransition, PrimitiveTransition<FixedTerminal, FixedTerminal> secondTransition, boolean inputBiTerminal, boolean outputBiTerminal) {
        this.firstTransition = firstTransition;
        this.secondTransition = secondTransition;

        if(inputBiTerminal)
            inputTerminals = List.of(new FixedBiTerminal(INPUT, this, firstTransition.getInput(), secondTransition.getInput()));
        else
            inputTerminals = List.of(new OptionalFixedBiTerminal(INPUT, this, firstTransition.getInput(), secondTransition.getInput()));

        if(outputBiTerminal)
            outputTerminals = List.of(BiTerminal.biTerminal(OUTPUT, this, firstTransition.getOutput(), secondTransition.getOutput()));
        else
            outputTerminals = List.of(BiTerminal.optionalBiTerminal(OUTPUT, this, firstTransition.getOutput(), secondTransition.getOutput()));
    }

    public static BiTransition biTransition(PrimitiveTransition firstTransition, PrimitiveTransition secondTransition, boolean inputBiTerminal, boolean outputBiTerminal) {
        return new BiTransition(firstTransition, secondTransition, inputBiTerminal, outputBiTerminal);
    }

    @Override
    public Stream<Terminal> getInputTerminals() {
        return inputTerminals.stream();
    }

    @Override
    public Stream<Terminal> getOutputTerminals() {
        return outputTerminals.stream();
    }

    @Override
    public TransitionMode getMode() {
        return MATCH_ONLY;
    }

    @Override
    public void link(Synapse ts, Direction dir, BindingSignal... fromBS) {
        link(firstTransition, ts, fromBS[0], fromBS[1], dir);
        link(secondTransition, ts, fromBS[1], fromBS[0], dir);
    }

    @Override
    public void latentLinking(Synapse ts, BindingSignal... fromBS) {
        LatentLinking.latentLinking(firstTransition, ts, fromBS[0]);
        LatentLinking.latentLinking(secondTransition, ts, fromBS[1]);
    }

    private void link(PrimitiveTransition t, Synapse ts, BindingSignal fromBS, BindingSignal relatedFromBindingSignal, Direction dir) {
        if(fromBS == null)
            return;

        Predicate<BindingSignal> biCheck = toBS ->
                checkRelated(
                        getRelatedTransition(t),
                        relatedFromBindingSignal,
                        toBS.getActivation(),
                        dir
                );

        t.link(ts, dir, biCheck, fromBS);
    }


    private PrimitiveTransition getRelatedTransition(PrimitiveTransition t) {
        return t == firstTransition ?
                secondTransition :
                firstTransition;
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
        return "BiTr: active:<" + firstTransition + ">  passive:<" + secondTransition + ">";
    }
}
