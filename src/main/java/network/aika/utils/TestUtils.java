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
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.axons.PatternAxon;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.ConjunctiveNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.PositiveFeedbackSynapse;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.neuron.disjunctive.InhibitoryNeuron;
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
                }else if (n instanceof CategoryNeuron) {
                    return "C-" + ((Document)act.getThought()).getContent();
                } else {
                    return "I-" + trimPrefix(iAct.getLabel());
                }
            }
        };
    }

    private static String trimPrefix(String l) {
        return l.substring(l.indexOf("-") + 1);
    }

    public static CategoryNeuron initCategory(Templates t, String label, PatternNeuron... inputPatterns) {
        CategoryNeuron categoryN = createNeuron(t.CATEGORY_TEMPLATE, "C-" + label);

        for (PatternNeuron pn : inputPatterns) {
            createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, pn, categoryN, 1.0);
        }

        return categoryN;
    }

    public static InhibitoryNeuron initInhibitoryLoop(Templates t, String label, BindingNeuron... bns) {
        InhibitoryNeuron inhibN = createNeuron(t.INHIBITORY_TEMPLATE, "I-" + label);

        for(BindingNeuron bn: bns) {
            createSynapse(t.INHIBITORY_SYNAPSE_TEMPLATE, bn, inhibN, 1.0);
            createSynapse(t.NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE, inhibN, bn, -100.0);
        }
        return inhibN;
    }

    public static PatternNeuron initPatternLoop(Templates t, String label, BindingNeuron... bns) {
        PatternNeuron patternN = createNeuron(t.PATTERN_TEMPLATE, "P-" + label);

        for(BindingNeuron bn: bns) {
            createSynapse(t.PATTERN_SYNAPSE_TEMPLATE, bn, patternN, 10.0);
            createPositiveFeedbackSynapse(t.POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE, patternN, bn, 0.0, 10.0);
        }
        return patternN;
    }

    public static <N extends Neuron> N createNeuron(Neuron templateNeuron, String label, boolean inputNeuron) {
        Neuron n = createNeuron(templateNeuron, label);
        n.setNetworkInput(inputNeuron);
        return (N) n;
    }

    public static <N extends Neuron> N createNeuron(Neuron templateNeuron, String label) {
        Neuron n = templateNeuron.instantiateTemplate(true);
        n.setLabel(label);
        return (N) n;
    }

    public static void updateBias(Neuron n, double bias) {
        n.getBias().addAndTriggerUpdate(bias);

        n.updateAllowPropagate();
    }

    public static PositiveFeedbackSynapse createPositiveFeedbackSynapse(PositiveFeedbackSynapse templateSynapse, PatternNeuron input, BindingNeuron output, double weight, double feedbackWeight) {
        PositiveFeedbackSynapse s = (PositiveFeedbackSynapse) templateSynapse.instantiateTemplate(input, output);

        s.setWeight(weight);

        s.linkInput();
        s.linkOutput();
        s.getOutput().getBias().addAndTriggerUpdate(-weight);
        s.getFeedbackWeight().setAndTriggerUpdate(feedbackWeight);
        s.getFeedbackBias().addAndTriggerUpdate(-feedbackWeight);
        output.updateAllowPropagate();
        return s;
    }

    public static <S extends Synapse> S createSynapse(Synapse templateSynapse, Neuron input, Neuron output, double weight) {
        Synapse s = templateSynapse.instantiateTemplate(input, output);

        s.setWeight(weight);

        s.linkInput();
        if(output instanceof ConjunctiveNeuron) {
            s.linkOutput();
            if(weight > 0.0)
                s.getOutput().getBias().addAndTriggerUpdate(-weight);
        }

        output.updateAllowPropagate();
        return (S) s;
    }

    public static void setStatistic(Neuron n, double frequency, int N, long lastPosition) {
        n.setFrequency(frequency);
        n.getSampleSpace().setN(N);
        n.getSampleSpace().setLastPosition(lastPosition);
    }
}
