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
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;

/**
 *
* @author Lukas Molzberger
*/
public class TextModel extends Model {

    public InhibitoryNeuron prevTokenInhib;
    public InhibitoryNeuron nextTokenInhib;


    @Override
    public void linkInputRelations(PatternPartSynapse s, Activation originAct) {
        if(s.getInput() != nextTokenInhib) {
            return;
        }

        Document doc = (Document) originAct.getThought();
        Cursor c = doc.getCursor();

        if(c.previousNextTokenAct != null) {
            Link l = new Link(s, c.previousNextTokenAct, originAct);
            doc.add(l);
        }
    }

    public PatternNeuron initToken(String label) {
        NeuronProvider inProv = getNeuron(label);
        if(inProv != null) {
            return (PatternNeuron) inProv.getNeuron();
        }

        PatternNeuron in = new PatternNeuron(this, label, true);
        PatternPartNeuron inRelPW = new PatternPartNeuron(this, label + "Rel Prev. Word", true);
        PatternPartNeuron inRelNW = new PatternPartNeuron(this, label + "Rel Next Word", true);

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelPW);
                s.setPropagate(true);

                s.link();
                s.setWeight(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(nextTokenInhib, inRelPW);

                s.link();
                s.setWeight(10.0);
            }
            inRelPW.setBias(4.0);
        }
        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelNW);
                s.setPropagate(true);

                s.link();
                s.setWeight(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(prevTokenInhib, inRelNW);

                s.link();
                s.setWeight(10.0);
            }
            inRelNW.setBias(4.0);
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelPW, prevTokenInhib);

            s.link();
            s.setWeight(1.0);
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelNW, nextTokenInhib);

            s.link();
            s.setWeight(1.0);
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
