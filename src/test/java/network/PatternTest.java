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
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.*;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.PatternInhibitoryNeuron;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternTest {

    @Test
    public void testPattern() {
        Model m = new Model();

        InhibitoryNeuron prevWordInhib = new PatternInhibitoryNeuron(m, "INPUT PW INHIB", true);
        InhibitoryNeuron nextWordInhib = new PatternInhibitoryNeuron(m, "INPUT NW INHIB", true);

        Neuron[] inputA = initInput(m, prevWordInhib, nextWordInhib, "A");
        Neuron[] inputB = initInput(m, prevWordInhib, nextWordInhib, "B");
        Neuron[] inputC = initInput(m, prevWordInhib, nextWordInhib, "C");


        PatternPartNeuron eA = new PatternPartNeuron(m, "E A", false);
        PatternPartNeuron eB = new PatternPartNeuron(m, "E B", false);
        PatternPartNeuron eC = new PatternPartNeuron(m, "E C", false);

        PatternNeuron out = new PatternNeuron(m, "OUT", false);


        eA.link(4.0,
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(inputA[0])
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        eB.link(4.0,
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(inputB[0])
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(eA)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(inputB[1])
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        eC.link(4.0,
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(inputC[0])
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(eB)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(inputC[1])
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        out.link(4.0,
                new PatternSynapse.Builder()
                        .setNeuron(eA)
                        .setPropagate(true)
                        .setWeight(10.0),
                new PatternSynapse.Builder()
                        .setNeuron(eB)
                        .setWeight(10.0),
                new PatternSynapse.Builder()
                        .setNeuron(eC)
                        .setWeight(10.0)
        );


        Document doc = new Document("ABC");

        Activation wordAAct = addWordActivation(doc, inputA, prevWordInhib, nextWordInhib, null);
        Activation wordBAct = addWordActivation(doc, inputB, prevWordInhib, nextWordInhib, wordAAct);
        Activation wordCAct = addWordActivation(doc, inputC, prevWordInhib, nextWordInhib, wordBAct);

        doc.process();

        System.out.println(doc.activationsToString());
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

    private Neuron[] initInput(Model m, InhibitoryNeuron prevWordInhib, InhibitoryNeuron nextWordInhib, String label) {
        PatternNeuron in = new PatternNeuron(m, label, true);
        PatternPartNeuron inRelPW = new PatternPartNeuron(m, label + "Rel Prev. Word", true);
        PatternPartNeuron iARelNW = new PatternPartNeuron(m, label + "Rel Next Word", true);

        inRelPW.link(4.0,
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(in)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(nextWordInhib)
                        .setWeight(10.0)
        );

        iARelNW.link(4.0,
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(in)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(prevWordInhib)
                        .setWeight(10.0)
        );


        prevWordInhib.link(0.0,
                new InhibitorySynapse.Builder()
                        .setNeuron(inRelPW)
                        .setWeight(1.0)
        );

        nextWordInhib.link(0.0,
                new InhibitorySynapse.Builder()
                        .setNeuron(iARelNW)
                        .setWeight(1.0)
        );

        return new Neuron[] {in, inRelPW, iARelNW};
    }
}
