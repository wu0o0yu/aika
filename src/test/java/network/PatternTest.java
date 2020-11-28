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

import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.excitatory.PatternSynapse;
import network.aika.text.Document;
import network.aika.neuron.Neuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

import static network.aika.neuron.Templates.*;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternTest {

    @Test
    public void testPatternPos() {
        TextModel m = initModel();

        Document doc = new Document("ABC");

        TextReference refA = doc.processToken(m, null, 0, 1,  "A").getReference();
        TextReference refB = doc.processToken(m, refA, 1, 2,  "B").getReference();
        TextReference refC = doc.processToken(m, refB, 2, 3,  "C").getReference();

        System.out.println(doc.activationsToString());

        doc.process(m);

        System.out.println(doc.activationsToString());
    }


    @Test
    public void testPatternNeg() {
        TextModel m = initModel();

        Document doc = new Document("ABC");

        TextReference refA = doc.processToken(m, null, 0, 1,  "A").getReference();
        TextReference refB = doc.processToken(m, refA, 1, 2,  "B").getReference();

        doc.process(m);

        System.out.println(doc.activationsToString());
    }

    public TextModel initModel() {
        TextModel m = new TextModel();

        PatternNeuron nA = m.lookupToken(null, "A");
        PatternNeuron nB = m.lookupToken(null, "B");
        PatternNeuron nC = m.lookupToken(null, "C");

        PatternPartNeuron eA = new PatternPartNeuron(m);
        eA.setDescriptionLabel("E A");
        PatternPartNeuron eB = new PatternPartNeuron(m);
        eB.setDescriptionLabel("E B");
        PatternPartNeuron eC = new PatternPartNeuron(m);
        eC.setDescriptionLabel("E C");

        PatternNeuron out = new PatternNeuron(m, "ABC");
        out.setDescriptionLabel("OUT");

        {
            {
                PatternPartSynapse s = PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(nA, eA);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                eA.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(out, eA);

                s.linkInput();
                s.linkOutput();
                s.setWeight(10.0);
                eA.addConjunctiveBias(-10.0, true);
            }
            eA.setBias(4.0);
        }

        {
            {
                PatternPartSynapse s = PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(nB, eB);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                eB.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(eA, eB);

                s.linkOutput();
                s.addWeight(10.0);
                eB.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = RELATED_INPUT_SYNAPSE_FROM_PP_TEMPLATE.instantiateTemplate(lookupPPPT(m, nB), eB);

                s.linkOutput();
                s.addWeight(10.0);
                eB.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(out, eB);

                s.linkOutput();
                s.addWeight(10.0);
                eB.addConjunctiveBias(-10.0, true);
            }
            eB.setBias(4.0);
        }

        {
            {
                PatternPartSynapse s = PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(nC, eC);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                eC.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(eB, eC);

                s.linkOutput();
                s.addWeight(10.0);
                eC.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = RELATED_INPUT_SYNAPSE_FROM_PP_TEMPLATE.instantiateTemplate(lookupPPPT(m, nC), eC);

                s.linkOutput();
                s.addWeight(10.0);
                eC.addConjunctiveBias(-10.0, false);
            }

            {
                PatternPartSynapse s = RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(out, eC);

                s.linkOutput();
                s.addWeight(10.0);
                eC.addConjunctiveBias(-10.0, true);
            }
            eC.setBias(4.0);
        }

        {
            {
                PatternSynapse s = PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(eA, out);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                out.addConjunctiveBias(-10.0, false);
            }
            {
                PatternSynapse s = PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(eB, out);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                out.addConjunctiveBias(-10.0, false);
            }
            {
                PatternSynapse s = PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(eC, out);

                s.linkInput();
                s.linkOutput();
                s.addWeight(10.0);
                out.addConjunctiveBias(-10.0, false);
            }
            out.setBias(4.0);
        }
        return m;
    }

    public PatternPartNeuron lookupPPPT(TextModel tm, PatternNeuron pn) {
        return (PatternPartNeuron) pn.getOutputSynapses()
                .map(s -> s.getOutput())
                .filter(n -> isPTNeuron(tm, n))
                .findAny()
                .orElse(null);
    }

    private boolean isPTNeuron(TextModel tm, Neuron<?> n) {
        return n.getOutputSynapses()
                .map(s -> s.getOutput())
                .anyMatch(in -> in == tm.getPrevTokenInhib());
    }
}
