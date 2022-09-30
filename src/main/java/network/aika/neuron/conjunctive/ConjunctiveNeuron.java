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
import network.aika.neuron.activation.ConjunctiveActivation;
import network.aika.neuron.bindingsignal.BindingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

import static network.aika.neuron.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveNeuron<S extends ConjunctiveSynapse, A extends ConjunctiveActivation> extends Neuron<S, A> {

    private static final Logger log = LoggerFactory.getLogger(ConjunctiveNeuron.class);

    protected ConjunctiveNeuronType type;

    public ConjunctiveNeuron(ConjunctiveNeuronType type) {
        this.type = type;
        bias.addEventListener(this::updateSumOfLowerWeights);
    }


    public ConjunctiveNeuronType getType() {
        return type;
    }

    @Override
    public void setModified() {
        super.setModified();
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

    private void sortInputSynapses() {
        Collections.sort(
                inputSynapses,
                Comparator.<ConjunctiveSynapse>comparingDouble(s -> s.getSortingWeight())
        );
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeInt(type.ordinal());
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        type = ConjunctiveNeuronType.values()[in.readInt()];
    }
}
