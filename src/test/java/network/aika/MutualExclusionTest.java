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

import network.aika.debugger.AikaDebugger;
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
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
        m.init();
        Templates t = m.getTemplates();

        PatternNeuron in = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);
        in.setTokenLabel("I");
        in.setInputNeuron(true);
        in.setLabel("IN");
        BindingNeuron na = t.SAME_BINDING_TEMPLATE.instantiateTemplate(true);
        na.setLabel("A");
        BindingNeuron nb = t.SAME_BINDING_TEMPLATE.instantiateTemplate(true);
        nb.setLabel("B");
        BindingNeuron nc = t.SAME_BINDING_TEMPLATE.instantiateTemplate(true);
        nc.setLabel("C");
        InhibitoryNeuron inhib = t.INHIBITORY_TEMPLATE.instantiateTemplate(true);
        inhib.setLabel("I");

        {
            {
                Synapse s = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, na);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                na.addConjunctiveBias(-10.0);
            }

            {
                Synapse s = t.NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, na);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            na.addBias(1.0);
        }

        {
            {
                Synapse s = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, nb);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nb.addConjunctiveBias(-10.0);
            }

            {
                Synapse s = t.NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, nb);

                s.linkOutput();
                s.addWeight(-100.0);
            }
            nb.addBias(1.5);
        }


        {
            {
                Synapse s = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, nc);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nc.addConjunctiveBias(-10.0);
            }

            {
                Synapse s = t.NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, nc);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            nc.addBias(1.2);
        }

        {
            {
                Synapse s = t.INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(na, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                Synapse s = t.INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(nb, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                Synapse s = t.INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(nc, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }

            inhib.addBias(0.0);
        }


        Document doc = new Document("test");

        Config c = new TestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setEnableTraining(true);
        doc.setConfig(c);

        doc.addInput(in, new TextReference(doc, 0, 4));

        doc.process(m);

        System.out.println(doc);

        Set<Activation> nbActs = doc.getActivations(nb);
        Activation nbAct = nbActs.iterator().next();

        assertTrue(nbAct.getValue() > 0.38);
    }



    @Test
    public void testPropagationWithPrimaryLink() {
        Model m = new TextModel();
        m.init();
        Templates t = m.getTemplates();

        PatternNeuron in = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);
        in.setTokenLabel("I");
        in.setInputNeuron(true);
        in.setLabel("IN");
        BindingNeuron na = t.SAME_BINDING_TEMPLATE.instantiateTemplate(true);
        na.setLabel("A");
        BindingNeuron nb = t.SAME_BINDING_TEMPLATE.instantiateTemplate(true);
        nb.setLabel("B");
        InhibitoryNeuron inhib = t.INHIBITORY_TEMPLATE.instantiateTemplate(true);
        inhib.setLabel("I");

        {
            {
                Synapse s = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, na);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                na.addConjunctiveBias(-10.0);
            }

            {
                Synapse s = t.NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, na);

                s.linkOutput();
                s.addWeight(-100.0);
            }

            na.addBias(1.0);
        }

        {
            {
                Synapse s = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, nb);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                nb.addConjunctiveBias(-10.0);
            }

            {
                Synapse s = t.NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhib, nb);

                s.linkOutput();
                s.addWeight(-100.0);
            }
            nb.addBias(1.5);
        }

        {
/*            {
                InhibitorySynapse s = new InhibitorySynapse(in, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
*/
            {
                Synapse s = t.INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(na, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }
            {
                Synapse s = t.INHIBITORY_SYNAPSE_TEMPLATE.instantiateTemplate(nb, inhib);
                s.linkInput();
                s.addWeight(1.0);
            }

            inhib.addBias(0.0);
        }


        Document doc = new Document("test");

        Config c = new TestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setEnableTraining(true);
        doc.setConfig(c);

        doc.addInput(in, new TextReference(doc, 0, 4));

        AikaDebugger.createAndShowGUI(doc,m);

        doc.process(m);

        System.out.println(doc);
        System.out.println();
    }
}
