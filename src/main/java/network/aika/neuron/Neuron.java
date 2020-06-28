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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Neuron<S extends Synapse> implements Writable {

    private static final Logger log = LoggerFactory.getLogger(Neuron.class);

    volatile long retrievalCount = 0;

    private volatile boolean modified;

    private NeuronProvider provider;

    private String label;

    private volatile double bias;

    protected TreeMap<NeuronProvider, Synapse> outputSynapses = new TreeMap<>();

    protected final ReadWriteLock lock = new ReadWriteLock();

    protected double binaryFrequency;
    protected double frequency;
    protected double coveredFactorSum;
    protected double coveredFactorCount;

    protected boolean isInputNeuron; // Input Neurons won't be trained!


    protected Neuron() {
    }

    public Neuron(NeuronProvider p) {
        provider = p;
    }

    public Neuron(Model m, String label, Boolean isInputNeuron) {
        this.label = label;
        this.isInputNeuron = isInputNeuron;
        provider = new NeuronProvider(m, this);
        modified = true;
    }

    public abstract ActivationFunction getActivationFunction();

    public abstract Fired incrementFired(Fired f);

    public abstract Synapse getInputSynapse(NeuronProvider n);

    public NeuronProvider getProvider() {
        return provider;
    }

    public Stream<Synapse> getOutputSynapses() {
        return outputSynapses.values().stream();
    }

    public abstract void tryToLink(Activation iAct, Activation oAct);

    public abstract void addInputSynapse(S s);

    public abstract void addOutputSynapse(Synapse synapse);

    public abstract void removeInputSynapse(S s);

    public abstract void removeOutputSynapse(Synapse s);

    public abstract byte getType();

    public Long getId() {
        return provider.getId();
    }

    public String getLabel() {
        return label;
    }

    public Model getModel() {
        return provider.getModel();
    }

    public long getRetrievalCount() {
        return retrievalCount;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void setBias(double b) {
        bias += b;
        modified = true;
    }

    public void updateBias(double biasDelta) {
        bias += biasDelta;
        modified = true;
    }

    public double getBias(Phase p) {
        return bias;
    }

    public double getCost(Sign s) {
        return 0.0;
    }

    public abstract double propagateRangeCoverage(Activation iAct);

    public ReadWriteLock getLock() {
        return lock;
    }

    public void count(Activation act) {
        double v = act.getValue();
        frequency += v;
        binaryFrequency += (v > 0.0 ? 1.0 : 0.0);

        coveredFactorSum += act.rangeCoverage;
        coveredFactorCount += 1.0;
    }

    public void applyMovingAverage(Config trainingConfig) {
        Double alpha = trainingConfig.getAlpha();
        if(alpha != null) {
            frequency *= alpha;
            binaryFrequency *= alpha;
        }
    }

    public abstract Neuron induceNeuron(Activation act);

    public abstract Synapse induceSynapse(Activation iAct, Activation oAct);

    public void train(Activation act) {
        act.propagate();
        act.getThought().processLinks();

        if(isInputNeuron) {
            return;
        }

        propagateCost(act);
    }

    protected void propagateCost(Activation act) {
    }

    public double getP() {
        return frequency / getN();
    }

    public double getN() {
        double coveredFactor = coveredFactorSum / coveredFactorCount;
        return getModel().getN() / coveredFactor;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setBinaryFrequency(int f) {
        binaryFrequency = f;
    }

    public void reactivate() {
    }

    public void suspend() {
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
            outputSynapses.put(syn.getPOutput(), syn);
        }

        frequency = in.readDouble();
        binaryFrequency = in.readDouble();
        coveredFactorSum = in.readDouble();
        coveredFactorCount = in.readDouble();
    }

    public String toString() {
        return getId() + ":" + getLabel();
    }

    public String toDetailedString() {
        return "N " + getClass().getSimpleName() + " " + toString() + " B:" + Utils.round(bias);
    }

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
