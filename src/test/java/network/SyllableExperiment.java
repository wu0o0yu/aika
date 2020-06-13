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

import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.PatternInhibitoryNeuron;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Lukas Molzberger
 */
public class SyllableExperiment {

    private Model model;

    private Map<Character, PatternNeuron> inputLetters = new TreeMap<>();
    private InhibitoryNeuron inputInhibN;
    private ExcitatoryNeuron relN;

    @BeforeEach
    public void init() {
        model = new Model();

        inputInhibN = new PatternInhibitoryNeuron(model, "Input-Inhib", true);
        relN = new PatternPartNeuron(model, "Char-Relation", true);

        relN.link(1.0,
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(inputInhibN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNegative(false)
                        .setNeuron(inputInhibN)
                        .setWeight(10.0)
        );
    }

    public PatternNeuron lookupChar(Character character) {
        return inputLetters.computeIfAbsent(character, c -> {
            PatternNeuron n = new PatternNeuron(model, "" + c, true);

            inputInhibN.link(0.0,
                    new InhibitorySynapse.Builder()
                            .setNeuron(n)
                            .setWeight(1.0)
                            .setPropagate(true)
            );

            return n;
        });
    }

    private void train(String word) {
        Document doc = new Document(word,
                new Config()
                        .setLearnRate(0.025)
                        .setMetaThreshold(0.3)
        );
        System.out.println("  " + word);

        Activation lastAct = null;
        Activation lastInInhibAct = null;
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);

            Activation currentAct = lookupChar(c)
                    .propagate(doc,
                            new Activation.Builder()
                                    .setInputTimestamp(i)
                                    .setFired(0)
                                    .setValue(1.0)
                                    .setRangeCoverage(1.0)
                    );

            Activation currentInInhibAct = currentAct.getOutputLinks(inputInhibN.getProvider())
                    .findAny()
                    .map(l -> l.getOutput())
                    .orElse(null);

            if(lastAct != null) {
                relN.propagate(doc,
                        new Activation.Builder()
                            .setInputTimestamp(i)
                            .setFired(0)
                            .setValue(1.0)
                );
            }

            lastAct = currentAct;
            lastInInhibAct = currentInInhibAct;
        }

//        System.out.println(doc.activationsToString());

        doc.train(model);
    }

    @Test
    public void testTraining() throws IOException {
        for(String word: Util.loadExamplesAsWords(new File("/Users/lukas.molzberger/aika-ws/maerchen"))) {
            train( word + " ");
        }

//        model.dumpModel();
        System.out.println();
    }
}
