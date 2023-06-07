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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Field.FIRST_ROUND;
import static network.aika.steps.Phase.TRAINING;
import static network.aika.utils.Utils.TOLERANCE;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveNeuron<A extends ConjunctiveActivation> extends Neuron<A> {

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
        QueueSumField synBiasSum = (QueueSumField) new QueueSumField(this, TRAINING, "synapseBiasSum", 1, TOLERANCE, true)
                .addListener("onSynapseBiasSumModified", () ->
                        setModified()
                );
        synBiasSum.setInitialValue(FIRST_ROUND, 0.0);
        return synBiasSum;
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
        return getBias().getUpdatedValue(FIRST_ROUND) +
                synapseBiasSum.getUpdatedValue(FIRST_ROUND);
    }

    @Override
    protected void initFromTemplate(Neuron templateN) {
        super.initFromTemplate(templateN);

        synapseBiasSum.setInitialValue(
                FIRST_ROUND,
                ((ConjunctiveNeuron)templateN).getSynapseBiasSum().getUpdatedValue(FIRST_ROUND)
        );
    }

    public boolean isInstanceOf(ConjunctiveNeuron templateNeuron) {
        CategorySynapse<?,?,?> cs = getCategoryOutputSynapse();
        if(cs == null)
            return false;

        CategoryInputSynapse cis = cs.getOutput().getOutgoingCategoryInputSynapse();
        if(cis == null)
            return false;

        return cis.getOutput().getId() == templateNeuron.getId();
    }

    @Override
    public void addInactiveLinks(Activation act) {
        getInputSynapsesAsStream()
                .filter(s -> !s.linkExists(act))
                .forEach(s ->
                        s.createAndInitLink(null, act)
                );
    }

    public ActivationFunction getActivationFunction() {
        return ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
    }

    protected void updateSumOfLowerWeights() {
        ConjunctiveSynapse[] inputSynapses = sortInputSynapses();

        double sum = bias.getUpdatedValue(FIRST_ROUND);
        for(ConjunctiveSynapse s: inputSynapses) {
            double w = s.getWeight().getUpdatedValue(FIRST_ROUND);
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
    public void addInputSynapse(Synapse s) {
        super.addInputSynapse(s);
        s.getWeight().addEventListener(
                "onWeightUpdate",
                this::updateSumOfLowerWeights,
                true
        );
    }

    private ConjunctiveSynapse[] sortInputSynapses() {
        ConjunctiveSynapse[] inputsSynapses = getInputSynapsesByType(ConjunctiveSynapse.class)
                .toList()
                .toArray(new ConjunctiveSynapse[0]);

        Arrays.sort(
                inputsSynapses,
                Comparator.comparingDouble(s -> s.getSortingWeight())
        );
        return inputsSynapses;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        synapseBiasSum.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        synapseBiasSum.readFields(in, m);
    }
}
