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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.Range;

import network.aika.neuron.bindingsignal.BindingSignal;

import java.util.stream.Stream;

import static network.aika.neuron.activation.Timestamp.MAX;


/**
 * The dummy activation is just used to add neurons to the event queue.
 *
 * @author Lukas Molzberger
 */
public class DummyActivation extends Activation {

    public DummyActivation(Neuron neuron) {
        super(0, neuron);
        thought = getModel().getCurrentThought();
    }

    public DummyActivation(int id, Neuron neuron) {
        super(id, neuron);
        thought = getModel().getCurrentThought();
    }

    @Override
    public Timestamp getFired() {
        return MAX;
    }

    @Override
    public Range getRange() {
        return null;
    }

    @Override
    public Stream<? extends BindingSignal<?>> getReverseBindingSignals(Neuron toNeuron) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean isInput() {
        return false;
    }

    @Override
    public int compareTo(Object o) {
        throw new NoSuchMethodError();
    }

    public String toString() {
        return "DummyAct - neuron: " + neuron;
    }
}
