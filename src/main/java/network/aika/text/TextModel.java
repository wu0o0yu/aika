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
import network.aika.SuspensionHook;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;

/**
 *
* @author Lukas Molzberger
*/
public class TextModel extends Model {

    public InhibitoryNeuron prevTokenInhib;
    public InhibitoryNeuron nextTokenInhib;

    public TextModel() {
        super();
        init();
    }

    public TextModel(SuspensionHook sh) {
        super(sh);
        init();
    }

    private void init() {
        prevTokenInhib = new InhibitoryNeuron(this, "Prev. Token", false);
        nextTokenInhib = new InhibitoryNeuron(this, "Next Token", false);
    }

    @Override
    public void linkInputRelations(Activation originAct, Direction dir) {
        Document doc = (Document) originAct.getThought();
        Cursor lc = doc.getLastCursor();
        if(lc == null) return;

        switch (dir) {
            case OUTPUT:
                if (originAct.getNeuron() == prevTokenInhib && lc.nextTokenPPAct != null) {
                    Synapse s = ((ExcitatoryNeuron) lc.nextTokenPPAct.getNeuron()).getInputSynapse();
                    doc.add(new Link(s, originAct, lc.nextTokenPPAct));
                }
                break;

            case INPUT: {
                Neuron n = originAct.getNeuron();
                if (n instanceof ExcitatoryNeuron) {
                    Synapse s = ((ExcitatoryNeuron) n).getInputSynapse();

                    if (s != null) {
                        if (originAct.getOutputLinks(prevTokenInhib.getProvider()) != null && lc.nextTokenIAct != null) {
                            doc.add(new Link(s, lc.nextTokenIAct, originAct));
                        }
                    }
                }
                break;
            }
        }
    }

    public PatternNeuron lookupToken(String label) {
        Neuron inProv = getNeuron(label);
        if(inProv != null) {
            return (PatternNeuron) inProv;
        }

        PatternNeuron in = new PatternNeuron(this, label, true);
        getSuspensionHook().putLabel(label, in.getId());

        PatternPartNeuron inRelPW = new PatternPartNeuron(this, label + " Rel Prev. Word", true);
        PatternPartNeuron inRelNW = new PatternPartNeuron(this, label + " Rel Next Word", true);

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(in, inRelPW);
                s.setPropagate(true);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(nextTokenInhib, inRelPW);
                s.setInput(true);

                s.link();
                s.update(10.0, false);
            }
            inRelPW.setBias(4.0);
        }
        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(in, inRelNW);
                s.setPropagate(true);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(prevTokenInhib, inRelNW);
                s.setInput(true);

                s.link();
                s.update(10.0, true);
            }
            inRelNW.setBias(4.0);
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelPW, prevTokenInhib);

            s.link();
            s.update(1.0, false);
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelNW, nextTokenInhib);

            s.link();
            s.update(1.0, false);
        }

        return in;
    }

    public InhibitoryNeuron getPrevTokenInhib() {
        return prevTokenInhib;
    }

    public InhibitoryNeuron getNextTokenInhib() {
        return nextTokenInhib;
    }
}
