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
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.ConjunctiveNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.PositiveFeedbackSynapse;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.neuron.disjunctive.InhibitoryNeuron;
import network.aika.text.Document;
import network.aika.neuron.activation.text.TokenActivation;

import java.util.ArrayList;
import java.util.List;

import static network.aika.steps.Phase.PROCESSING;


/**
 *
 * @author Lukas Molzberger
 */
public class TestUtils {

    public static void processTokens(SimpleTemplateGraph tg, Document doc, Iterable<String> tokens) {
        int i = 0;
        int pos = 0;

        List<TokenActivation> tokenActs = new ArrayList<>();
        for(String t: tokens) {
            int j = i + t.length();

            tokenActs.add(
                    addToken(tg, doc, t, pos++, i,  j)
            );

            i = j + 1;
        }

        for(TokenActivation tAct: tokenActs) {
            tAct.setNet(10.0);
            doc.process(PROCESSING);
        }

        doc.updateModel();
    }

    public static TokenActivation addToken(SimpleTemplateGraph tg, Document doc, String t, Integer pos, int i, int j) {
        return doc.addToken(
                tg.TOKEN_TEMPLATE.lookupToken(t),
                pos++,
                i,
                j
        );
    }

    public static Config getConfig() {
        return new Config() {
            public String getLabel(BindingSignal bs) {
                Activation iAct = bs.getOriginActivation();
                Activation act = bs.getActivation();
                Neuron n = act.getNeuron();

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

    public static CategoryNeuron initCategory(SimpleTemplateGraph t, String label, PatternNeuron... inputPatterns) {
        CategoryNeuron categoryN = createNeuron(t.CATEGORY_TEMPLATE, "C-" + label);

        for (PatternNeuron pn : inputPatterns) {
            createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, pn, categoryN, 1.0);
        }

        return categoryN;
    }

    public static InhibitoryNeuron initInhibitoryLoop(SimpleTemplateGraph t, String label, BindingNeuron... bns) {
        InhibitoryNeuron inhibN = createNeuron(t.INHIBITORY_TEMPLATE, "I-" + label);

        for(BindingNeuron bn: bns) {
            createSynapse(t.INHIBITORY_SYNAPSE_TEMPLATE, bn, inhibN, 1.0);
            createSynapse(t.NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE, inhibN, bn, -100.0);
        }
        return inhibN;
    }

    public static InhibitoryNeuron addInhibitoryLoop(SimpleTemplateGraph t, InhibitoryNeuron inhibN, BindingNeuron... bns) {
        for(BindingNeuron bn: bns) {
            createSynapse(t.INHIBITORY_SYNAPSE_TEMPLATE, bn, inhibN, 1.0);
            createSynapse(t.NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE, inhibN, bn, -100.0);
        }
        return inhibN;
    }

    public static PatternNeuron initPatternLoop(SimpleTemplateGraph t, String label, BindingNeuron... bns) {
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
        n.getBias().receiveUpdate(bias);
    }

    public static PositiveFeedbackSynapse createPositiveFeedbackSynapse(PositiveFeedbackSynapse templateSynapse, PatternNeuron input, BindingNeuron output, double weight, double feedbackWeight) {
        PositiveFeedbackSynapse s = (PositiveFeedbackSynapse) templateSynapse.instantiateTemplate(null, input, output);

        s.setWeight(weight);

        s.linkInput();
        s.linkOutput();
        s.getOutput().getBias().receiveUpdate(-weight);
        s.getWeight().receiveUpdate(feedbackWeight);
        return s;
    }

    public static <S extends Synapse> S createSynapse(Synapse templateSynapse, Neuron input, Neuron output, double weight) {
        Synapse s = templateSynapse.instantiateTemplate(null, input, output);

        s.setWeight(weight);

        s.linkInput();
        if(output instanceof ConjunctiveNeuron) {
            s.linkOutput();
            if(weight > 0.0)
                s.getOutput().getBias().receiveUpdate(-weight);
        }

        return (S) s;
    }

    public static void setStatistic(Neuron n, double frequency, int N, long lastPosition) {
        n.setFrequency(frequency);
        n.getSampleSpace().setN(N);
        n.getSampleSpace().setLastPosition(lastPosition);
    }
}
