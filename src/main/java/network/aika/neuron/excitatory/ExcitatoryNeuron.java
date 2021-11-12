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

import network.aika.Model;
import network.aika.neuron.ActivationFunction;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.fields.Field;
import network.aika.neuron.inhibitory.InhibitorySynapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;

import static network.aika.neuron.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatoryNeuron<S extends ExcitatorySynapse, A extends Activation> extends Neuron<S, A> {

    private volatile Field conjunctiveBias;
    private volatile Field weightSum;

    public ExcitatoryNeuron() {
        super();
    }

    public ExcitatoryNeuron(NeuronProvider p) {
        super(p);
    }

    public ExcitatoryNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }

    public Field getWeightSum() {
        return weightSum;
    }

    public Field getConjunctiveBias() {
        return conjunctiveBias;
    }

    public void addWeight(double weightDelta) {
        weightSum.add(weightDelta);
    }

    protected void initFromTemplate(ExcitatoryNeuron n) {
        super.initFromTemplate(n);
        n.conjunctiveBias = conjunctiveBias;
    }

    public void addConjunctiveBias(double b) {
        conjunctiveBias.add(b);
    }

    /**
     * If the complete bias exceeds the threshold of 0 by itself, the neuron would become constantly active. The training
     * should account for that and reduce the bias back to a level, where the neuron can be blocked again by its input synapses.
     */
    public void limitBias() {
        bias = Math.min(weightSum, bias);

        if(bias + conjunctiveBias > 0.0) {
            double oldCB = conjunctiveBias;

            conjunctiveBias = Math.max(-weightSum, -bias);
        }
    }

    public void addDummyLinks(Activation act) {
        inputSynapses
                .values()
                .stream()
                .filter(s -> !act.inputLinkExists(s))
                .forEach(s ->
                        new Link(s, null, act, false)
                );
    }

    public ActivationFunction getActivationFunction() {
        return RECTIFIED_HYPERBOLIC_TANGENT;
    }

    @Override
    public double getInitialNet() {
        return bias.getOldValue() + conjunctiveBias.getOldValue();
    }

    public void updateSynapseInputConnections() {
        TreeSet<ExcitatorySynapse> sortedSynapses = new TreeSet<>(
                Comparator.<ExcitatorySynapse>comparingDouble(Synapse::getWeight).reversed()
                        .thenComparing(Synapse::getPInput)
        );

        sortedSynapses.addAll(inputSynapses.values());

        double sum = getWeightSum();
        for(ExcitatorySynapse s: sortedSynapses) {
            if(s.getWeight() <= 0.0)
                break;

            if(s.isWeak(sum))
                s.unlinkInput();
            else
                s.linkInput();

            sum -= s.getWeight();
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        conjunctiveBias.write(out);
        weightSum.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        conjunctiveBias.readFields(in, m);
        weightSum.readFields(in, m);
    }
}
