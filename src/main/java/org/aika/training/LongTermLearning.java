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
        public Config setSynapseEvaluation(SynapseEvaluation synapseEvaluation) {
            this.synapseEvaluation = synapseEvaluation;
            return this;
        }


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
        doc.getFinalActivations().forEach(act -> {
            longTermPotentiation(doc, config, act);
            longTermDepression(doc, config, act, false);
            longTermDepression(doc, config, act, true);
        });
        doc.commit();
    }


    /**
     * The long-term potentiation algorithm is a variant of the Hebb learning rule.
     *
     * @param doc
     * @param config
     * @param act
     */
    public static void longTermPotentiation(Document doc, Config config, Activation act) {
        INeuron n = act.key.node.neuron.get();

        double maxActValue = n.activationFunction.f(n.biasSum + n.posDirSum + n.posRecSum);
        double m = maxActValue > 0.0 ? Math.max(1.0, act.getFinalState().value / maxActValue) : 1.0;
        double x = config.ltpLearnRate * (1.0 - act.getFinalState().value) * m;

        doc.getFinalActivations().filter(iAct -> iAct.key.node != act.key.node).forEach(iAct -> {
            Result r = config.synapseEvaluation.evaluate(null, iAct, act);
            double sDelta = iAct.getFinalState().value * x * r.significance;

            if(sDelta > 0.0) {
                Synapse synapse = Synapse.createOrLookup(r.synapseKey, iAct.key.node.neuron, act.key.node.neuron);

                synapse.weightDelta += (float) sDelta;
                synapse.biasDelta -= config.beta * sDelta;
                assert !Double.isNaN(n.bias);
            }
        });

        doc.notifyWeightsModified(n, n.inputSynapses.values());
    }


    /**
     * The long-term depression algorithm decreases the strength of a synapse if only one side of the synapse is
     * firing. The algorithm tries however to preserve the logical characteristic of the synapse. If for example the
     * synapse has an or-characteristic, then a non firing input neuron and a firing output neuron will not change
     * the synapse weight. On the other hand, if the synapse has an and-characteristic, then a firing input neuron
     * and a non firing output neuron will not change the synapse weight, too.
     *
     * @param doc
     * @param config
     * @param act
     * @param dir
     */
    public static void longTermDepression(Document doc, Config config, Activation act, boolean dir) {
        INeuron n = act.key.node.neuron.get();

        Set<Synapse> actSyns = new TreeSet<>(dir ? Synapse.OUTPUT_SYNAPSE_COMP : Synapse.INPUT_SYNAPSE_COMP);
        (dir ? act.getFinalOutputActivations() : act.getFinalInputActivations()).forEach(sa -> actSyns.add(sa.synapse));

        (dir ? n.outputSynapses : n.inputSynapses).values().stream()
                .filter(s -> !s.isNegative() && !actSyns.contains(s))
                .forEach(s -> {
                    Result r = config.synapseEvaluation.evaluate(s, dir ? act : null, dir ? null : act);
                    s.weightDelta -= (float) (config.ltdLearnRate * act.getFinalState().value * r.significance);

                    if(r.deleteIfNull && s.weight - s.weightDelta <= 0.0) {
                        s.toBeDeleted = true;
                    }

                    if (dir) {
                        doc.notifyWeightsModified(s.output.get(), Collections.singletonList(s));
                    }
                });

        if(!dir) {
            doc.notifyWeightsModified(n, n.inputSynapses.values());
        }
    }
}
