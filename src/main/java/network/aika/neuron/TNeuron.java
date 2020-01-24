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


import network.aika.Config;
import network.aika.Model;
import network.aika.Utils;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.ExcitatoryNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class TNeuron<S extends Synapse> extends INeuron<S> {

    public static double RELIABILITY_THRESHOLD = 10.0;


    public double countValue;
    public double binaryFrequency;
    public double posFrequency;
    public double negFrequency;
    public double N;
    public int lastCount;
    public double alpha = 0.99;

    public double lengthSum;
    public int numActs;


    public double trainingBias = 0.0;


    protected TNeuron() {
        super();
    }


    public TNeuron(Neuron p) {
        super(p);
    }


    public TNeuron(Model model, String label) {
        super(model, label);
        this.lastCount = model.charCounter;
    }

    public void initCountValues() {
        countValue = 0.0;

        for (Synapse s : getProvider().getActiveInputSynapses()) {
            TSynapse ts = (TSynapse) s;

            ts.initCountValues();
        }
        for (Synapse s : getProvider().getActiveOutputSynapses()) {
            TSynapse ts = (TSynapse) s;

            ts.initCountValues();
        }
    }


    public double getAvgLength() {
        return lengthSum / (double) numActs;
    }


    public void count(Activation act) {
        countValue += act.value * act.getP();

        countSynapses(act, INPUT);
        countSynapses(act, OUTPUT);
    }


    private void countSynapses(Activation act, Direction dir) {
        Set<Synapse> rest = new TreeSet<>(dir.getSynapseComparator());
        rest.addAll(getSynapses(dir));

        act.getLinks(dir)
                .stream()
                .filter(l -> l.getSynapse() != null)
                .forEach(l -> {
                    TSynapse ts = (TSynapse) l.getSynapse();

                    rest.remove(ts);

                    dir.getUpdateCounts().updateCounts(ts, l.getActivation(dir), act);
                });

        rest
                .stream()
                .map(s -> (TSynapse) s)
                .forEach(s -> dir.getUpdateCounts().updateCounts(s, null, act));
    }


    public void updateFrequencies(Activation act) {
        int beginPos = 0; //StatUtil.getCurrentPos(act, BEGIN);
        int endPos = 0; //StatUtil.getCurrentPos(act, END);

        int stepsBefore = beginPos - lastCount;
        int stepsWithin = endPos - beginPos;

        lengthSum += stepsWithin;
        numActs++;

        lastCount = endPos;

        posFrequency = (stepsWithin * countValue) + (alpha * posFrequency);
        negFrequency = stepsBefore + ((double) stepsWithin * (1.0 - countValue)) + (alpha * negFrequency);
        binaryFrequency = (countValue > 0.0 ? 1.0 : 0.0) + (alpha * binaryFrequency);
        N = stepsBefore + stepsWithin + (alpha * N);

        updateSynapseFrequencies(act, INPUT);
        updateSynapseFrequencies(act, OUTPUT);
    }


    private void updateSynapseFrequencies(Activation act, Direction dir) {
        Set<Synapse> rest = new TreeSet<>(dir.getSynapseComparator());
        rest.addAll(getSynapses(dir));

        act.getLinks(dir)
                .stream()
                .filter(l -> l.getSynapse() != null)
                .forEach(l -> {
                    TSynapse ts = (TSynapse) l.getSynapse();

                    rest.remove(ts);
                    dir.getUpdateFrequencies().updateFrequencies(ts, alpha, l.getActivation(dir), act);
                });

        rest
                .stream()
                .map(s -> (TSynapse) s)
                .forEach(s -> dir.getUpdateFrequencies().updateFrequencies(s, alpha, null, act));
    }


    public double[] getP() {
        double p = posFrequency / N;
        return new double[] {
                p,
                1.0 - p
        };
    }


    public String freqToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pos:" + Utils.round(posFrequency));
        sb.append(" Neg:" + Utils.round(N - posFrequency));
        return sb.toString();
    }

    public String propToString() {
        double[] p = getP();
        StringBuilder sb = new StringBuilder();
        sb.append("Pos:" + Utils.round(p[0]));
        sb.append(" Neg:" + Utils.round(p[1]));
        return sb.toString();
    }


    public double getReliability() {
        return binaryFrequency >= RELIABILITY_THRESHOLD ? Math.log(binaryFrequency - (RELIABILITY_THRESHOLD - 1.0)) : 0.0;
    }


    public void prepareTrainingStep(Config c, Activation o, Function<Activation, ExcitatoryNeuron> callback) {
        prepareMetaTraining(c, o, callback);
        count(o);
    }


    public void train(Config c, Activation o) {
//      if((o.p * (1.0 - getCoverage(o))) > THRESHOLD) {
        if (isMature(c)) {
            generateNeuron(o);
        }
//      }
    }


    // Implemented only for meta and target neurons
    public void prepareMetaTraining(Config c, Activation act, Function<Activation, ExcitatoryNeuron> callback) {
        if (act.getP() > c.getMetaThreshold() && getTrainingNetValue(act) > 0.0) {
//            act.targetNeuron = getTargetNeuron(act, callback);
        }
    }


    public ExcitatoryNeuron getTargetNeuron(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        return null;
    }


    public double getTrainingNetValue(Activation act) {
        return act.net;
    }


    public void computeOutputRelations() {

    }


    private void generateNeuron(Activation act) {
        ExcitatoryNeuron targetNeuron = new ExcitatoryNeuron(getModel(), "DERIVED-FROM-(" + act.getLabel() + ")");

        targetNeuron.init(act);
    }


    private double getCoverage(Activation seedAct) {
        double maxCoverage = 0.0;
        for(Map.Entry<Activation, Link> me: seedAct.outputLinks.entrySet()) {
            maxCoverage = Math.max(maxCoverage, getCoverage(me.getValue()));
        }

        return maxCoverage;
    }


    private static double getCoverage(Link ol) {
        Activation oAct = ol.getOutput();
        INeuron n = oAct.getINeuron();
        return Math.min(Math.max(0.0, oAct.net), Math.max(0.0, ol.getInput().value * ol.getSynapse().getWeight())) / n.getBias();
    }


    public abstract boolean isMature(Config c);


    protected String toDetailedString() {
        return super.toDetailedString() + " f:(" + freqToString() + ")";
    }


    public void dumpStat() {
        System.out.println("OUT:  " +getLabel() + "  Freq:(" + freqToString() + ")  P(" + propToString() + ")");

        for(Synapse s: getProvider().getActiveInputSynapses()) {
            TSynapse ts = (TSynapse) s;

            System.out.println("IN:  " + ts.getInput().getLabel());
            System.out.println("     Freq:(" + ts.freqToString() + ")");
            System.out.println("     PXi(" + ts.pXiToString() + ")");
            System.out.println("     PXout(" + ts.pXoutToString() + ")");
            System.out.println("     P(" + ts.propToString() + ")");
            System.out.println("     Rel:" + ts.getReliability());
        }
    }

    // TODO: store and retrieve targetSynapseIds
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(posFrequency);
        out.writeDouble(N);
        out.writeInt(lastCount);

        out.writeDouble(trainingBias);
    }


    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        posFrequency = in.readDouble();
        N = in.readDouble();
        lastCount = in.readInt();

        trainingBias = in.readDouble();
    }

}
