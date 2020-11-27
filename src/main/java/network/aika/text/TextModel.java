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
        InhibitoryNeuron ptN = new InhibitoryNeuron(this);
        ptN.setInputNeuron(true);
        ptN.setDescriptionLabel("Prev. Token");
        prevTokenInhib = ptN.getProvider();
        prevTokenInhib.save();

        InhibitoryNeuron ntN = new InhibitoryNeuron(this);
        ntN.setInputNeuron(true);
        ntN.setDescriptionLabel("Next Token");
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

        PatternNeuron in = new PatternNeuron(this, tokenLabel);
        in.setInputNeuron(true);
        in.setDescriptionLabel("P-" + tokenLabel);
        getSuspensionHook().putLabel(tokenLabel, in.getId());

        PatternPartNeuron inRelPW = new PatternPartNeuron(this);
        inRelPW.setInputNeuron(true);
        inRelPW.setDescriptionLabel(tokenLabel + " Rel Prev. Word");

        PatternPartNeuron inRelNW = new PatternPartNeuron(this);
        inRelNW.setInputNeuron(true);
        inRelNW.setDescriptionLabel(tokenLabel + " Rel Next Word");

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelPW, null);
                s.setRecurrent(true);

                s.linkInput();
                s.linkOutput();
                s.setWeight(11.0);
                inRelPW.addConjunctiveBias(-11.0, false);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(getNextTokenInhib(), inRelPW, null);
                s.setInputScope(true);

                s.linkOutput();
                s.addWeight(10.0);
                inRelPW.addConjunctiveBias(-10.0, false);
            }
            inRelPW.setBias(4.0);
        }
        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelNW, null);
                s.setRecurrent(true);

                s.linkInput();
                s.linkOutput();
                s.addWeight(11.0);
                inRelNW.addConjunctiveBias(-11.0, false);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(getPrevTokenInhib(), inRelNW, null);
                s.setInputScope(true);

                s.linkOutput();
                s.addWeight(10.0);
                inRelNW.addConjunctiveBias(-10.0, true);
            }
            inRelNW.setBias(4.0);
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelPW, getPrevTokenInhib(), null);

            s.linkInput();
            s.addWeight(1.0);
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelNW, getNextTokenInhib(), null);

            s.linkInput();
            s.addWeight(1.0);
        }

        in.getProvider().save();
        inRelPW.getProvider().save();
        inRelNW.getProvider().save();

        return in;
    }

    public InhibitoryNeuron getPrevTokenInhib() {
        return (InhibitoryNeuron) prevTokenInhib.getNeuron();
    }

    public InhibitoryNeuron getNextTokenInhib() {
        return (InhibitoryNeuron) nextTokenInhib.getNeuron();
    }
}
