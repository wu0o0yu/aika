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
package network.aika;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.Phase;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.Sign.POS;

public class Config {
    private Double alpha = null; //0.99;
    private double learnRate;

    private Set<Phase> phases = new TreeSet<>(Comparator.comparing(p -> p.getRank()));

    private double surprisalInductionThreshold = 2.0;
    private double gradientInductionThreshold = 2.0;

    public Set<Phase> getPhases() {
        return phases;
    }

    public Config setPhases(Phase... phases) {
        for(Phase p: phases) {
            this.phases.add(p);
        }
        return this;
    }

    public double getLearnRate() {
        return learnRate;
    }

    public Config setLearnRate(double learnRate) {
        this.learnRate = learnRate;
        return this;
    }

    public Double getAlpha() {
        return alpha;
    }

    public Config setAlpha(Double alpha) {
        this.alpha = alpha;
        return this;
    }

    public double getSurprisalInductionThreshold() {
        return surprisalInductionThreshold;
    }

    public Config setSurprisalInductionThreshold(double surprisalInductionThreshold) {
        this.surprisalInductionThreshold = surprisalInductionThreshold;
        return this;
    }

    public double getGradientInductionThreshold() {
        return gradientInductionThreshold;
    }

    public Config setGradientInductionThreshold(double gradientInductionThreshold) {
        this.gradientInductionThreshold = gradientInductionThreshold;
        return this;
    }

    public boolean checkPatternPartNeuronInduction(Neuron n) {
        double s = n.getSurprisal(POS);

        return s < getSurprisalInductionThreshold();
    }


    public boolean checkInhibitoryNeuronInduction(Neuron n) {
        double s = n.getSurprisal(POS);

        return s < getSurprisalInductionThreshold();
    }

    public boolean checkPatternNeuronInduction(Activation act) {
        double s = act.getSelfGradient();

        return s < getGradientInductionThreshold();
    }

    public boolean checkSynapseInduction(Link l) {
        Config c = l.getOutput().getThought().getConfig();

        return (l.getFinalGradient() - l.getOutput().getSelfGradient()) > -c.getSurprisalInductionThreshold();
    }

    public String getLabel(Activation iAct, Neuron n) {
        return "";
    }

    public String toString() {
        return "Alpha: " + alpha + "\n" +
                "LearnRate" + learnRate + "\n" +
                "SurprisalInductionThreshold" + surprisalInductionThreshold + "\n" +
                "GradientInductionThreshold" + gradientInductionThreshold + "\n\n";
    }
}
