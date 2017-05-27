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
package org.aika.network;


import org.aika.Iteration;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.Activation;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.Iteration.Input;
import org.aika.neuron.Synapse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 *
 * @author Lukas Molzberger
 */
public class NamedEntityRecognitionTest {




    @Test
    public void testNamedEntityRecognition() {
        // This test demonstrates the recognition of the words 'jackson cook' as forename and surname
        // even though each individual word would have been recognized as city or profession
        // respectively.

        Model m = new Model();
        Iteration t = m.startIteration(null, 0);

        Neuron forenameCategory = new Neuron("C-forename");
        Neuron surnameCategory = new Neuron("C-surname");
        Neuron suppressingN = new Neuron("SUPPR");

        InputNeuron spaceN = t.createOrLookupInputSignal("SPACE");
        InputNeuron startSignal = t.createOrLookupInputSignal("START-SIGNAL");
        Neuron ctNeuron = t.createCycleNeuron(new Neuron("CTN"),
                spaceN, false,
                startSignal, true,
                false
        );

        HashMap<String, InputNeuron> inputNeurons = new HashMap<>();
        HashMap<String, Neuron> relNeurons = new HashMap<>();


        String[] words = new String[] {
                "mr.", "jackson", "cook", "was", "born", "in", "new", "york"
        };
        for(String word: words) {
            InputNeuron in = t.createOrLookupInputSignal("W-" + word);
            Neuron rn = t.createRelationalNeuron(
                    new Neuron("W-" + word + "-RN"),
                    ctNeuron,
                    in, false
            );

            inputNeurons.put(word, in);
            relNeurons.put(word, rn);
        }

        // The entity neurons represent the concrete meanings of the input words.
        Neuron cookProfessionEntity = t.createAndNeuron(
                new Neuron("E-cook (profession)"),
                0.1,
                new Input()
                        .setNeuron(relNeurons.get("cook"))
                        .setWeight(12.0)
                        .setMinInput(0.9)
                        .setRecurrent(false),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
        );

        Neuron cookSurnameEntity = t.createAndNeuron(
                new Neuron("E-cook (surname)"),
                0.8,
                new Input() // Requires the word to be recognized
                        .setNeuron(relNeurons.get("cook"))
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(0)
                        .setRecurrent(false),
                new Input() // The previous word needs to be a forename
                        .setNeuron(forenameCategory)
                        .setWeight(5.0)
                        .setMinInput(0.9)
                        .setRelativeRid(-1) // previous word
                        .setRecurrent(true)
                        .setMatchRange(false) // ignore the range when matching this input
                        // The range of this input should have no effect on the range of the output
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE),

                // This neuron may be suppressed by the E-cook (profession) neuron, but there is no
                // self suppression taking place even though 'E-cook (surname)' is also contained
                // in the suppressingN.
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
        );

        Neuron jacksonForenameEntity = t.createAndNeuron(
                new Neuron("E-jackson (forename)"),
                0.8,
                new Input()
                        .setNeuron(relNeurons.get("jackson"))
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(0)
                        .setRecurrent(false),
                new Input()
                        .setNeuron(surnameCategory)
                        .setWeight(5.0)
                        .setMinInput(0.9)
                        .setRelativeRid(1)
                        .setRecurrent(true)
                        .setMatchRange(false)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
        );

        Neuron jacksonCityEntity = t.createAndNeuron(
                new Neuron("E-jackson (city)"),
                0.1,
                new Input()
                        .setNeuron(relNeurons.get("jackson"))
                        .setWeight(12.0)
                        .setMinInput(0.9)
                        .setRecurrent(false),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
        );

        t.createOrNeuron(forenameCategory,
                new Input() // In this example there is only one forename considered.
                        .setNeuron(jacksonForenameEntity)
                        .setWeight(10.0)
                        .setRelativeRid(0)
        );
        t.createOrNeuron(surnameCategory,
                new Input()
                        .setNeuron(cookSurnameEntity)
                        .setWeight(10.0)
                        .setRelativeRid(0)
        );

        t.createOrNeuron(suppressingN,
                new Input().setNeuron(cookProfessionEntity).setWeight(10.0),
                new Input().setNeuron(cookSurnameEntity).setWeight(10.0),
                new Input().setNeuron(jacksonCityEntity).setWeight(10.0),
                new Input().setNeuron(jacksonForenameEntity).setWeight(10.0)
        );

        // Now that the model is complete, start processing an actual text.

        Document doc = Document.create("mr. jackson cook was born in new york ");
        t = m.startIteration(doc, 0);

        // The start signal is used as a starting point for relational id counter.
        startSignal.addInput(t, 0, 1, 0);  // iteration, begin, end, relational id

        int i = 0;
        for(String w: doc.getContent().split(" ")) {
            int j = i + w.length();
            // The space is used as a clock signal to increase the relational id.
            spaceN.addInput(t, j, j + 1);

            // Feed the individual words as inputs into the network.
            inputNeurons.get(w).addInput(t, i, j);
            i = j + 1;
        }

        // Search for the best interpretation of this text.
        t.process();

        System.out.println(t.networkStateToString(true, true));
        System.out.println();

        System.out.println("Selected Option: " + t.doc.selectedOption.toString());
    }
}
