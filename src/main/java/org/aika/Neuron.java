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
package org.aika;


import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.lattice.InputNode;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;

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

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     */
    public Activation addInput(Document doc, int begin, int end) {
        return addInput(doc, begin, end, null, doc.bottom);
    }


    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     * @param value The activation value of this input activation
     * @param targetValue The target activation value for supervised learning
     */
    public Activation addInput(Document doc, int begin, int end, double value, double targetValue) {
        return addInput(doc, begin, end, null, doc.bottom, value, targetValue);
    }


    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     * @param o     The interpretation node
     */
    public Activation addInput(Document doc, int begin, int end, InterprNode o) {
        return addInput(doc, begin, end, null, o);
    }


    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     * @param rid   The relational id (e.g. the word position)
     */
    public Activation addInput(Document doc, int begin, int end, Integer rid) {
        return addInput(doc, begin, end, rid, doc.bottom);
    }


    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     * @param rid   The relational id (e.g. the word position)
     * @param o     The interpretation node
     */
    public Activation addInput(Document doc, int begin, int end, Integer rid, InterprNode o) {
        return addInput(doc, begin, end, rid, o, 1.0);
    }


    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     * @param rid   The relational id (e.g. the word position)
     * @param o     The interpretation node
     * @param value The activation value of this input activation
     */
    public Activation addInput(Document doc, int begin, int end, Integer rid, InterprNode o, double value) {
        return addInput(doc, begin, end, rid, o, value, 0.0);
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param begin The range begin
     * @param end   The range end
     * @param rid   The relational id (e.g. the word position)
     * @param o     The interpretation node
     * @param value The activation value of this input activation
     * @param targetValue The target activation value for supervised learning
     */
    public Activation addInput(Document doc, int begin, int end, Integer rid, InterprNode o, double value, double targetValue) {
        return get().addInput(doc, begin, end, rid, o, value, targetValue);
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