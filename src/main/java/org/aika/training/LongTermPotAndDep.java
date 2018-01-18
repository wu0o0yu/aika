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
package org.aika.training;


import org.aika.corpus.Document;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;


/**
 *
 * @author Lukas Molzberger
 */
public class LongTermPotAndDep {

    public static double LTP_LEARN_RATE = 0.1;
    public static double LTD_LEARN_RATE = 0.1;

    public static double BETA = 0.5;



    public static class TrainConfig {
        public SynapseEvaluation synapseEvaluation;
        public double ltpLearnRate;
        public double ltdLearnRate;


        /**
         * Determines whether a synapse should be created between two neurons during training.
         *
         * @param synapseEvaluation
         * @return
         */
        public TrainConfig setSynapseEvaluation(SynapseEvaluation synapseEvaluation) {
            this.synapseEvaluation = synapseEvaluation;
            return this;
        }


        public TrainConfig setLTPLearnRate(double learnRate) {
            this.ltpLearnRate = learnRate;
            return this;
        }


        public TrainConfig setLTDLearnRate(double learnRate) {
            this.ltdLearnRate = learnRate;
            return this;
        }
    }



    public static void train(Document doc, TrainConfig trainConfig) {
        for(INeuron n: doc.finallyActivatedNeurons) {
            for(Activation act: n.getFinalActivations(doc)) {
                longTermPotentiation(doc, trainConfig, n, act);
                longTermDepression(doc, trainConfig, n, act, false);
                longTermDepression(doc, trainConfig, n, act, true);
            }
        }
    }


    private static void longTermPotentiation(Document doc, TrainConfig trainConfig, INeuron n, Activation act) {
        double iaSum = 0.0;
        for(Activation.SynapseActivation sa: act.getFinalInputActivations()) {
            if(!sa.synapse.isNegative()) {
                iaSum += sa.input.getFinalState().value * sa.synapse.weight;
            }
        }

        // n.posDirSum wird bei Oder-Neuronen sehr groß.
        double x = LTP_LEARN_RATE * (1.0 - act.getFinalState().value) * (iaSum / (n.posDirSum + n.posRecSum));

        act.getFinalInputActivations().forEach(sa -> {
            SynapseEvaluation.Result r = trainConfig.synapseEvaluation.evaluate(sa.synapse, sa.input, sa.output);
            double sDelta = sa.input.getFinalState().value * x * r.significance;

            sa.synapse.weightDelta += (float) sDelta;
            n.bias -= BETA * sDelta;
            assert !Double.isNaN(n.bias);
        });

        doc.notifyWeightsModified(n, n.inputSynapses.values());
    }


    private static void longTermDepression(Document doc, TrainConfig trainConfig, INeuron n, Activation act, boolean dir) {
        Set<Synapse> actSyns = new TreeSet<>(dir ? Synapse.OUTPUT_SYNAPSE_COMP : Synapse.INPUT_SYNAPSE_COMP);
        (dir ? act.getFinalOutputActivations() : act.getFinalInputActivations()).forEach(sa -> actSyns.add(sa.synapse));

        (dir ? n.outputSynapses : n.inputSynapses).values().stream()
                .filter(s -> !s.isNegative() && !actSyns.contains(s))
                .forEach(s -> {
                    SynapseEvaluation.Result r = trainConfig.synapseEvaluation.evaluate(s, dir ? act : null, dir ? null : act);
                    s.weightDelta -= (float) (LTD_LEARN_RATE * act.getFinalState().value * r.significance);

                    if (dir) {
                        doc.notifyWeightsModified(s.output.get(), Collections.singletonList(s));
                    }
                });

        if(!dir) {
            doc.notifyWeightsModified(n, n.inputSynapses.values());
        }
    }
}
