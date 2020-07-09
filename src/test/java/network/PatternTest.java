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

import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.text.Document;
import network.aika.neuron.Neuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternTest {

    @Test
    public void testPatternPos() {
        TextModel m = initModel();

        Document doc = new Document("ABC");

        doc.processToken(m, 0, 1,  "A");
        doc.processToken(m, 1, 2,  "B");
        doc.processToken(m, 2, 3,  "C");

        doc.process();

        System.out.println(doc.activationsToString());
    }


    @Test
    public void testPatternNeg() {
        TextModel m = initModel();

        Document doc = new Document("ABC");

        doc.processToken(m, 0, 1,  "A");
        doc.processToken(m, 1, 2,  "B");

        doc.process();

        System.out.println(doc.activationsToString());
    }

    public TextModel initModel() {
        TextModel m = new TextModel();

        PatternNeuron nA = m.lookupToken("A");
        PatternNeuron nB = m.lookupToken("B");
        PatternNeuron nC = m.lookupToken("C");

        PatternPartNeuron eA = new PatternPartNeuron(m, "E A", false);
        PatternPartNeuron eB = new PatternPartNeuron(m, "E B", false);
        PatternPartNeuron eC = new PatternPartNeuron(m, "E C", false);

        PatternNeuron out = new PatternNeuron(m, "ABC", "OUT", false);

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(nA, eA);
                s.setPropagate(true);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(out, eA);

                s.link();
                s.setWeight(10.0);
                eA.setRecurrentConjunctiveBias(-10.0);
            }
            eA.setBias(4.0);
        }

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(nB, eB);
                s.setPropagate(true);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(eA, eB);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(lookupPPPT(m, nB), eB);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(out, eB);

                s.link();
                s.update(10.0, true);
            }
            eB.setBias(4.0);
        }

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(nC, eC);
                s.setPropagate(true);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(eB, eC);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(lookupPPPT(m, nC), eC);

                s.link();
                s.update(10.0, false);
            }

            {
                ExcitatorySynapse s = new ExcitatorySynapse(out, eC);

                s.link();
                s.update(10.0, true);
            }
            eC.setBias(4.0);
        }

        {
            {
                ExcitatorySynapse s = new ExcitatorySynapse(eA, out);
                s.setPropagate(true);

                s.link();
                s.update(10.0, false);
            }
            {
                ExcitatorySynapse s = new ExcitatorySynapse(eB, out);
                s.setPropagate(true);

                s.link();
                s.update(10.0, false);
            }
            {
                ExcitatorySynapse s = new ExcitatorySynapse(eC, out);
                s.setPropagate(true);

                s.link();
                s.update(10.0, false);
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
