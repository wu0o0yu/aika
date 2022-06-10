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
public class Output implements Direction {

    @Override
    public Direction invert() {
        return INPUT;
    }

    @Override
    public <I> I getInput(I from, I to) {
        return from;
    }

    @Override
    public <O> O getOutput(O from, O to) {
        return to;
    }

    @Override
    public Neuron getNeuron(Synapse s) {
        return s.getOutput();
    }

    @Override
    public Activation getActivation(Link l) {
        return l.getOutput();
    }

    @Override
    public SingleTerminal getTerminal(SingleTransition t) {
        return t.getOutput();
    }

    @Override
    public SingleTerminal getFromTerminal(SingleTransition t) {
        return t.getInput();
    }

    @Override
    public Stream<Link> getLinks(Activation act) {
        return act.getOutputLinks();
    }

    @Override
    public Stream<? extends Synapse> getSynapses(Neuron n) {
        return n.getOutputSynapses();
    }

    @Override
    public Stream<? extends Terminal> getTerminals(Transition t) {
        return t.getOutputTerminals();
    }

    public String toString() {
        return "OUTPUT";
    }
}
