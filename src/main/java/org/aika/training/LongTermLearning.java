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
import org.aika.training.SynapseEvaluation.Result;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;


/**
 *
 * Implements the biologically inspired learning algorithms long term potentiation and long term depression.
 *
 *
 *
 * @author Lukas Molzberger
 */
public class LongTermLearning {


    public static class TrainConfig {
        public SynapseEvaluation synapseEvaluation;
        public double ltpLearnRate;
        public double ltdLearnRate;
        public double beta;


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


        public void setBeta(double beta) {
            this.beta = beta;
        }
    }



    public static void train(Document doc, TrainConfig trainConfig) {
        doc.getFinalActivations().forEach(act -> {
            longTermPotentiation(doc, trainConfig, act);
            longTermDepression(doc, trainConfig, act, false);
            longTermDepression(doc, trainConfig, act, true);
        });
    }


    private static void longTermPotentiation(Document doc, TrainConfig trainConfig, Activation act) {
        INeuron n = act.key.node.neuron.get();

        double iaSum = 0.0;
        for(Activation.SynapseActivation sa: act.getFinalInputActivations()) {
            if(!sa.synapse.isNegative()) {
                iaSum += sa.input.getFinalState().value * sa.synapse.weight;
            }
        }

        // n.posDirSum wird bei Oder-Neuronen sehr groß.
        double x = trainConfig.ltpLearnRate * (1.0 - act.getFinalState().value) * (iaSum / (n.posDirSum + n.posRecSum));

        doc.getFinalActivations().forEach(iAct -> {
            Result r = trainConfig.synapseEvaluation.evaluate(null, iAct, act);
            double sDelta = iAct.getFinalState().value * x * r.significance;

            Synapse synapse = Synapse.createOrLookup(r.synapseKey, iAct.key.node.neuron, act.key.node.neuron);

            synapse.weightDelta += (float) sDelta;
            n.bias -= trainConfig.beta * sDelta;
            assert !Double.isNaN(n.bias);
        });

        doc.notifyWeightsModified(n, n.inputSynapses.values());
    }


    private static void longTermDepression(Document doc, TrainConfig trainConfig, Activation act, boolean dir) {
        INeuron n = act.key.node.neuron.get();

        Set<Synapse> actSyns = new TreeSet<>(dir ? Synapse.OUTPUT_SYNAPSE_COMP : Synapse.INPUT_SYNAPSE_COMP);
        (dir ? act.getFinalOutputActivations() : act.getFinalInputActivations()).forEach(sa -> actSyns.add(sa.synapse));

        (dir ? n.outputSynapses : n.inputSynapses).values().stream()
                .filter(s -> !s.isNegative() && !actSyns.contains(s))
                .forEach(s -> {
                    Result r = trainConfig.synapseEvaluation.evaluate(s, dir ? act : null, dir ? null : act);
                    s.weightDelta -= (float) (trainConfig.ltdLearnRate * act.getFinalState().value * r.significance);

                    if (dir) {
                        doc.notifyWeightsModified(s.output.get(), Collections.singletonList(s));
                    }
                });

        if(!dir) {
            doc.notifyWeightsModified(n, n.inputSynapses.values());
        }
    }
}
