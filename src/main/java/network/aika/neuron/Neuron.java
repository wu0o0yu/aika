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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.Synapse.Builder;

import java.util.*;

/**
 * The {@code Neuron} class is a proxy implementation for the real neuron implementation in the class {@code INeuron}.
 * Aika uses the provider pattern to store and reload rarely used neurons or logic nodes.
 *
 * @author Lukas Molzberger
 */
public class Neuron extends Provider<INeuron<? extends Synapse>> {

    public static final Neuron MIN_NEURON = new Neuron(null, Integer.MIN_VALUE);
    public static final Neuron MAX_NEURON = new Neuron(null, Integer.MAX_VALUE);

    public Neuron(Model m, int id) {
        super(m, id);
    }

    public Neuron(Model m, INeuron n) {
        super(m, n);
    }

    public String getLabel() {
        return get().getLabel();
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param t   The current document
     * @param inputAct
     */
    public Activation propagateInput(Thought t, Activation.Builder inputAct) {
        return get().addInputActivation(t, inputAct);
    }

    public static Neuron init(INeuron<?> n, double bias, Builder... inputs) {
        return init(null, n, bias, inputs);
    }

    public static Neuron init(Thought t, INeuron<?> n, double bias, Builder... inputs) {
        n.init(t, bias, Arrays.asList(inputs));
        return n.getProvider();
    }

    public static Neuron init(Thought t, Neuron n, double bias, Builder... inputs) {
        return init(t, n, bias, inputs);
    }

    public String toString() {
        if(this == MIN_NEURON) return "MIN_NEURON";
        if(this == MAX_NEURON) return "MAX_NEURON";

        return super.toString();
    }
}