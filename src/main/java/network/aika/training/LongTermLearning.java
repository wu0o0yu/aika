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
package network.aika.training;


import network.aika.Document;
import network.aika.Utils;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.INeuron;
import network.aika.training.SynapseEvaluation.Result;

import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.activation.Activation.*;


/**
 *
 * Implements the biologically inspired learning algorithms for long-term potentiation and long-term depression.
 *
 * Wikipedia: "In neuroscience, long-term potentiation (LTP) is a persistent strengthening of synapses based on recent
 * patterns of activity. These are patterns of synaptic activity that produce a long-lasting increase in signal
 * transmission between two neurons. The opposite of LTP is long-term depression, which produces a long-lasting
 * decrease in synaptic strength."
 *
 *
 * @author Lukas Molzberger
 */
public class LongTermLearning {


    public static class Config {
        public double ltpLearnRate;
        public double ltdLearnRate;
        public double beta;



        public Config setLTPLearnRate(double learnRate) {
            this.ltpLearnRate = learnRate;
            return this;
        }


        public Config setLTDLearnRate(double learnRate) {
            this.ltdLearnRate = learnRate;
            return this;
        }


        public Config setBeta(double beta) {
            this.beta = beta;
            return this;
        }
    }



    public static void train(Document doc, Config config) {
        doc.getActivations()
                .filter(act -> act.targetValue == null ? act.isFinalActivation() : act.targetValue > 0.0)
                .filter(act -> act.getINeuron().type != INeuron.Type.META)
                .forEach(act -> {
            longTermPotentiation(config, act);
            longTermDepression(config, act, false);
            longTermDepression(config, act, true);
        });
        doc.commit();
    }


    private static double hConj(Activation act) {
        INeuron n = act.getINeuron();
        return act.getFinalState().net / (n.biasSum + n.posDirSum + n.posRecSum);
    }

    /**
     * The long-term potentiation algorithm is a variant of the Hebb learning rule.
     *
     * @param config
     * @param act
     */
    public static void longTermPotentiation(Config config, Activation act) {
        double iv = Utils.nullSafeMax(act.getFinalState().value, act.targetValue);

        double x = config.ltpLearnRate * (1.0 - act.getFinalState().value) * iv * act.getSelectionProbability();

        act.neuronInputs.values()
                .stream()
                .filter(sa -> sa.input.targetValue == null ? sa.input.isFinalActivation() : sa.input.targetValue > 0.0)
                .forEach(sa -> synapseLTP(config, sa.synapse, sa.input, act, x));
    }


    private static void synapseLTP(Config config, Synapse s, Activation iAct, Activation act, double x) {
        double h = s.isConjunction(false, true) ? hConj(act) : 1.0;

        double delta = iAct.getFinalState().value * x * h * iAct.getSelectionProbability();

        if(delta > 0.0) {
            s.updateDelta(act.doc, delta, -config.beta * delta);
        }
    }


    /**
     * The long-term depression algorithm decreases the strength of a synapse if only one side of the synapse is
     * firing. The algorithm tries however to preserve the logical characteristic of the synapse. If for example the
     * synapse has an or-characteristic, then a non firing input neuron and a firing output neuron will not change
     * the synapse weight. On the other hand, if the synapse has an and-characteristic, then a firing input neuron
     * and a non firing output neuron will not change the synapse weight, too.
     *
     * @param config
     * @param act
     * @param dir
     */
    public static void longTermDepression(Config config, Activation act, boolean dir) {
        if(act.getFinalState().value <= 0.0) return;

        INeuron n = act.getINeuron();

        for(Synapse s: (dir ? n.outputSynapses : n.inputSynapses).values()) {
            if(!s.isNegative() && s.isConjunction(false, true) != dir) {
                double maxSP = 0.0;
                for(Link l: (dir ? act.neuronOutputs : act.neuronInputs).subMap(
                        new Link(s, MIN_ACTIVATION, MIN_ACTIVATION),
                        new Link(s, MAX_ACTIVATION, MAX_ACTIVATION)).values()) {
                    Activation rAct = dir ? l.output : l.input;
                    maxSP = Math.max(maxSP, rAct.getSelectionProbability());
                }

                double delta = -config.ltdLearnRate * act.getFinalState().value * (1.0 - maxSP) * act.getSelectionProbability();
                if(delta < 0.0) {
                    s.updateDelta(act.doc, delta, 0.0);
                }
            }
        }
    }
}
