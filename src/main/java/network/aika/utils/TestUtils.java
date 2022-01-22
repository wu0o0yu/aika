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
package network.aika.utils;

import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.ConjunctiveNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.text.Document;


/**
 *
 * @author Lukas Molzberger
 */
public class TestUtils {

    public static Config getConfig() {
        return new Config() {
            public String getLabel(Activation<?> act) {
                Neuron n = act.getNeuron();
                Activation iAct = act.getInputLinks()
                        .findFirst()
                        .map(Link::getInput)
                        .orElse(null);

                if(n instanceof BindingNeuron) {
                    return "B-" + trimPrefix(iAct.getLabel());
                } else if (n instanceof PatternNeuron) {
                    return "P-" + ((Document)act.getThought()).getContent();
                } else {
                    return "I-" + trimPrefix(iAct.getLabel());
                }
            }
        };
    }

    private static String trimPrefix(String l) {
        return l.substring(l.indexOf("-") + 1);
    }

    public static <N extends Neuron> N createNeuron(Neuron templateNeuron, String label, boolean inputNeuron) {
        Neuron n = createNeuron(templateNeuron, label);
        n.setInputNeuron(inputNeuron);
        return (N) n;
    }

    public static <N extends Neuron> N createNeuron(Neuron templateNeuron, String label) {
        Neuron n = templateNeuron.instantiateTemplate(true);
        n.setLabel(label);
        return (N) n;
    }

    public static void updateBias(Neuron n, double bias) {
        n.getBias().addAndTriggerUpdate(bias);

        if(n instanceof BindingNeuron) {
            BindingNeuron bn = (BindingNeuron) n;
            bn.getFinalBias().addAndTriggerUpdate(bias);
        }
    }

    public static Synapse createSynapse(Synapse templateSynapse, Neuron input, Neuron output, double weight) {
        Synapse s = templateSynapse.instantiateTemplate(input, output);

        s.linkInput();
        s.getWeight().setInitialValue(weight);
        if(output instanceof ConjunctiveNeuron) {
            s.linkOutput();

            if(weight >= 0.0) {
                output.getBias().addAndTriggerUpdate(-weight);

                if (output instanceof BindingNeuron) {
                    BindingNeuron bn = (BindingNeuron) output;
                    bn.getFinalBias().addAndTriggerUpdate(-weight);
                }
            }
        }
        return s;
    }

    public static void setStatistic(Neuron n, double frequency, int N, long lastPosition) {
        n.setFrequency(frequency);
        n.getSampleSpace().setN(N);
        n.getSampleSpace().setLastPosition(lastPosition);
    }
}
