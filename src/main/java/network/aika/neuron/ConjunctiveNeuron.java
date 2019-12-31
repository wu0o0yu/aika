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


import network.aika.ActivationFunction;
import network.aika.Model;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import static network.aika.neuron.Synapse.INPUT_SYNAPSE_COMP;
import static network.aika.neuron.Synapse.State.CURRENT;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveNeuron<S extends TSynapse> extends TNeuron<S> {

    private InhibitoryNeuron inhibitoryNeuron;

    private volatile double directConjunctiveBias;
    private volatile double recurrentConjunctiveBias;


    public ConjunctiveNeuron() {
        super();
    }


    public ConjunctiveNeuron(Neuron p) {
        super(p);
    }

    public ConjunctiveNeuron(Model model, String label) {
        super(model, label);
    }



    public InhibitoryNeuron getInhibitoryNeuron() {
        return inhibitoryNeuron;
    }


    public void setInhibitoryNeuron(InhibitoryNeuron inhibitoryNeuron) {
        this.inhibitoryNeuron = inhibitoryNeuron;
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


    public double getTotalBias(Synapse.State state) {
        return getBias(state) - getConjunctiveBias();
    }


    public double getConjunctiveBias() {
        return directConjunctiveBias + recurrentConjunctiveBias;
    }


    public void commit(Collection<? extends Synapse> modifiedSynapses) {
        commitBias();

        directConjunctiveBias = 0.0;
        recurrentConjunctiveBias = 0.0;
        for (Synapse s : inputSynapses.values()) {
            s.commit();

            if(!s.isNegative(CURRENT)) {
                if(!s.isRecurrent()) {
                    directConjunctiveBias += s.getWeight();
                } else  {
                    recurrentConjunctiveBias += s.getWeight();
                }
            }
        }

        TreeSet<Synapse> sortedSynapses = new TreeSet<>(
                Comparator.<Synapse>comparingDouble(s -> s.getWeight()).reversed()
                        .thenComparing(INPUT_SYNAPSE_COMP)
        );

        sortedSynapses.addAll(inputSynapses.values());

        double sum = getBias(CURRENT);
        for(Synapse s: sortedSynapses) {
            if(!s.isRecurrent() && !s.isNegative(CURRENT)) {
                s.setPropagate(sum > 0.0);

                sum -= s.getWeight();
            }
        }

        setModified();
    }
}
