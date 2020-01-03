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


    ReadWriteLock lock = new ReadWriteLock();

    NavigableMap<Integer, Synapse> inputSynapsesById = new TreeMap<>();
    NavigableMap<Neuron, Synapse> activeInputSynapses = new TreeMap<>();
    NavigableMap<Neuron, Synapse> activeOutputSynapses = new TreeMap<>();


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
     * @param doc   The current document
     * @param inputAct
     */
    public Activation addInput(Document doc, Activation.Builder inputAct) {
        return get(doc).addInput(doc, inputAct);
    }


    public static Neuron init(Neuron n, Builder... inputs) {
        return init(null, n, inputs);
    }


    public static Neuron init(Document doc, Neuron n, Builder... inputs) {
        n.init(doc, null, getSynapseBuilders(inputs));
        return n;
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Neuron n, double bias, Builder... inputs) {
        return init(n, bias, getSynapseBuilders(inputs));
    }


    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public static Neuron init(Document doc, Neuron n, double bias, Builder... inputs) {
        return init(doc, n, bias, getSynapseBuilders(inputs));
    }



    public static Neuron init(Neuron n, double bias, Collection<Synapse.Builder> inputs) {
        n.init((Document) null, bias, getSynapseBuilders(inputs));
        return n;
    }


    public static Neuron init(Document doc, Neuron n, double bias, Collection<Synapse.Builder> inputs) {
        n.init(doc, bias, getSynapseBuilders(inputs));
        return n;
    }


    private void init(Document doc, Double bias, Collection<Synapse.Builder> synapseBuilders) {
        INeuron n = get();

        if(bias != null) {
            n.setBias(bias);
        }

        ArrayList<Synapse> modifiedSynapses = new ArrayList<>();
        // s.link requires an updated n.biasSumDelta value.
        synapseBuilders.forEach(input -> {
            Synapse s = input.getSynapse(this);
            s.update(doc, input.weight);
            modifiedSynapses.add(s);
        });

        modifiedSynapses.forEach(s -> s.link());

        n.commit(modifiedSynapses);
    }


    public Synapse getSynapseById(int synapseId) {
        return inputSynapsesById.get(synapseId);
    }


    public Synapse getInputSynapse(Neuron inputNeuron) {
        lock.acquireReadLock();
        Synapse synapse = activeInputSynapses.get(inputNeuron);

        lock.releaseReadLock();
        return synapse;
    }


    void addActiveInputSynapse(Synapse s) {
        lock.acquireWriteLock();
        activeInputSynapses.put(s.getPInput(), s);
        inputSynapsesById.put(s.getId(), s);
        lock.releaseWriteLock();
    }


    void removeActiveInputSynapse(Synapse s) {
        lock.acquireWriteLock();
        activeInputSynapses.remove(s.getPInput());
        inputSynapsesById.remove(s.getId());
        lock.releaseWriteLock();
    }


    void addActiveOutputSynapse(Synapse s) {
        lock.acquireWriteLock();
        activeOutputSynapses.put(s.getPOutput(), s);
        lock.releaseWriteLock();
    }


    void removeActiveOutputSynapse(Synapse s) {
        lock.acquireWriteLock();
        activeOutputSynapses.remove(s.getPOutput());
        lock.releaseWriteLock();
    }


    public String toString() {
        if(this == MIN_NEURON) return "MIN_NEURON";
        if(this == MAX_NEURON) return "MAX_NEURON";

        return super.toString();
    }


    public int getNewSynapseId() {
        return get().getNewSynapseId();
    }


    public void registerSynapseId(int synId) {
        get().registerSynapseId(synId);
    }


    /**
     * Active input synapses are those synapses that are currently available in the main memory.
     *
     * @return
     */
    public Collection<Synapse> getActiveInputSynapses() {
        return activeInputSynapses.values();
    }


    public Collection<Synapse> getActiveOutputSynapses() {
        return activeOutputSynapses.values();
    }


    private static Collection<Synapse.Builder> getSynapseBuilders(Collection<Synapse.Builder> builders) {
        ArrayList<Synapse.Builder> result = new ArrayList<>();
        for(Builder b: builders) {
            if(b instanceof Synapse.Builder) {
                result.add((Synapse.Builder) b);
            }
        }
        return result;
    }


    private static Collection<Synapse.Builder> getSynapseBuilders(Builder... builders) {
        ArrayList<Synapse.Builder> result = new ArrayList<>();
        for(Builder b: builders) {
            if(b instanceof Synapse.Builder) {
                result.add((Synapse.Builder) b);
            }
        }
        return result;
    }


    public interface Builder {
        void registerSynapseIds(Neuron n);
    }

}