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

import network.aika.neuron.ActivationFunction;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.ConjunctiveActivation;
import network.aika.neuron.disjunctive.CategorySynapse;
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

    public ConjunctiveNeuron() {
        bias.addEventListener(this::updateSumOfLowerWeights);
    }

    @Override
    protected void initFromTemplate(Neuron n) {
        super.initFromTemplate(n);

        S cis = (S) getCategoryInputSynapse();
        newCategorySynapse()
                .init(n, cis.getInput(), 1.0);
    }

    public abstract CategorySynapse newCategorySynapse();

    @Override
    public void setModified() {
        super.setModified();
    }

    public boolean isAbstract() {
        return getCategoryInputSynapse() != null;
    }

    public abstract CategoryInputSynapse getCategoryInputSynapse();

    public void addInactiveLinks(Activation bs) {
        inputSynapses
                .stream()
                .filter(s -> !s.linkExists(bs))
                .forEach(s ->
                        s.createLink(null, bs)
                );
    }

    public ActivationFunction getActivationFunction() {
        return RECTIFIED_HYPERBOLIC_TANGENT;
    }

    protected void updateSumOfLowerWeights() {
        sortInputSynapses();

        double sum = getBias().getNewValue();
        for(ConjunctiveSynapse s: inputSynapses) {
            if(s.getWeight().getCurrentValue() <= 0.0)
                continue;

            s.setSumOfLowerWeights(sum);

            sum += s.getWeight().getCurrentValue();
        }
    }

    @Override
    public void addInputSynapse(S s) {
        super.addInputSynapse(s);
        s.getWeight().addEventListener(this::updateSumOfLowerWeights);
    }

    @Override
    public void addOutputSynapse(Synapse s) {
        super.addOutputSynapse(s);
        sortOutputSynapses();
    }

    private void sortInputSynapses() {
        Collections.sort(
                inputSynapses,
                Comparator.<ConjunctiveSynapse>comparingDouble(s -> s.getSortingWeight())
        );
    }

    private void sortOutputSynapses() {
        Collections.sort(
                outputSynapses,
                Comparator.comparingInt(s -> -s.getRank())
        );
    }
}
