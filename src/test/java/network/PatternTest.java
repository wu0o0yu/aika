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
package network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.*;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternTest {

    @Test
    public void testPattern() {
        Model m = new Model();

        InhibitoryNeuron prevWordInhib = new InhibitoryNeuron(m, "INPUT PW INHIB", true);
        InhibitoryNeuron nextWordInhib = new InhibitoryNeuron(m, "INPUT NW INHIB", true);

        Neuron[] inputA = initInput(m, prevWordInhib, nextWordInhib, "A");
        Neuron[] inputB = initInput(m, prevWordInhib, nextWordInhib, "B");
        Neuron[] inputC = initInput(m, prevWordInhib, nextWordInhib, "C");


        PatternPartNeuron eA = new PatternPartNeuron(m, "E A", false);
        PatternPartNeuron eB = new PatternPartNeuron(m, "E B", false);
        PatternPartNeuron eC = new PatternPartNeuron(m, "E C", false);

        PatternNeuron out = new PatternNeuron(m, "OUT", false);

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(inputA[0], eA);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(out, eA);

                s.link();
                s.update(10.0);
            }
            eA.setBias(4.0);
            eA.commit();
        }

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(inputB[0], eB);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(eA, eB);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(inputB[1], eB);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(out, eB);

                s.link();
                s.update(10.0);
            }
            eB.setBias(4.0);
            eB.commit();
        }

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(inputC[0], eC);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(eA, eC);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(inputC[1], eC);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(out, eC);

                s.link();
                s.update(10.0);
            }
            eC.setBias(4.0);
            eC.commit();
        }

        {
            {
                PatternSynapse s = new PatternSynapse(eA, out);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }
            {
                PatternSynapse s = new PatternSynapse(eB, out);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }
            {
                PatternSynapse s = new PatternSynapse(eC, out);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }
            eC.setBias(4.0);
            eC.commit();
        }

        Document doc = new Document("ABC");

        Activation wordAAct = addWordActivation(doc, inputA, prevWordInhib, nextWordInhib, null);
        Activation wordBAct = addWordActivation(doc, inputB, prevWordInhib, nextWordInhib, wordAAct);
        Activation wordCAct = addWordActivation(doc, inputC, prevWordInhib, nextWordInhib, wordBAct);

        doc.process();

        System.out.println(doc.activationsToString());
    }


    private Neuron[] initInput(Model m, InhibitoryNeuron prevWordInhib, InhibitoryNeuron nextWordInhib, String label) {
        PatternNeuron in = new PatternNeuron(m, label, true);
        PatternPartNeuron inRelPW = new PatternPartNeuron(m, label + "Rel Prev. Word", true);
        PatternPartNeuron inRelNW = new PatternPartNeuron(m, label + "Rel Next Word", true);

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelPW);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(nextWordInhib, inRelPW);

                s.link();
                s.update(10.0);
            }
            inRelPW.setBias(4.0);
            inRelPW.commit();
        }
        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelNW);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(prevWordInhib, inRelNW);

                s.link();
                s.update(10.0);
            }
            inRelNW.setBias(4.0);
            inRelNW.commit();
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelPW, prevWordInhib);

            s.link();
            s.update(1.0);
            s.commit();
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelNW, nextWordInhib);

            s.link();
            s.update(1.0);
            s.commit();
        }

        return new Neuron[] {in, inRelPW, inRelNW};
    }

    private Activation addWordActivation(Document doc, Neuron[] n, InhibitoryNeuron prevWordInhib, InhibitoryNeuron nextWordInhib, Activation previousPatternAct) {
        Activation patternAct = n[0].propagate(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
        );

        Activation prevRelAct = n[1].propagate(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
                        .addInputLink(patternAct)
        );

        Activation nextRelAct = n[2].propagate(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
                        .addInputLink(previousPatternAct)
        );

        Activation inhibNWAct = lookupInhibRelAct(nextWordInhib, nextRelAct);
        Activation inhibPWAct = lookupInhibRelAct(prevWordInhib, prevRelAct);

        prevRelAct.addLink(
                new Link(
                        prevRelAct.getNeuron().getInputSynapse(inhibNWAct.getNeuronProvider()),
                        inhibNWAct,
                        prevRelAct
                )
        );
        prevRelAct.linkForward();

        nextRelAct.addLink(
                new Link(
                        prevRelAct.getNeuron().getInputSynapse(inhibNWAct.getNeuronProvider()),
                        inhibPWAct,
                        nextRelAct
                )
        );
        nextRelAct.linkForward();

        doc.processActivations();

        return patternAct;
    }

    private Activation lookupInhibRelAct(InhibitoryNeuron inhiN, Activation relPPAct) {
        if(relPPAct == null) {
            return null;
        }
        return relPPAct.getOutputLinks(inhiN.getProvider())
                .findAny()
                .map(l -> l.getOutput())
                .orElse(null);
    }
}
