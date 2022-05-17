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
package network.aika.neuron.conjunctive;

import network.aika.Model;
import network.aika.direction.Direction;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.SamePatternLink;
import network.aika.neuron.bindingsignal.BiTransition;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.BiTransition.biTransition;

/**
 * The Same Pattern Binding Neuron Synapse is an inner synapse between two binding neurons of the same pattern.
 *
 * @author Lukas Molzberger
 */
public class SamePatternSynapse extends BindingNeuronSynapse<SamePatternSynapse, BindingNeuron, SamePatternLink, BindingActivation> {

    private static BiTransition sameTransition = (BiTransition) biTransition(State.SAME, State.SAME);

    private static BiTransition inputTransition = (BiTransition) biTransition(State.INPUT, State.INPUT);

    private static List<Transition> TRANSITIONS = List.of(
            sameTransition, // Same Pattern BindingSignal
            inputTransition // Input BS becomes related
/* Loose Linking
            transition(State.INPUT, State.INPUT)
                    .setCheck(true)
                    .setCheckBoundToSamePattern(true)
                    .setCheckLooseLinking(true)
                    .setPropagateBS(true), // Input BS becomes related
*/
    );

    static {
        BiTransition.link(sameTransition, inputTransition);
    }

    private int looseLinkingRange;
    private boolean allowLooseLinking;

    @Override
    public SamePatternLink createLink(BindingActivation input, BindingActivation output) {
        return new SamePatternLink(this, input, output);
    }

    @Override
    protected double getSortingWeight() {
        if(allowLooseLinking)
            return 0.0;

        return super.getSortingWeight();
    }

    public void setLooseLinkingRange(int looseLinkingRange) {
        this.looseLinkingRange = looseLinkingRange;
    }

    public Integer getLooseLinkingRange() {
        return looseLinkingRange;
    }

    public void setAllowLooseLinking(boolean allowLooseLinking) {
        this.allowLooseLinking = allowLooseLinking;
    }

    public boolean allowLooseLinking() {
        return allowLooseLinking;
    }

    @Override
    public boolean networkInputsAllowed(Direction dir) {
        return !isTemplate();
    }

    @Override
    public Stream<Transition> getTransitions() {
        return TRANSITIONS.stream();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeBoolean(allowLooseLinking);
        out.writeInt(looseLinkingRange);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        allowLooseLinking = in.readBoolean();
        looseLinkingRange = in.readInt();
    }
}
