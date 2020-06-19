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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
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
        Model m = new Model();

        PatternNeuron in = new PatternNeuron(m, "IN", true);
        PatternPartNeuron na = new PatternPartNeuron(m, "A", false);
        PatternPartNeuron nb = new PatternPartNeuron(m, "B", false);
        PatternPartNeuron nc = new PatternPartNeuron(m, "C", false);
        InhibitoryNeuron inhib = new InhibitoryNeuron(m, "I", false);

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, na);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(inhib, na);
                s.setNegative(true);

                s.link();
                s.update(-100.0);
            }

            na.setBias(1.0);
            na.commit();
        }

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, nb);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(inhib, nb);
                s.setNegative(true);

                s.link();
                s.update(-100.0);
            }
            na.setBias(1.5);
            na.commit();
        }


        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, nc);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(inhib, nc);
                s.setNegative(true);

                s.link();
                s.update(-100.0);
            }

            na.setBias(1.2);
            na.commit();
        }

        {
            {
                InhibitorySynapse s = new InhibitorySynapse(na, inhib);
                s.link();
                s.update(1.0);
                s.commit();
            }
            {
                InhibitorySynapse s = new InhibitorySynapse(nb, inhib);
                s.link();
                s.update(1.0);
                s.commit();
            }
            {
                InhibitorySynapse s = new InhibitorySynapse(nc, inhib);
                s.link();
                s.update(1.0);
                s.commit();
            }

            inhib.setBias(0.0);
            inhib.commitBias();
        }


        Document doc = new Document("test");

        Activation inAct = in.propagate(doc,
                new Activation.Builder()
                    .setValue(1.0)
                    .setInputTimestamp(0)
                    .setFired(0)
        );

        doc.process();

        System.out.println(doc.activationsToString());

        Set<Activation> nbActs = doc.getActivations(nb);
        Activation nbAct = nbActs.iterator().next();

        assertTrue(nbAct.getValue() > 0.38);
    }
}
