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
package network.aika.neuron.excitatory;


import network.aika.ActivationFunction;
import network.aika.Model;
import network.aika.neuron.InputKey;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.activation.Fired;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

import static network.aika.neuron.Synapse.INPUT_COMP;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveNeuron<S extends Synapse> extends TNeuron<S> {

    private volatile double directConjunctiveBias;
    private volatile double recurrentConjunctiveBias;

    protected TreeMap<InputKey, S> inputSynapses = new TreeMap<>(INPUT_COMP);


    public ConjunctiveNeuron() {
        super();
    }

    public ConjunctiveNeuron(Neuron p) {
        super(p);
    }

    public ConjunctiveNeuron(Model model, String label) {
        super(model, label);
    }

    @Override
    public void suspend() {
        for (Synapse s : inputSynapses.values()) {
            s.getPInput().removeActiveOutputSynapse(s);
        }

        for (Synapse s : outputSynapses.values()) {
            s.getPOutput().removeActiveInputSynapse(s);
        }
    }

    @Override
    public void reactivate() {
    }

    public void addInputSynapse(S s) {
        inputSynapses.put(s, s);
        setModified();
    }

    public void removeInputSynapse(S s) {
        if(inputSynapses.remove(s) != null) {
            setModified();
        }
    }

    public void addOutputSynapse(Synapse s) {
        outputSynapses.put(s, s);
        setModified();
    }

    public void removeOutputSynapse(Synapse s) {
        if(outputSynapses.remove(s) != null) {
            setModified();
        }
    }

    public Collection<S> getInputSynapses() {
        return inputSynapses.values();
    }

    public ActivationFunction getActivationFunction() {
        return ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
    }

    @Override
    public Fired incrementFired(Fired f) {
        return new Fired(f.getInputTimestamp(), f.getFired() + 1);
    }

    public boolean isWeak(Synapse s, Synapse.State state) {
        return s.getWeight(state) < getBias();
    }

    public double getTotalBias(boolean initialRound, Synapse.State state) {
        return getBias(state) - (directConjunctiveBias + (initialRound ? 0.0 : recurrentConjunctiveBias));
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        for (Synapse s : inputSynapses.values()) {
            if (s.getInput() != null) {
                out.writeBoolean(true);
                getModel().writeSynapse(s, out);
            }
        }
        out.writeBoolean(false);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        while (in.readBoolean()) {
            S syn = (S) m.readSynapse(in);
            inputSynapses.put(syn, syn);
        }
    }

    public void commit(Collection<? extends Synapse> modifiedSynapses) {
        commitBias();

        directConjunctiveBias = 0.0;
        recurrentConjunctiveBias = 0.0;
        for (Synapse s : inputSynapses.values()) {
            s.commit();

            if(!s.isNegative()) {
                if(!s.isRecurrent()) {
                    directConjunctiveBias += s.getWeight();
                } else  {
                    recurrentConjunctiveBias += s.getWeight();
                }
            }
        }

        setModified();
    }
}
