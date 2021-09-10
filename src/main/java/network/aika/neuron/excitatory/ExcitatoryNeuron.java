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
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.inhibitory.InhibitorySynapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static network.aika.neuron.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
import static network.aika.utils.Utils.logChange;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatoryNeuron<S extends ExcitatorySynapse> extends Neuron<S> {

    private volatile double directConjunctiveBias;
    private volatile double recurrentConjunctiveBias;

    public ExcitatoryNeuron() {
        super();
    }

    public ExcitatoryNeuron(NeuronProvider p) {
        super(p);
    }

    public ExcitatoryNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }

    protected void initFromTemplate(ExcitatoryNeuron n) {
        super.initFromTemplate(n);
        n.directConjunctiveBias = directConjunctiveBias;
        n.recurrentConjunctiveBias = recurrentConjunctiveBias;
    }

    public void addConjunctiveBias(double b, boolean recurrent) {
        if(recurrent) {
            double oldRCB = recurrentConjunctiveBias;
            recurrentConjunctiveBias += b;
            logChange(this, oldRCB, recurrentConjunctiveBias, "limitBias: recurrentConjunctiveBias");
        }
        else {
            double oldDCB = directConjunctiveBias;
            directConjunctiveBias += b;
            logChange(this, oldDCB, directConjunctiveBias, "limitBias: directConjunctiveBias");
        }
    }

    /**
     * If the complete bias exceeds the threshold of 0 by itself, the neuron would become constantly active. The training
     * should account for that and reduce the bias back to a level, where the neuron can be blocked again by its input synapses.
     */
    public void limitBias() {
        double weightSumDirect = getWeightsSum(false);
        double weightSumRecurrent = getWeightsSum(true);

        bias = Math.min(weightSumDirect + weightSumRecurrent, bias);

        if(bias + directConjunctiveBias + recurrentConjunctiveBias > 0.0) {
            double oldDCB = directConjunctiveBias;
            double oldRCB = recurrentConjunctiveBias;

            directConjunctiveBias = Math.max(-weightSumDirect, -bias);
            recurrentConjunctiveBias = Math.max(-weightSumRecurrent, -bias);

            logChange(this, oldDCB, directConjunctiveBias, "limitBias: directConjunctiveBias");
            logChange(this, oldRCB, recurrentConjunctiveBias, "limitBias: recurrentConjunctiveBias");
        }
    }

    private double getWeightsSum(boolean recurrent) {
        return inputSynapses
                .values()
                .stream()
                .filter(s -> !s.isNegative())
                .filter(s -> s.isRecurrent() == recurrent)
                .mapToDouble(Synapse::getWeight)
                .sum();
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
    public Fired incrementFired(Fired f) {
        return new Fired(f.getInputTimestamp(), f.getFired() + 1);
    }

    public double getBias() {
        return super.getBias() + directConjunctiveBias;
    }

    public double getRecurrentBias() {
        return recurrentConjunctiveBias;
    }

    public void updateSynapseInputConnections() {
        TreeSet<Synapse> sortedSynapses = new TreeSet<>(
                Comparator.<Synapse>comparingDouble(Synapse::getWeight).reversed()
                        .thenComparing(Synapse::getPInput)
        );

        sortedSynapses.addAll(inputSynapses.values());

        double sum = super.getBias();
        for(Synapse s: sortedSynapses) {
            if(s.getWeight() <= 0.0)
                break;

            if(sum > 0.0)
                s.linkInput();
            else
                s.unlinkInput();

            sum -= s.getWeight();
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeDouble(directConjunctiveBias);
        out.writeDouble(recurrentConjunctiveBias);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        directConjunctiveBias = in.readDouble();
        recurrentConjunctiveBias = in.readDouble();
    }

    public String toStringWithSynapses() {
        StringBuilder sb = new StringBuilder();
        sb.append(toDetailedString());
        sb.append("\n");
        for (Synapse s : inputSynapses.values()) {
            sb.append("  ");
            sb.append(s.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public String statToString() {
        return super.statToString() +
                inStatToString() +
                outStatToString();
    }

    public String inStatToString() {
        return inputSynapses.values().stream()
                .map(s ->
                        "  in " +
                        s.getInput().getId() +
                        ":" + s.getInput().getLabel() +
                        " " + s.statToString()
                )
                .collect(Collectors.joining());
    }

    public String outStatToString() {
        return outputSynapses.values().stream()
                .filter(s -> s instanceof InhibitorySynapse)
                .map(s ->
                        "  out " +
                        s.getOutput().getId() +
                        ":" + s.getOutput().getLabel() +
                        "  " + s.statToString()
                )
                .collect(Collectors.joining());
    }
}
