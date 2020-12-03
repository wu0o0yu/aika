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
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

import java.util.Set;


import static network.aika.neuron.Templates.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Lukas Molzberger
 */
public class MutualExclusionTest {

    @Test
    public void testPropagation() {
        Model m = new TextModel();

        PatternNeuron in = INPUT_PATTERN_TEMPLATE.instantiateTemplate();
        in.setTokenLabel("I");
        in.setInputNeuron(true);
        in.setLabel("IN");
        PatternPartNeuron na = PATTERN_PART_TEMPLATE.instantiateTemplate();
        na.setLabel("A");
        PatternPartNeuron nb = PATTERN_PART_TEMPLATE.instantiateTemplate();
        nb.setLabel("B");
        PatternPartNeuron nc = PATTERN_PART_TEMPLATE.instantiateTemplate();
        nc.setLabel("C");
        InhibitoryNeuron inhib = INHIBITORY_TEMPLATE.instantiateTemplate();
        inhib.setLabel("I");

        {
            {
                PatternPartSynapse s = PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, na);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                na.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, na);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            na.setBias(1.0);
        }

        {
            {
                PatternPartSynapse s = PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, nb);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nb.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, nb);

                s.linkOutput();
                s.addWeight(-100.0);
            }
            nb.setBias(1.5);
        }


        {
            {
                PatternPartSynapse s = PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, nc);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nc.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, nc);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            nc.setBias(1.2);
        }

        {
            {
                InhibitorySynapse s = INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(na, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                InhibitorySynapse s = INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(nb, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                InhibitorySynapse s = INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(nc, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }

            inhib.setBias(0.0);
        }


        Document doc = new Document("test");

        Activation act = new Activation(doc, in);
        act.setValue(1.0);
        act.setFired(0);

        act.initInput(new TextReference(doc, 0, 4));

        doc.process(m);

        System.out.println(doc.activationsToString());

        Set<Activation> nbActs = doc.getActivations(nb);
        Activation nbAct = nbActs.iterator().next();

        assertTrue(nbAct.getValue() > 0.38);
    }



    @Test
    public void testPropagationWithPrimaryLink() {
        Model m = new TextModel();

        PatternNeuron in = INPUT_PATTERN_TEMPLATE.instantiateTemplate();
        in.setTokenLabel("I");
        in.setInputNeuron(true);
        in.setLabel("IN");
        PatternPartNeuron na = PATTERN_PART_TEMPLATE.instantiateTemplate();
        na.setLabel("A");
        PatternPartNeuron nb = PATTERN_PART_TEMPLATE.instantiateTemplate();
        nb.setLabel("B");
        InhibitoryNeuron inhib = INHIBITORY_TEMPLATE.instantiateTemplate();
        inhib.setLabel("I");

        {
            {
                PatternPartSynapse s = PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, na);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                na.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, na);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            na.setBias(1.0);
        }

        {
            {
                PatternPartSynapse s = PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, nb);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nb.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, nb);

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
                InhibitorySynapse s = new InhibitorySynapse(na, inhib, null);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                InhibitorySynapse s = new InhibitorySynapse(nb, inhib, null);
                s.linkInput();
                s.addWeight(1.0);
            }

            inhib.setBias(0.0);
        }


        Document doc = new Document("test");

        Activation act = new Activation(doc, in);
        act.initInput(new TextReference(doc, 0, 4));

        doc.process(m);

        System.out.println(doc.activationsToString());
        System.out.println();
    }
}
