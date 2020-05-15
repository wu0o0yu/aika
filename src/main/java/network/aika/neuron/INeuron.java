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
package network.aika.neuron;


import network.aika.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.OutputKey.PureOutputKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static network.aika.neuron.Synapse.OUTPUT_COMP;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.Direction.OUTPUT;
import static network.aika.neuron.activation.linker.LinkGraphs.propagateT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class INeuron<S extends Synapse> extends AbstractNode<Neuron> implements Comparable<INeuron> {

    public static double RELIABILITY_THRESHOLD = 10.0;

    private static final Logger log = LoggerFactory.getLogger(INeuron.class);

    private String label;

    private volatile double bias;
    private volatile double biasDelta;

    protected TreeMap<Synapse, Synapse> outputSynapses = new TreeMap<>(OUTPUT_COMP);

    protected ReadWriteLock lock = new ReadWriteLock();

    public double binaryFrequency;
    public double frequency;
    public double coveredFactorSum;
    public double coveredFactorCount;

    protected boolean isInputNeuron; // Input Neurons won't be trained!


    protected INeuron() {
    }

    public INeuron(Neuron p) {
        provider = p;
    }

    public INeuron(Model m, String label, Boolean isInputNeuron) {
        this.label = label;
        this.isInputNeuron = isInputNeuron;
        provider = new Neuron(m, this);
        setModified();
    }

    public void init(Thought t, Double bias, Collection<Synapse.Builder> synapseBuilders) {
        if(bias != null) {
            setBias(bias);
        }

        ArrayList<Synapse> modifiedSynapses = new ArrayList<>();
        synapseBuilders.forEach(input -> {
            Synapse s = input.getSynapse(getProvider());
            s.link();
            s.update(t, input.weight);
            modifiedSynapses.add(s);
        });

        modifiedSynapses.forEach(s -> s.link());
        commit(modifiedSynapses);
    }

    public abstract ActivationFunction getActivationFunction();

    public abstract Fired incrementFired(Fired f);

    public abstract boolean isWeak(Synapse synapse, Synapse.State state);

    public abstract Synapse getInputSynapse(Neuron n, PatternScope ps);

    public Synapse getOutputSynapse(Neuron n, PatternScope ps) {
        lock.acquireReadLock();
        Synapse s = outputSynapses.get(new PureOutputKey(n, ps));
        lock.releaseReadLock();
        return s;
    }

    public Stream<Synapse> getOutputSynapses() {
        return outputSynapses.values().stream();
    }

    public abstract void addInputSynapse(S s);

    public abstract void addOutputSynapse(Synapse synapse);

    public abstract void removeInputSynapse(S s);

    public abstract void removeOutputSynapse(Synapse s);

    public abstract void commit(Collection<? extends Synapse> modifiedSynapses);

    public abstract byte getType();

    public abstract byte getOuterType();

    public Integer getId() {
        return provider.getId();
    }

    public String getLabel() {
        return label;
    }

    public Model getModel() {
        return provider.getModel();
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param input
     */
    public Activation addInputActivation(Thought doc, Activation.Builder input) {
        Activation act = new Activation(doc.getNewActivationId(), doc, this);
        act.initInputActivation(input);
        return act;
    }

    public void commitBias() {
        bias += biasDelta;
        biasDelta = 0.0;
    }

    public void setBias(double b) {
        biasDelta = b - bias;
    }

    public void updateBiasDelta(double biasDelta) {
        this.biasDelta += biasDelta;
    }

    public abstract double getTotalBias(boolean assumePosRecLinks, Synapse.State state);

    public abstract boolean hasPositiveRecurrentSynapses();

    public double getBias() {
        return bias;
    }

    protected double getBias(Synapse.State state) {
        return state == CURRENT ? bias : bias + biasDelta;
    }

    public double getCost(Sign s) {
        return 0.0;
    }

    public double computeGradient(Link l, int depth, Function<Link, Double> f) {
        return 0.0;
    }

    public abstract double propagateRangeCoverage(Activation iAct);

    public void linkForwards(Activation act) {
        propagateT.follow(act, OUTPUT, false);
    }

    public abstract void linkBackwards(Link l);

    public abstract void linkPosRecSynapses(Activation act);

    public abstract void induceStructure(Activation act);

    public int compareTo(INeuron n) {
        if (this == n) return 0;
        return Integer.compare(getId(), n.getId());
    }

    public abstract boolean isMature();

    public void count(Activation act) {
        double v = act.getValue();
        frequency += v;
        binaryFrequency += (v > 0.0 ? 1.0 : 0.0);

        coveredFactorSum += act.rangeCoverage;
        coveredFactorCount += 1.0;
    }

    public void applyMovingAverage() {
        Double alpha = getModel().getTrainingConfig().getAlpha();
        if(alpha != null) {
            frequency *= alpha;
            binaryFrequency *= alpha;
        }
    }

    public void train(Activation act) {
        induceStructure(act);
        act.getThought().processLinks();
    }

    public double getP() {
        return frequency / getN();
    }

    public double getN() {
        double coveredFactor = coveredFactorSum / coveredFactorCount;
        return getModel().getN() / coveredFactor;
    }

    public double getReliability() {
        return binaryFrequency >= RELIABILITY_THRESHOLD ? Math.log(binaryFrequency - (RELIABILITY_THRESHOLD - 1.0)) : 0.0;
    }

    private double getCoverage(Activation seedAct) {
        return seedAct.getLinks(OUTPUT)
                .map(l -> getCoverage(l))
                .max(Comparator.comparingDouble(c -> c))
                .orElse(0.0);
    }

    private static double getCoverage(Link ol) {
        Activation oAct = ol.getOutput();
        INeuron n = oAct.getINeuron();
        return Math.min(Math.max(0.0, oAct.getNet()), Math.max(0.0, ol.getInput().getValue() * ol.getSynapse().getWeight())) / n.getBias();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());

        out.writeBoolean(label != null);
        if(label != null) {
            out.writeUTF(label);
        }

        out.writeDouble(bias);

        for (Synapse s : outputSynapses.values()) {
            if (s.getOutput() != null) {
                out.writeBoolean(true);
                getModel().writeSynapse(s, out);
            }
        }
        out.writeBoolean(false);

        out.writeDouble(frequency);
        out.writeDouble(binaryFrequency);
        out.writeDouble(coveredFactorSum);
        out.writeDouble(coveredFactorCount);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        if(in.readBoolean()) {
            label = in.readUTF();
        }

        bias = in.readDouble();

        while (in.readBoolean()) {
            Synapse syn = m.readSynapse(in);
            outputSynapses.put(syn, syn);
        }

        frequency = in.readDouble();
        binaryFrequency = in.readDouble();
        coveredFactorSum = in.readDouble();
        coveredFactorCount = in.readDouble();
    }

    public String toString() {
        return label;
    }

    protected String toDetailedString() {
        return typeToString() + " " + label + " B:" + Utils.round(bias);
    }

    public abstract String typeToString();

    public String freqToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pos:" + Utils.round(frequency));
        sb.append(" Neg:" + Utils.round(getN() - frequency));
        return sb.toString();
    }

    public String propToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pos:" + Utils.round(Sign.POS.getP(this)));
        sb.append(" Neg:" + Utils.round(Sign.NEG.getP(this)));
        return sb.toString();
    }

    public void dumpStat() {
        System.out.println("OUT:  " + getLabel() + "  Freq:(" + freqToString() + ")  P(" + propToString() + ")");
    }
}
