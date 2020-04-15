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
import network.aika.neuron.activation.Link;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class TNeuron<S extends Synapse> extends INeuron<S> {

    public static double RELIABILITY_THRESHOLD = 10.0;


    public double binaryFrequency;
    public double frequency;
    public double coveredFactorSum;
    public double coveredFactorCount;


    protected TNeuron() {
        super();
    }

    public TNeuron(Neuron p) {
        super(p);
    }

    public TNeuron(Model model, String label) {
        super(model, label);
    }

    public abstract boolean isMature(Config c);


    public void count(Activation act) {
        double v = act.value * act.getP();
        frequency += v;
        binaryFrequency += (v > 0.0 ? 1.0 : 0.0);

        coveredFactorSum += act.rangeCoverage;
        coveredFactorCount += 1.0;
    }

    public void applyMovingAverage() {
        double alpha = getModel().ALPHA;
        frequency *= alpha;
        binaryFrequency *= alpha;
    }

    public double getP() {
        return frequency / getN();
    }

    public double getN() {
        double coveredFactor = coveredFactorSum / coveredFactorCount;
        return getModel().N / coveredFactor;
    }

    public double getReliability() {
        return binaryFrequency >= RELIABILITY_THRESHOLD ? Math.log(binaryFrequency - (RELIABILITY_THRESHOLD - 1.0)) : 0.0;
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

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(frequency);
        out.writeDouble(binaryFrequency);
        out.writeDouble(coveredFactorSum);
        out.writeDouble(coveredFactorCount);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        frequency = in.readDouble();
        binaryFrequency = in.readDouble();
        coveredFactorSum = in.readDouble();
        coveredFactorCount = in.readDouble();
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

    protected String toDetailedString() {
        return super.toDetailedString() + " f:(" + freqToString() + ")";
    }

    public void dumpStat() {
        System.out.println("OUT:  " + getLabel() + "  Freq:(" + freqToString() + ")  P(" + propToString() + ")");
    }
}
