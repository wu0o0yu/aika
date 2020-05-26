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
package network.aika.neuron;

import network.aika.*;

import java.util.Collection;

/**
 * The {@code NeuronProvider} class is a proxy implementation for the real neuron implementation in the class {@code Neuron}.
 * Aika uses the provider pattern to store and reload rarely used neurons or logic nodes.
 *
 * @author Lukas Molzberger
 */
public class NeuronProvider extends Provider<Neuron<? extends Synapse>> {

    public static final NeuronProvider MIN_NEURON = new NeuronProvider(null, Long.MIN_VALUE);
    public static final NeuronProvider MAX_NEURON = new NeuronProvider(null, Long.MAX_VALUE);

    public NeuronProvider(Model m, long id) {
        super(m, id);
    }

    public NeuronProvider(Model m, Neuron n) {
        super(m, n);
    }

    public String getLabel() {
        return get().getLabel();
    }

    public void link(double bias, Synapse.Builder... inputs) {
        get().link(bias, inputs);
    }

    public void link(Double bias, Collection<Synapse.Builder> synapseBuilders) {
        get().link(bias, synapseBuilders);
    }

    public String toString() {
        if(this == MIN_NEURON) return "MIN_NEURON";
        if(this == MAX_NEURON) return "MAX_NEURON";

        return super.toString();
    }
}