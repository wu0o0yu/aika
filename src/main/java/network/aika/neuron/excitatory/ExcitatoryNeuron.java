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
import network.aika.neuron.*;
import network.aika.neuron.InputKey.PureInputKey;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static network.aika.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
import static network.aika.neuron.InputKey.INPUT_COMP;
import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatoryNeuron<S extends Synapse> extends Neuron<S> {

    private static final Logger log = LoggerFactory.getLogger(ExcitatoryNeuron.class);

    private volatile double directConjunctiveBias;
    private volatile double recurrentConjunctiveBias;

    protected TreeMap<InputKey, S> inputSynapses = new TreeMap<>(INPUT_COMP);

    public ExcitatoryNeuron() {
        super();
    }

    public ExcitatoryNeuron(NeuronProvider p) {
        super(p);
    }

    public ExcitatoryNeuron(Model model, String label, Boolean isInputNeuron) {
        super(model, label, isInputNeuron);
    }

    public double computeWeightGradient(Link il) {
        return computeGradient(il, 0, l -> l.getInput().getValue());
    }

    public double computeGradient(Link il, int depth, GradFunction f) {
        if(depth > 2) return 0.0;

        double g = f.grad(il) * getActivationFunction().outerGrad(il.getOutput().getNet());

        double sum = 0.0;
        for (Sign s : Sign.values()) {
            sum += s.getSign() * getCost(s) * g;

            sum = il.getOutput()
                    .getLinks(OUTPUT)
                    .map(ol ->
                            ol.getOutput().getNeuron().computeGradient(
                                    ol,
                                    depth + 1,
                                    l -> g * l.getSynapse().getWeight()
                            )
                    )
                    .reduce(sum, Double::sum);
        }

        return sum;
    }

    public void train(Activation act) {
        addDummyLinks(act);
        super.train(act);

        if(isInputNeuron)
            return;

        double learnRate = getModel().getTrainingConfig().getLearnRate();
        act.getLinks(INPUT)
                .forEach(l ->
                        l.getSynapse().update(learnRate * computeWeightGradient(l))
                );

        commit(inputSynapses.values());
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

    public Synapse getInputSynapse(NeuronProvider n, PatternScope ps) {
        lock.acquireReadLock();
        Synapse s = inputSynapses.get(new PureInputKey(n, ps));
        lock.releaseReadLock();
        return s;
    }

    public void addInputSynapse(S s) {
        inputSynapses.put(s, s);
        setModified();
    }

    public void removeInputSynapse(S s) {
        if(inputSynapses.remove(s) != null) {
            setModified();
        }
    }

    public void addOutputSynapse(Synapse s) {
        outputSynapses.put(s, s);
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

    public boolean isWeak(Synapse s, Synapse.State state) {
        return s.getWeight(state) < getBias();
    }

    @Override
    public boolean hasPositiveRecurrentSynapses() {
        return recurrentConjunctiveBias != 0.0;
    }

    public double getTotalBias(boolean assumePosRecLinks, Synapse.State state) {
        return getBias(state) - (directConjunctiveBias + (assumePosRecLinks ? 0.0 : recurrentConjunctiveBias));
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
            inputSynapses.put(syn, syn);
        }
    }

    public void commit(Collection<? extends Synapse> modifiedSynapses) {
        commitBias();

        directConjunctiveBias = 0.0;
        recurrentConjunctiveBias = 0.0;
        for (Synapse s : inputSynapses.values()) {
            s.commit();

            if(!s.isNegative()) {
                if(!s.isRecurrent()) {
                    directConjunctiveBias += s.getWeight();
                } else  {
                    recurrentConjunctiveBias += s.getWeight();
                }
            }
        }

        setModified();
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
