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
package network.aika.elements.neurons;

import network.aika.Model;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.ConjunctiveActivation;
import network.aika.elements.synapses.CategorySynapse;
import network.aika.elements.synapses.CategoryInputSynapse;
import network.aika.elements.synapses.ConjunctiveSynapse;
import network.aika.elements.synapses.Synapse;
import network.aika.fields.QueueSumField;
import network.aika.fields.SumField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.steps.Phase.TRAINING;
import static network.aika.utils.Utils.TOLERANCE;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveNeuron<S extends ConjunctiveSynapse, A extends ConjunctiveActivation> extends Neuron<S, A> {

    private static final Logger log = LoggerFactory.getLogger(ConjunctiveNeuron.class);

    protected SumField synapseBiasSum = initSynapseBiasSum();

    public ConjunctiveNeuron() {
        bias.addEventListener(
                "onBiasUpdate",
                this::updateSumOfLowerWeights,
                true
        );
        synapseBiasSum.addEventListener(
                "onSynapseBiasSumUpdate",
                this::updateSumOfLowerWeights,
                true
        );
    }

    protected SumField initSynapseBiasSum() {
        return (SumField) new QueueSumField(this, TRAINING, "synapseBiasSum", TOLERANCE)
                .addListener("onSynapseBiasSumModified", () ->
                        setModified()
                );
    }

    public SumField getSynapseBiasSum() {
        return synapseBiasSum;
    }

    @Override
    public void reactivate(Model m) {
        super.reactivate(m);

        getProvider().getInputSynapses()
                .forEach(Synapse::linkFields);
    }

    @Override
    public double getCurrentCompleteBias() {
        return getBias().getUpdatedCurrentValue() +
                synapseBiasSum.getUpdatedCurrentValue();
    }

    @Override
    protected void initFromTemplate(Neuron templateN) {
        super.initFromTemplate(templateN);

        ConjunctiveNeuron templateCN = (ConjunctiveNeuron)templateN;

        synapseBiasSum.setInitialValue(templateCN.getSynapseBiasSum().getUpdatedCurrentValue());

        CategoryInputSynapse cis = templateCN.getCategoryInputSynapse();
        newCategorySynapse()
                .setWeight(10.0)
                .init(this, cis.getInput());
    }

    public abstract CategorySynapse newCategorySynapse();

    @Override
    public void setModified() {
        super.setModified();
    }

    public boolean isAbstract() {
        return getCategoryInputSynapse() != null;
    }

    public boolean isInstanceOf(ConjunctiveNeuron templateNeuron) {
        CategorySynapse<?,?,?,?,?> cs = getCategoryOutputSynapse();
        if(cs == null)
            return false;

        CategoryInputSynapse cis = cs.getOutput().getOutgoingCategoryInputSynapse();
        if(cis == null)
            return false;

        return cis.getOutput().getId() == templateNeuron.getId();
    }

    public abstract CategoryInputSynapse getCategoryInputSynapse();

    public abstract CategorySynapse getCategoryOutputSynapse();

    public void addInactiveLinks(Activation bs) {
        getInputSynapsesAsStream()
                .filter(s -> !s.linkExists(bs))
                .forEach(s ->
                        s.createAndInitLink(null, bs)
                );
    }

    public ActivationFunction getActivationFunction() {
        return ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
    }

    protected void updateSumOfLowerWeights() {
        ConjunctiveSynapse[] inputSynapses = sortInputSynapses();

        double sum = bias.getUpdatedCurrentValue();
        for(ConjunctiveSynapse s: inputSynapses) {
            double w = s.getWeight().getUpdatedCurrentValue();
            if(w <= 0.0)
                continue;

            s.setSumOfLowerWeights(sum);
            sum += w;

            s.setStoredAt(
                    sum < 0 ?
                            OUTPUT :
                            INPUT
            );
        }
    }

    @Override
    public void addInputSynapse(S s) {
        super.addInputSynapse(s);
        s.getWeight().addEventListener(
                "onWeightUpdate",
                this::updateSumOfLowerWeights,
                true
        );
    }

    private ConjunctiveSynapse[] sortInputSynapses() {
        ConjunctiveSynapse[] inputsSynapses = getInputSynapses().toArray(new ConjunctiveSynapse[0]);
        Arrays.sort(
                inputsSynapses,
                Comparator.comparingDouble(s -> s.getSortingWeight())
        );
        return inputsSynapses;
    }
}
