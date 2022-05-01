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

import network.aika.Model;
import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.neuron.ActivationFunction;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.InhibitoryActivation;
import network.aika.neuron.bindingsignal.BindingSignal;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class DisjunctiveNeuron<S extends DisjunctiveSynapse, A extends Activation> extends Neuron<S, A> {

    public DisjunctiveNeuron() {
        super();
    }

    public DisjunctiveNeuron(NeuronProvider p) {
        super(p);
    }

    protected DisjunctiveNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }

    public Stream<? extends Synapse> getTargetSynapses(Direction dir, boolean template) {
        if(dir == INPUT)
            return Stream.empty();

        return super.getTargetSynapses(dir, template);
    }

    @Override
    public void addInactiveLinks(BindingSignal bs) {
    }

    public ActivationFunction getActivationFunction() {
        return ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT;
    }
}
