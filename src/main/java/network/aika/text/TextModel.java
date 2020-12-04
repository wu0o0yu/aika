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
package network.aika.text;

import network.aika.Model;
import network.aika.neuron.activation.Reference;
import network.aika.SuspensionHook;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.excitatory.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;

import static network.aika.neuron.Templates.*;


/**
 *
* @author Lukas Molzberger
*/
public class TextModel extends Model {

    public NeuronProvider prevTokenInhib;
    public NeuronProvider nextTokenInhib;

    public TextModel() {
        super();
        init();
    }

    public TextModel(SuspensionHook sh) {
        super(sh);
        init();
    }

    private void init() {
        InhibitoryNeuron ptN = getTemplates().INHIBITORY_TEMPLATE.instantiateTemplate();
        ptN.setInputNeuron(true);
        ptN.setLabel("Prev. Token");
        prevTokenInhib = ptN.getProvider();
        prevTokenInhib.save();

        InhibitoryNeuron ntN = getTemplates().INHIBITORY_TEMPLATE.instantiateTemplate();
        ntN.setInputNeuron(true);
        ntN.setLabel("Next Token");
        nextTokenInhib = ntN.getProvider();
        nextTokenInhib.save();
    }

    @Override
    public void linkInputRelations(Activation originAct, Direction dir) {
        TextReference ref = originAct.getReference();
        TextReference lastRef = ref.getPrevious();
        if(lastRef == null) return;

        switch (dir) {
            case OUTPUT:
                if (originAct.getNeuron().isInputNeuron() && prevTokenInhib.getId().equals(originAct.getNeuron().getId()) && lastRef.nextTokenPPAct != null) {
                    Synapse s = getRelSynapse(lastRef.nextTokenPPAct.getNeuron());
                    lastRef.nextTokenPPAct.addLink(s, originAct, false);
                }
                break;

            case INPUT: {
                Neuron n = originAct.getNeuron();
                if (n instanceof ExcitatoryNeuron) {
                    Synapse s = getRelSynapse(n);

                    if (s != null) {
                        if (isPrevTokenPatternPart(originAct.getNeuron()) && lastRef.nextTokenIAct != null) {
                            originAct.addLink(s, lastRef.nextTokenIAct, false);
                        }
                    }
                }
                break;
            }
        }
    }

    private Synapse getRelSynapse(Neuron<?> n) {
        return n.getInputSynapses()
                .filter(s -> s instanceof PatternPartSynapse)
                .map(s -> (PatternPartSynapse) s)
                .filter(s -> s.isInputScope())
                .findAny()
                .orElse(null);
    }

    private boolean isPrevTokenPatternPart(Neuron<?> n) {
        return n.getOutputSynapses()
                .anyMatch(s -> prevTokenInhib.getId().equals(s.getOutput().getId()));
    }

    public PatternNeuron lookupToken(Reference ref, String tokenLabel) {
        Neuron inProv = getNeuron(tokenLabel);
        if(inProv != null) {
            return (PatternNeuron) inProv;
        }

        PatternNeuron in = getTemplates().INPUT_PATTERN_TEMPLATE.instantiateTemplate();
        in.setTokenLabel(tokenLabel);
        in.setInputNeuron(true);
        in.setLabel("P-" + tokenLabel);
        getSuspensionHook().putLabel(tokenLabel, in.getId());

        PatternPartNeuron inRelPT = getTemplates().PATTERN_PART_TEMPLATE.instantiateTemplate();
        inRelPT.setInputNeuron(true);
        inRelPT.setLabel(tokenLabel + " Rel Prev. Token");

        PatternPartNeuron inRelNT = getTemplates().PATTERN_PART_TEMPLATE.instantiateTemplate();
        inRelNT.setInputNeuron(true);
        inRelNT.setLabel(tokenLabel + " Rel Next Token");

        {
            {
                PatternPartSynapse s = getTemplates().RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(in, inRelPT);

                s.linkInput();
                s.linkOutput();
                s.setWeight(11.0);
                inRelPT.addConjunctiveBias(-11.0, false);
            }

            {
                PatternPartSynapse s = getTemplates().RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE.instantiateTemplate(getNextTokenInhib(), inRelPT);

                s.linkOutput();
                s.addWeight(10.0);
                inRelPT.addConjunctiveBias(-10.0, false);
            }
            inRelPT.setBias(4.0);
        }
        {
            {
                PatternPartSynapse s = getTemplates().RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(in, inRelNT);

                s.linkInput();
                s.linkOutput();
                s.addWeight(11.0);
                inRelNT.addConjunctiveBias(-11.0, false);
            }

            {
                PatternPartSynapse s = getTemplates().RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE.instantiateTemplate(getPrevTokenInhib(), inRelNT);

                s.linkOutput();
                s.addWeight(10.0);
                inRelNT.addConjunctiveBias(-10.0, true);
            }
            inRelNT.setBias(4.0);
        }

        {
            InhibitorySynapse s = getTemplates().INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(inRelPT, getPrevTokenInhib());

            s.linkInput();
            s.addWeight(1.0);
        }

        {
            InhibitorySynapse s = getTemplates().INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(inRelNT, getNextTokenInhib());

            s.linkInput();
            s.addWeight(1.0);
        }

        in.getProvider().save();
        inRelPT.getProvider().save();
        inRelNT.getProvider().save();

        return in;
    }

    public InhibitoryNeuron getPrevTokenInhib() {
        return (InhibitoryNeuron) prevTokenInhib.getNeuron();
    }

    public InhibitoryNeuron getNextTokenInhib() {
        return (InhibitoryNeuron) nextTokenInhib.getNeuron();
    }
}
