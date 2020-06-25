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
import network.aika.Phase;
import network.aika.neuron.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

import static network.aika.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
import static network.aika.neuron.Sign.NEG;
import static network.aika.neuron.Sign.POS;
import static network.aika.neuron.activation.Activation.TOLERANCE;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatoryNeuron<S extends Synapse> extends Neuron<S> {

    private static final Logger log = LoggerFactory.getLogger(ExcitatoryNeuron.class);

    private volatile double directConjunctiveBias;
    private volatile double recurrentConjunctiveBias;

    protected TreeMap<NeuronProvider, S> inputSynapses = new TreeMap<>();

    public ExcitatoryNeuron() {
        super();
    }

    public ExcitatoryNeuron(NeuronProvider p) {
        super(p);
    }

    public ExcitatoryNeuron(Model model, String label, Boolean isInputNeuron) {
        super(model, label, isInputNeuron);
    }

    public void commit() {
        commit(inputSynapses.values());
    }

    public void setDirectConjunctiveBias(double b) {
        directConjunctiveBias = b - directConjunctiveBias;
    }

    public void updateDirectConjunctiveBiasDelta(double directConjunctiveBias) {
        this.directConjunctiveBias += directConjunctiveBias;
    }

    public void setRecurrentConjunctiveBias(double b) {
        recurrentConjunctiveBias = b - recurrentConjunctiveBias;
    }

    public void updateRecurrentConjunctiveBiasDelta(double recurrentConjunctiveBias) {
        this.recurrentConjunctiveBias += recurrentConjunctiveBias;
    }

    public void train(Activation act) {
        addDummyLinks(act);
        super.train(act);
    }

    protected void propagateCost(Activation act) {
        double cost =
                (POS.getSign() * getCost(POS)) +
                (NEG.getSign() * getCost(NEG));

        if(Math.abs(cost) < TOLERANCE) {
            return;
        }

        act.getMutableGradient().gradient += cost;
    }

    protected void addDummyLinks(Activation act) {
        inputSynapses
                .values()
                .stream()
                .filter(s -> !act.inputLinkExists(s))
                .map(s -> new Link(s, null, act))
                .forEach(l -> l.link());
    }

    @Override
    public void suspend() {
    }

    @Override
    public void reactivate() {
    }

    public Synapse getInputSynapse(NeuronProvider n) {
        lock.acquireReadLock();
        Synapse s = inputSynapses.get(n);
        lock.releaseReadLock();
        return s;
    }

    public void addInputSynapse(S s) {
        inputSynapses.put(s.getPInput(), s);
        setModified();
    }

    public void removeInputSynapse(S s) {
        if(inputSynapses.remove(s) != null) {
            setModified();
        }
    }

    public void addOutputSynapse(Synapse s) {
        outputSynapses.put(s.getPOutput(), s);
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
        return RECTIFIED_HYPERBOLIC_TANGENT;
    }

    @Override
    public Fired incrementFired(Fired f) {
        return new Fired(f.getInputTimestamp(), f.getFired() + 1);
    }

    /*
    public boolean isWeak(Synapse s, Synapse.State state) {
        return s.getWeight(state) < getBias();
    }
*/

    public double getBias(Phase p, Synapse.State state) {
        return super.getBias(p, state) + (directConjunctiveBias + (p == Phase.INITIAL_LINKING ? 0.0 : recurrentConjunctiveBias));
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeDouble(directConjunctiveBias);
        out.writeDouble(recurrentConjunctiveBias);

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

        directConjunctiveBias = in.readDouble();
        recurrentConjunctiveBias = in.readDouble();

        while (in.readBoolean()) {
            S syn = (S) m.readSynapse(in);
            inputSynapses.put(syn.getPInput(), syn);
        }
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
}
