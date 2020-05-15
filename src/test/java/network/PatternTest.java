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
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.*;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.PatternInhibitoryNeuron;
import org.junit.jupiter.api.Test;

import static network.aika.neuron.PatternScope.INPUT_PATTERN;
import static network.aika.neuron.PatternScope.SAME_PATTERN;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternTest {

    @Test
    public void testPattern() {
        Model m = new Model();

        PatternNeuron inA = new PatternNeuron(m, "IN A", true);
        PatternNeuron inB = new PatternNeuron(m, "IN B", true);
        PatternNeuron inC = new PatternNeuron(m, "IN C", true);


        InhibitoryNeuron inputInhibN = new PatternInhibitoryNeuron(m, "INPUT INHIB", true);
        NeuronProvider.init(inputInhibN, 0.0,
                new InhibitorySynapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0),
                new InhibitorySynapse.Builder()
                        .setNeuron(inB)
                        .setWeight(1.0),
                new InhibitorySynapse.Builder()
                        .setNeuron(inC)
                        .setWeight(1.0)
        );

        PatternPartNeuron relN = new PatternPartNeuron(m, "Rel", true);
        NeuronProvider.init(relN, 1.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(inputInhibN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(inputInhibN)
                        .setWeight(10.0)
        );


        PatternPartNeuron eA = new PatternPartNeuron(m, "E A", false);
        PatternPartNeuron eB = new PatternPartNeuron(m, "E B", false);
        PatternPartNeuron eC = new PatternPartNeuron(m, "E C", false);

        PatternNeuron out = new PatternNeuron(m, "OUT", false);


        NeuronProvider.init(eA, 4.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(inA)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        NeuronProvider.init(eB, 4.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(inB)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(eA)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        NeuronProvider.init(eC, 4.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(inC)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(eB)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        NeuronProvider.init(out, 4.0,
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

        Activation actA = inA.addInputActivation(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
        );

        Activation inInhibA = actA.getOutputLinks(inputInhibN.getProvider(), SAME_PATTERN)
                .findAny()
                .map(l -> l.getOutput())
                .orElse(null);

        Activation actB = inB.addInputActivation(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(1)
                        .setFired(0)
        );

        Activation inInhibB = actB.getOutputLinks(inputInhibN.getProvider(), SAME_PATTERN)
                .findAny()
                .map(l -> l.getOutput())
                .orElse(null);

        relN.addInputActivation(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(1)
                        .setFired(0)
                        .addInputLink(INPUT_PATTERN, inInhibA)
                        .addInputLink(SAME_PATTERN, inInhibB)
        );

        Activation actC = inC.addInputActivation(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(2)
                        .setFired(0)
        );

        Activation inInhibC = actC.getOutputLinks(inputInhibN.getProvider(), SAME_PATTERN)
                .findAny()
                .map(l -> l.getOutput())
                .orElse(null);


        relN.addInputActivation(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(2)
                        .setFired(0)
                        .addInputLink(INPUT_PATTERN, inInhibB)
                        .addInputLink(SAME_PATTERN, inInhibC)
        );

        doc.process();

        System.out.println(doc.activationsToString());
    }
}
