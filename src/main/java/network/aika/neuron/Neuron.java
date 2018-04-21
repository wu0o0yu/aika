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


import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Linker;
import network.aika.ActivationFunction;
import network.aika.Model;
import network.aika.Provider;
import network.aika.ReadWriteLock;
import network.aika.Document;

import java.util.*;

/**
 * The {@code Neuron} class is a proxy implementation for the real neuron implementation in the class {@code INeuron}.
 * Aika uses the provider pattern to store and reload rarely used neurons or logic nodes.
 *
 * @author Lukas Molzberger
 */
public class Neuron extends Provider<INeuron> {

    public static final Neuron MIN_NEURON = new Neuron(null, Integer.MIN_VALUE);
    public static final Neuron MAX_NEURON = new Neuron(null, Integer.MAX_VALUE);


    public ReadWriteLock lock = new ReadWriteLock();

    public NavigableMap<Integer, Synapse> inputSynapsesById = new TreeMap<>();
    public NavigableMap<Synapse, Synapse> inMemoryInputSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);
    public NavigableMap<Synapse, Synapse> inMemoryOutputSynapses = new TreeMap<>(Synapse.OUTPUT_SYNAPSE_COMP);


    public Neuron(Model m, int id) {
        super(m, id);
    }

    public Neuron(Model m, INeuron n) {
        super(m, n);
    }


    public String getLabel() {
        return get().label;
    }


    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     */
    public Activation addInput(Document doc, int begin, int end) {
        return addInput(doc,
                new Activation.Builder()
                        .setRange(begin, end)
        );
    }


    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param inputAct
     */
    public Activation addInput(Document doc, Activation.Builder inputAct) {
        return get(doc).addInput(doc, inputAct);
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Neuron n, double bias, INeuron.Type type, Synapse.Builder... inputs) {
        return init(n, bias, type, new ArrayList<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Document doc, Neuron n, double bias, INeuron.Type type, Synapse.Builder... inputs) {
        return init(doc, n, bias, null, type, new ArrayList<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Neuron n, double bias, ActivationFunction activationFunction, INeuron.Type type, Synapse.Builder... inputs) {
        return init(n, bias, activationFunction, type, new ArrayList<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Document doc, Neuron n, double bias, ActivationFunction activationFunction, INeuron.Type type, Synapse.Builder... inputs) {
        return init(doc, n, bias, activationFunction, type, new ArrayList<>(Arrays.asList(inputs)));
    }



    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Neuron n, double bias, INeuron.Type type, List<Synapse.Builder> inputs) {
        return init(n, bias, null, type, inputs);
    }


    /**
     * Initializes a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Neuron n, double bias, ActivationFunction activationFunction, INeuron.Type type, List<Synapse.Builder> inputs) {
        if(n.init(bias, activationFunction, type, inputs)) return n;
        return null;
    }

    /**
     * Initializes a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Document doc, Neuron n, double bias, ActivationFunction activationFunction, INeuron.Type type, List<Synapse.Builder> inputs) {
        if(n.init(doc, bias, activationFunction, type, inputs)) return n;
        return null;
    }


    /**
     * Initializes a neuron with the given bias.
     *
     * @param bias
     * @param inputs
     * @return
     */
    public boolean init(double bias, ActivationFunction activationFunction, INeuron.Type type, List<Synapse.Builder> inputs) {
        return init((Document) null, bias, activationFunction, type, inputs);
    }


    public boolean init(Document doc, double bias, ActivationFunction activationFunction, INeuron.Type type, List<Synapse.Builder> inputs) {
        ArrayList<Synapse> is = new ArrayList<>();
        Map<Integer, Synapse> synapseIds = new TreeMap<>();

        for (Synapse.Builder input : inputs) {
            Synapse s = input.getSynapse(this);
            s.update(doc, input.weight, input.bias);
            if(input.synapseId != null) {
                synapseIds.put(input.synapseId, s);
            }
            is.add(s);
        }

        int i = 0;
        for (Synapse.Builder input : inputs) {
            Synapse s = is.get(i);
            for(Map.Entry<Integer, Relation> me: input.relations.entrySet()) {
                s.relations.put(synapseIds.get(me.getKey()), me.getValue());
            }
            i++;
        }


        if(activationFunction != null) {
            INeuron in = get();
            in.activationFunction = activationFunction;
        }

        if(type != null) {
            INeuron in = get();
            in.type = type;
        }

        return INeuron.update(model, model.defaultThreadId, doc, this, bias, is);
    }


    public void addSynapse(Synapse.Builder input) {
        addSynapse(null, input);
    }


    public void addSynapse(Document doc, Synapse.Builder input) {
        Synapse s = input.getSynapse(this);

        s.update(doc, input.weight, input.bias);

        INeuron.update(model, doc != null ? doc.threadId : model.defaultThreadId, doc, this, 0.0, Collections.singletonList(s));
    }



    public Synapse getSynapseById(int synapseId) {
        return inputSynapsesById.get(synapseId);
    }

    /**
     * {@code getFinalActivations} is a convenience method to retrieve all activations of the given neuron that
     * are part of the final interpretation. Before calling this method, the {@code doc.process()} needs to
     * be called first. {@code getFinalActivations} requires that the {@code doc.process()} method has been called first.
     *
     * @param doc The current document
     * @return A collection with all final activations of this neuron.
     */
    public Collection<Activation> getFinalActivations(Document doc) {
        INeuron n = getIfNotSuspended();
        if(n == null) return Collections.emptyList();
        return n.getFinalActivations(doc);
    }


    public void addInMemoryInputSynapse(Synapse s) {
        lock.acquireWriteLock();
        inMemoryInputSynapses.put(s, s);
        lock.releaseWriteLock();
    }


    public void removeInMemoryInputSynapse(Synapse s) {
        lock.acquireWriteLock();
        inMemoryInputSynapses.remove(s);
        lock.releaseWriteLock();
    }


    public void addInMemoryOutputSynapse(Synapse s) {
        lock.acquireWriteLock();
        inMemoryOutputSynapses.put(s, s);
        lock.releaseWriteLock();
    }


    public void removeInMemoryOutputSynapse(Synapse s) {
        lock.acquireWriteLock();
        inMemoryOutputSynapses.remove(s);
        lock.releaseWriteLock();
    }

    public String toString() {
        if(this == MIN_NEURON) return "MIN_NEURON";
        if(this == MAX_NEURON) return "MAX_NEURON";

        return super.toString();
    }

}