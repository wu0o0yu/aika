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
package network.aika.neuron.disjunctive;

import network.aika.direction.Direction;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.axons.Axon;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.Transition;


import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.fields.Fields.mul;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class DisjunctiveSynapse<
        S extends DisjunctiveSynapse,
        I extends Neuron & Axon,
        O extends DisjunctiveNeuron<?, OA>,
        L extends Link<S, IA, OA>,
        IA extends Activation<?>,
        OA extends DisjunctiveActivation
        > extends Synapse<S,I,O,L,IA,OA>
{

    public void linkAndPropagate(Transition t, Direction dir, BindingSignal... fromBSs) {
        if(isPropagate())
            propagate(fromBSs[0]);
    }

    @Override
    public Stream<Transition> getTransitions() {
        return Stream.empty();
    }

    @Override
    public boolean networkInputsAllowed(Direction dir) {
        return !isTemplate();
    }

    @Override
    public void setModified() {
        getInput().setModified();
    }

    @Override
    public double getSumOfLowerWeights() {
        return 0.0;
    }

    @Override
    public void linkOutput() {
        if(!isTemplate())
            return;

        super.linkOutput();
    }
}
