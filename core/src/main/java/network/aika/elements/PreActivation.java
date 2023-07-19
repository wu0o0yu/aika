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
package network.aika.elements;


import network.aika.Thought;
import network.aika.elements.activations.Activation;
import network.aika.elements.neurons.NeuronProvider;
import network.aika.elements.synapses.Synapse;

import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class PreActivation<A extends Activation> {

    private SortedSet<A> activations = new TreeSet<>();
    private Set<Synapse> outputSynapses = new TreeSet<>(
            Comparator.comparingLong(s -> s.getPOutput().getId())
    );

    public PreActivation(Thought t, NeuronProvider provider) {
        t.register(provider, this);
    }

    public SortedSet<A> getActivations() {
        return activations;
    }

    public void addActivation(A act) {
        activations.add(act);
    }

    public void addOutputSynapse(Synapse s) {
        outputSynapses.add(s);
    }

    public void removeOutputSynapse(Synapse s) {
        outputSynapses.remove(s);
    }

    public Stream<Synapse> getOutputSynapses() {
        return outputSynapses.stream();
    }
}
