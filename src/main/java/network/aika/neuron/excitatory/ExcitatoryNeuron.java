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

import network.aika.*;
import network.aika.neuron.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.phase.activation.ActivationPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static network.aika.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatoryNeuron<S extends ExcitatorySynapse> extends Neuron<S> {

    private static final Logger log = LoggerFactory.getLogger(ExcitatoryNeuron.class);

    private volatile double directConjunctiveBias;
    private volatile double recurrentConjunctiveBias;

    public ExcitatoryNeuron() {
        super();
    }

    public ExcitatoryNeuron(NeuronProvider p) {
        super(p);
    }

    public ExcitatoryNeuron(Model model) {
        super(model);
    }

    public void setDirectConjunctiveBias(double b) {
        directConjunctiveBias = b;
    }

    public void setRecurrentConjunctiveBias(double b) {
        recurrentConjunctiveBias = b;
    }

    public void addConjunctiveBias(double b, boolean recurrent) {
        if(recurrent) {
            recurrentConjunctiveBias += b;
        } else {
            directConjunctiveBias += b;
        }
    }

    public boolean isInitialized() {
        return getInputSynapses().count() > 1;
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

    public Stream<? extends Synapse> getInputSynapses() {
        return inputSynapses.values().stream();
    }

    public ActivationFunction getActivationFunction() {
        return RECTIFIED_HYPERBOLIC_TANGENT;
    }

    @Override
    public Fired incrementFired(Fired f) {
        return new Fired(f.getInputTimestamp(), f.getFired() + 1);
    }

    public double getBias(ActivationPhase p) {
        return super.getBias(p) + (directConjunctiveBias + (p == ActivationPhase.INITIAL_LINKING ? 0.0 : recurrentConjunctiveBias));
    }

    public void updatePropagateFlag() {
        TreeSet<Synapse> sortedSynapses = new TreeSet<>(
                Comparator.<Synapse>comparingDouble(s -> s.getWeight()).reversed()
                        .thenComparing(s -> s.getPInput())
        );

        sortedSynapses.addAll(inputSynapses.values());

        double sum = getRawBias();
        for(Synapse s: sortedSynapses) {
            if(s.getWeight() <= 0.0) break;

            s.updateInputLink(sum > 0.0);

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
        StringBuilder sb = new StringBuilder();

        sb.append(super.statToString());

        sb.append(inStatToString());
        sb.append(outStatToString());

        return sb.toString();
    }

    public String inStatToString() {
        StringBuilder sb = new StringBuilder();
        inputSynapses.values().forEach(s ->
                sb.append("  in " + s.getInput().getId() + ":" + s.getInput().getDescriptionLabel() + " " + s.statToString())
        );
        return sb.toString();
    }

    public String outStatToString() {
        StringBuilder sb = new StringBuilder();
        outputSynapses.values().stream()
                .filter(s -> s instanceof InhibitorySynapse)
                .forEach(s ->
                        sb.append("  out " + s.getOutput().getId() + ":" + s.getOutput().getDescriptionLabel() + " " + " " + s.statToString())
        );
        return sb.toString();
    }
}
