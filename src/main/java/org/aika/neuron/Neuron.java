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
package org.aika.neuron;


import org.aika.ActivationFunction;
import org.aika.Model;
import org.aika.Provider;
import org.aika.ReadWriteLock;
import org.aika.corpus.Document;
import org.aika.lattice.InputNode;

import java.util.*;

/**
 * The {@code Neuron} class is a proxy implementation for the real neuron implementation in the class {@code INeuron}.
 * Aika uses the provider pattern to store and reload rarely used neurons or logic nodes.
 *
 * @author Lukas Molzberger
 */
public class Neuron extends Provider<INeuron> {

    public ReadWriteLock lock = new ReadWriteLock();

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
        return addInput(doc, begin, end, null);
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     * @param relationalId
     */
    public Activation addInput(Document doc, int begin, int end, Integer relationalId) {
        return addInput(doc,
                new Activation.Builder()
                        .setRange(begin, end)
                        .setRelationalId(relationalId)
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
        return init(n, bias, type, new TreeSet<>(Arrays.asList(inputs)));
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
        return init(doc, n, bias, null, type, new TreeSet<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Neuron n, double bias, String activationFunctionKey, INeuron.Type type, Synapse.Builder... inputs) {
        return init(n, bias, activationFunctionKey, type, new TreeSet<>(Arrays.asList(inputs)));
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Document doc, Neuron n, double bias, String activationFunctionKey, INeuron.Type type, Synapse.Builder... inputs) {
        return init(doc, n, bias, activationFunctionKey, type, new TreeSet<>(Arrays.asList(inputs)));
    }



    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Neuron n, double bias, INeuron.Type type, Collection<Synapse.Builder> inputs) {
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
    public static Neuron init(Neuron n, double bias, String activationFunctionKey, INeuron.Type type, Collection<Synapse.Builder> inputs) {
        if(n.init(bias, activationFunctionKey, type, inputs)) return n;
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
    public static Neuron init(Document doc, Neuron n, double bias, String activationFunctionKey, INeuron.Type type, Collection<Synapse.Builder> inputs) {
        if(n.init(doc, bias, activationFunctionKey, type, inputs)) return n;
        return null;
    }


    /**
     * Initializes a neuron with the given bias.
     *
     * @param bias
     * @param inputs
     * @return
     */
    public boolean init(double bias, String activationFunctionKey, INeuron.Type type, Collection<Synapse.Builder> inputs) {
        return init((Document) null, bias, activationFunctionKey, type, inputs);
    }


    public boolean init(Document doc, double bias, String activationFunctionKey, INeuron.Type type, Collection<Synapse.Builder> inputs) {
        List<Synapse> is = new ArrayList<>();

        for (Synapse.Builder input : inputs) {
            Synapse s = input.getSynapse(this);
            s.weightDelta = input.weight;
            s.setBias(input.bias);
            is.add(s);
        }

        if(activationFunctionKey != null) {
            ActivationFunction af = model.activationFunctions.get(activationFunctionKey);
            INeuron in = get();
            in.activationFunction = af;
            in.activationFunctionKey = activationFunctionKey;
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

        s.weightDelta = input.weight;
        s.setBias(input.bias);

        INeuron.update(model, doc != null ? doc.threadId : model.defaultThreadId, doc, this, 0.0, Collections.singletonList(s));
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

        if(!s.input.isSuspended()) {
            InputNode iNode = s.inputNode.get();
            if (iNode != null) {
                iNode.setSynapse(s);
            }
        }
    }

    public void removeInMemoryInputSynapse(Synapse s) {
        lock.acquireWriteLock();
        inMemoryInputSynapses.remove(s);
        lock.releaseWriteLock();

        if(!s.input.isSuspended()) {
            InputNode iNode = s.inputNode.getIfNotSuspended();
            if (iNode != null) {
                iNode.removeSynapse(s);
            }
        }
    }


    public void addInMemoryOutputSynapse(Synapse s) {
        lock.acquireWriteLock();
        inMemoryOutputSynapses.put(s, s);
        lock.releaseWriteLock();

        if(!s.output.isSuspended()) {
            InputNode iNode = s.inputNode.get();
            if (iNode != null) {
                iNode.setSynapse(s);
            }
        }
    }


    public void removeInMemoryOutputSynapse(Synapse s) {
        lock.acquireWriteLock();
        inMemoryOutputSynapses.remove(s);
        lock.releaseWriteLock();

        if(!s.output.isSuspended()) {
            InputNode iNode = s.inputNode.getIfNotSuspended();
            if (iNode != null) {
                iNode.setSynapse(s);
            }
        }
    }
}