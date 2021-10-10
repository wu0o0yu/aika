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
package network.aika.neuron.activation.direction;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingSignal;
import network.aika.neuron.activation.Link;

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
    public Direction combine(Direction dir) {
        return dir.invert();
    }

    public Byte transitionScope(Byte fromScope) {
        if(fromScope >= 2)
            return null;

        return ++fromScope;
    }

    @Override
    public BindingSignal getInputBindingSignal(BindingSignal fromBindingSignal, BindingSignal toBindingSignal) {
        return fromBindingSignal;
    }

    @Override
    public BindingSignal getOutputBindingSignal(BindingSignal fromBindingSignal, BindingSignal toBindingSignal) {
        return toBindingSignal;
    }

    @Override
    public Activation getInput(Activation fromAct, Activation toAct) {
        return fromAct;
    }

    @Override
    public Activation getOutput(Activation fromAct, Activation toAct) {
        return toAct;
    }

    @Override
    public Neuron getNeuron(Synapse s) {
        return s.getOutput();
    }

    public Activation getActivation(Link l) {
        return l.getOutput();
    }

    public Stream<Link> getLinks(Activation act) {
        return act.getOutputLinks();
    }

    @Override
    public Stream<? extends Synapse> getSynapses(Neuron n) {
        return n.getOutputSynapses();
    }

    public String toString() {
        return "OUTPUT";
    }
}
