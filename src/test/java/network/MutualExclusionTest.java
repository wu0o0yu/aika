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
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.SortedSet;

import static network.aika.neuron.PatternScope.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Lukas Molzberger
 */
public class MutualExclusionTest {

    @Test
    public void testPropagation() {
        Model m = new Model();

        PatternNeuron in = new PatternNeuron(m, "IN");
        PatternPartNeuron na = new PatternPartNeuron(m, "A");
        PatternPartNeuron nb = new PatternPartNeuron(m, "B");
        PatternPartNeuron nc = new PatternPartNeuron(m, "C");
        InhibitoryNeuron inhib = new InhibitoryNeuron(m, "I", PatternPartNeuron.type);

        Neuron.init(na, 1.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(in)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(CONFLICTING_PATTERN)
                        .setRecurrent(true)
                        .setNegative(true)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
        );

        Neuron.init(nb, 1.5,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(in)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(CONFLICTING_PATTERN)
                        .setRecurrent(true)
                        .setNegative(true)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
        );

        Neuron.init(nc, 1.2,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(in)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(CONFLICTING_PATTERN)
                        .setRecurrent(true)
                        .setNegative(true)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
        );

        Neuron.init(inhib, 0.0,
                new InhibitorySynapse.Builder()
                        .setNeuron(na)
                        .setWeight(1.0),
                new InhibitorySynapse.Builder()
                        .setNeuron(nb)
                        .setWeight(1.0),
                new InhibitorySynapse.Builder()
                        .setNeuron(nc)
                        .setWeight(1.0)
                );


        Document doc = new Document("test");

        Activation inAct = in.addInputActivation(doc,
                new Activation.Builder()
                    .setValue(1.0)
                    .setInputTimestamp(0)
                    .setFired(0)
        );

        doc.process();

        System.out.println(doc.activationsToString());

        Map<INeuron, SortedSet<Activation>> results = doc.getActivationsPerNeuron();

        SortedSet<Activation> nbActs = results.get(nb);
        Activation nbAct = nbActs.first();

        assertTrue(nbAct.getValue() > 0.38);
    }
}
