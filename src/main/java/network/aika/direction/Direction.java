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
package network.aika.direction;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.bindingsignal.SingleTransition;
import network.aika.neuron.bindingsignal.SingleTerminal;
import network.aika.neuron.bindingsignal.Terminal;
import network.aika.neuron.bindingsignal.Transition;

import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public interface Direction {
    Direction INPUT = new Input();
    Direction OUTPUT = new Output();

    Direction[] DIRECTIONS = new Direction[] {INPUT, OUTPUT};

    Direction invert();

    <I> I getInput(I from, I to);

    <O> O getOutput(O from, O to);

    Neuron getNeuron(Synapse s);

    Activation getActivation(Link l);

    SingleTerminal getTerminal(SingleTransition t);

    SingleTerminal getFromTerminal(SingleTransition t);

    Stream<Link> getLinks(Activation act);

    Stream<? extends Synapse> getSynapses(Neuron n);

    Stream<Terminal> getTerminals(Transition t);

    static int compare(Direction a, Direction b) {
        if(a == b) return 0;
        if(a == null) return -1;
        if(b == null) return 1;
        if(a == INPUT && b == OUTPUT) return -1;
        if(a == OUTPUT && b == INPUT) return 1;
        throw new IllegalStateException();
    }
}
