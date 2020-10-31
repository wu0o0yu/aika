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

import network.aika.neuron.excitatory.*;
import network.aika.text.Document;
import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;

import java.util.Set;


import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Lukas Molzberger
 */
public class MutualExclusionTest {

    @Test
    public void testPropagation() {
        Model m = new TextModel();

        PatternNeuron in = new PatternNeuron(m, "I", true);
        in.setDescriptionLabel("IN");
        PatternPartNeuron na = new PatternPartNeuron(m, false);
        na.setDescriptionLabel("A");
        PatternPartNeuron nb = new PatternPartNeuron(m, false);
        nb.setDescriptionLabel("B");
        PatternPartNeuron nc = new PatternPartNeuron(m, false);
        nc.setDescriptionLabel("C");
        InhibitoryNeuron inhib = new InhibitoryNeuron(m, false);
        inhib.setDescriptionLabel("I");

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(in, na);
                s.setInputScope(true);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                na.addConjunctiveBias(-10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(inhib, na);
                s.setNegative(true);
                s.setRecurrent(true);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            na.setBias(1.0);
        }

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(in, nb);
                s.setInputScope(true);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nb.addConjunctiveBias(-10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(inhib, nb, true, true, false, false);

                s.linkOutput();
                s.addWeight(-100.0);
            }
            nb.setBias(1.5);
        }


        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(in, nc, false, false, true, false);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nc.addConjunctiveBias(-10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(inhib, nc, true, true, false, false);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            nc.setBias(1.2);
        }

        {
            {
                InhibitorySynapse s = new InhibitorySynapse(na, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                InhibitorySynapse s = new InhibitorySynapse(nb, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                InhibitorySynapse s = new InhibitorySynapse(nc, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }

            inhib.setBias(0.0);
        }


        Document doc = new Document("test");

        Activation act = new Activation(doc, in);
        act.setValue(1.0);
        act.setFired(0);

        act.propagateInput();

        doc.process();

        System.out.println(doc.activationsToString());

        Set<Activation> nbActs = doc.getActivations(nb);
        Activation nbAct = nbActs.iterator().next();

        assertTrue(nbAct.getValue() > 0.38);
    }



    @Test
    public void testPropagationWithPrimaryLink() {
        Model m = new TextModel();

        PatternNeuron in = new PatternNeuron(m, "I", true);
        in.setDescriptionLabel("IN");
        PatternPartNeuron na = new PatternPartNeuron(m, false);
        na.setDescriptionLabel("A");
        PatternPartNeuron nb = new PatternPartNeuron(m, false);
        nb.setDescriptionLabel("B");
        InhibitoryNeuron inhib = new InhibitoryNeuron(m, false);
        inhib.setDescriptionLabel("I");

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(in, na, false, false, true, false);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                na.addConjunctiveBias(-10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(inhib, na, true, true, false, false);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            na.setBias(1.0);
        }

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(in, nb, false, false, true, false);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nb.addConjunctiveBias(-10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(inhib, nb, true, true, false, false);

                s.linkOutput();
                s.addWeight(-100.0);
            }
            nb.setBias(1.5);
        }

        {
/*            {
                InhibitorySynapse s = new InhibitorySynapse(in, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
*/
            {
                InhibitorySynapse s = new InhibitorySynapse(na, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                InhibitorySynapse s = new InhibitorySynapse(nb, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }

            inhib.setBias(0.0);
        }


        Document doc = new Document("test");

        Activation act = new Activation(doc, in);
        act.setValue(1.0);

        act.propagateInput();

        doc.process();

        System.out.println(doc.activationsToString());
        System.out.println();
    }
}
