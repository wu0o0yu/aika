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
package network.aika.neuron.conjunctive;

import network.aika.Model;
import network.aika.neuron.ActivationFunction;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.ConjunctiveActivation;
import network.aika.neuron.bindingsignal.BindingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;

import static network.aika.neuron.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveNeuron<S extends ConjunctiveSynapse, A extends ConjunctiveActivation> extends Neuron<S, A> {

    private static final Logger log = LoggerFactory.getLogger(ConjunctiveNeuron.class);

    private boolean updateAllowPropagateIsQueued;

    public boolean getUpdateAllowPropagateIsQueued() {
        return updateAllowPropagateIsQueued;
    }

    public ConjunctiveNeuron() {
        super();
    }

    public ConjunctiveNeuron(NeuronProvider p) {
        super(p);
    }

    public ConjunctiveNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }

    @Override
    public void setModified() {
        super.setModified();
        updateAllowPropagateIsQueued = true;
    }

    protected void initFromTemplate(ConjunctiveNeuron n) {
        super.initFromTemplate(n);
    }

    public void addInactiveLinks(BindingSignal bs) {
        inputSynapses
                .stream()
                .filter(s -> !bs.getActivation().inputLinkExists(s))
                .forEach(s ->
                        s.createLink(null, bs.getActivation())
                );
    }

    public ActivationFunction getActivationFunction() {
        return RECTIFIED_HYPERBOLIC_TANGENT;
    }

    public void updateSumOfLowerWeights() {
        sortInputSynapses();

        double sum = getBias().getCurrentValue();
        for(ConjunctiveSynapse s: inputSynapses) {
            if(s.getWeight().getCurrentValue() <= 0.0)
                continue;

            s.setSumOfLowerWeights(sum);

            sum += s.getWeight().getCurrentValue();
        }

        updateAllowPropagateIsQueued = false;
    }

    private void sortInputSynapses() {
        Collections.sort(
                inputSynapses,
                Comparator.<ConjunctiveSynapse>comparingDouble(s -> s.getSortingWeight())
        );
    }
}
