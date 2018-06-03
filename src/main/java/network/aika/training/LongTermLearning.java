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
        public double learnRate;

        public Config setLearnRate(double learnRate) {
            this.learnRate = learnRate;
            return this;
        }
    }



    public static void train(Document doc, Config config) {
        for(Activation act: doc.getActivations(false)) {
            INeuron.Type t = act.getINeuron().type;
            if(t != null) {
                switch (t) {
                    case EXCITATORY:
                        trainExcitatory(config, act);
                        break;
                    case INHIBITORY:
//                    trainInhibitoryPot(config, act);
                        break;
                }
            }
//            trainInhibitoryDepr(config, act);
        }
        doc.commit();
    }


    private static void trainExcitatory(Config config, Activation act) {
        INeuron n = act.getINeuron();

        double x = (1.0 - act.avgState.value) * Utils.nullSafeMax(act.avgState.value, act.targetValue);

        for(Synapse s: n.inputSynapses.values()) {
            if (!s.isNegative()) {
                double maxValue = 0.0;
                double maxP = 0.0;
                for (Link l : act.neuronInputs.subMap(
                        new Link(s, MIN_ACTIVATION, MIN_ACTIVATION),
                        new Link(s, MAX_ACTIVATION, MAX_ACTIVATION)).values()) {
                    maxValue = Math.max(maxValue, l.input.avgState.value);
                    maxP = Math.max(maxP, l.input.avgState.p);
                }

                double h = act.avgState.net / (n.biasSum + n.posDirSum + n.posRecSum);

                double delta = config.learnRate * x * h * maxValue;
                delta -= config.learnRate * x * h * (1.0 - maxP);

                double biasDelta = -config.learnRate * (act.avgState.posValue - act.avgState.value);

                if(delta != 0.0 || biasDelta != 0.0) {
                    s.updateDelta(act.doc, delta, biasDelta);
                }
            }
        }
    }


    private static void trainInhibitoryPot(Config config, Activation act) {
        double x = (1.0 - act.avgState.value) * Utils.nullSafeMax(act.avgState.value, act.targetValue);

        for (Link l : act.neuronInputs.values()) {
            Activation iAct = l.input;

            double delta = config.learnRate * x * iAct.avgState.value;

            if(delta != 0.0) {
                l.synapse.updateDelta(act.doc, delta, 0.0);
            }
        }
    }


    private static void trainInhibitoryDepr(Config config, Activation act) {
        INeuron n = act.getINeuron();

        double x = -config.learnRate * (1.0 - act.avgState.value) * Utils.nullSafeMax(act.avgState.value, act.targetValue);

        for(Synapse s: n.outputSynapses.values()) {
            INeuron on = s.output.get(act.doc);
            if(!s.isNegative() && on.type == INeuron.Type.INHIBITORY) {
                double maxP = 0.0;
                for(Link l: act.neuronOutputs.subMap(
                        new Link(s, MIN_ACTIVATION, MIN_ACTIVATION),
                        new Link(s, MAX_ACTIVATION, MAX_ACTIVATION)).values()) {
                    Activation rAct = l.output;
                    maxP = Math.max(maxP, rAct.avgState.p);
                }

                double delta = x * (1.0 - maxP);
                if(delta < 0.0) {
                    s.updateDelta(act.doc, delta, 0.0);
                }
            }
        }
    }
}
