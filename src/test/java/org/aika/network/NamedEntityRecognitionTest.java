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


import org.aika.Activation;
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.SearchNode;
import org.aika.lattice.Node;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Test;

import java.util.HashMap;

import static org.aika.Input.RangeRelation.*;

/**
 *
 * @author Lukas Molzberger
 */
public class NamedEntityRecognitionTest {

    // This test demonstrates the recognition of the words 'jackson cook' as forename and surname
    // even though each individual word would have been recognized as city or profession
    // respectively.
    @Test
    public void testNamedEntityRecognitionWithoutCounterNeuron() {
        Model m = new Model(1); // number of threads

        Neuron forenameCategory = m.createNeuron("C-forename");
        Neuron surnameCategory = m.createNeuron("C-surname");
        Neuron suppressingN = m.createNeuron("SUPPR");


        // The word input neurons which do not yet possess a relational id.
        HashMap<String, InputNeuron> inputNeurons = new HashMap<>();

        String[] words = new String[] {
                "mr.", "jackson", "cook", "was", "born", "in", "new", "york"
        };
        for(String word: words) {
            InputNeuron in = m.createOrLookupInputNeuron("W-" + word);

            inputNeurons.put(word, in);
        }

        // The entity neurons represent the concrete meanings of the input words.
        // The helper function 'initAndNeuron' computes the required bias for a
        // conjunction of the inputs.
        Neuron cookSurnameEntity = m.initAndNeuron(
                m.createNeuron("E-cook (surname)"),
                0.5, // adjusts the bias
                new Input() // Requires the word to be recognized
                        .setNeuron(inputNeurons.get("cook"))
                        .setWeight(10.0)
                        // This input requires the input activation to have an
                        // activation value of at least 0.9
                        .setMinInput(0.9)
                        .setRelativeRid(0) // references the current word
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input() // The previous word needs to be a forename
                        .setNeuron(forenameCategory)
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(-1) // references the previous word
                        .setRecurrent(true) // this input is a positive feedback loop
                        .setRangeMatch(NONE)
                        .setRangeOutput(false),

                // This neuron may be suppressed by the E-cook (profession) neuron, but there is no
                // self suppression taking place even though 'E-cook (surname)' is also contained
                // in the suppressingN.
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true) // this input is a negative feedback loop
                        .setRangeMatch(CONTAINS)
        );

        Neuron cookProfessionEntity = m.initAndNeuron(
                m.createNeuron("E-cook (profession)"),
                0.2,
                new Input()
                        .setNeuron(inputNeurons.get("cook"))
                        .setWeight(15.0)
                        .setMinInput(0.9)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
                        .setRangeMatch(CONTAINS)
        );

        Neuron jacksonForenameEntity = m.initAndNeuron(
                m.createNeuron("E-jackson (forename)"),
                0.5,
                new Input()
                        .setNeuron(inputNeurons.get("jackson"))
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(surnameCategory)
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(1)
                        .setRecurrent(true)
                        .setRangeMatch(NONE),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
                        .setRangeMatch(CONTAINED_IN)
        );

        Neuron jacksonCityEntity = m.initAndNeuron(
                m.createNeuron("E-jackson (city)"),
                0.2,
                new Input()
                        .setNeuron(inputNeurons.get("jackson"))
                        .setWeight(12.0)
                        .setMinInput(0.9)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
                        .setRangeMatch(CONTAINED_IN)
        );

        m.initOrNeuron(forenameCategory,
                new Input() // In this example there is only one forename considered.
                        .setNeuron(jacksonForenameEntity)
                        .setWeight(10.0)
                        .setRelativeRid(0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );
        m.initOrNeuron(surnameCategory,
                new Input()
                        .setNeuron(cookSurnameEntity)
                        .setWeight(10.0)
                        .setRelativeRid(0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        m.initOrNeuron(suppressingN,
                new Input().setNeuron(cookProfessionEntity).setWeight(10.0).setRangeMatch(EQUALS).setRangeOutput(true),
                new Input().setNeuron(cookSurnameEntity).setWeight(10.0).setRangeMatch(EQUALS).setRangeOutput(true),
                new Input().setNeuron(jacksonCityEntity).setWeight(10.0).setRangeMatch(EQUALS).setRangeOutput(true),
                new Input().setNeuron(jacksonForenameEntity).setWeight(10.0).setRangeMatch(EQUALS).setRangeOutput(true)
        );


        // Now that the model is complete, start processing an actual text.
        Document doc = m.createDocument("mr. jackson cook was born in new york ");

        int i = 0;
        int wordPos = 0;
        for(String w: doc.getContent().split(" ")) {
            int j = i + w.length();

            // Feed the individual words as inputs into the network.
            inputNeurons.get(w).addInput(doc, i, j, wordPos);
            i = j + 1;
            wordPos++;
        }

        // Search for the best interpretation of this text.
        doc.process();

        System.out.println(doc.networkStateToString(true, true, false, true));
        System.out.println();

        System.out.println("Selected Option: " + doc.bestInterpretation.toString());
        System.out.println();

        System.out.println("Activations of the Surname Category:");
        Node<?> n = surnameCategory.node.get();
        for(Activation act: n.getActivations(doc)) {
            if(act.finalState.value > 0.0) {
                System.out.print(act.key.r + " ");
                System.out.print(act.key.rid + " ");
                System.out.print(act.key.o + " ");
                System.out.print(act.key.n.neuron.get().label + " ");
                System.out.print(act.finalState.value);
            }
        }

        doc.clearActivations();
    }


    // This test demonstrates the recognition of the words 'jackson cook' as forename and surname
    // even though each individual word would have been recognized as city or profession
    // respectively.
    @Test
    public void testNamedEntityRecognitionWithCounterNeuron() {
        Model m = new Model(1); // number of threads

        Neuron forenameCategory = m.createNeuron("C-forename");
        Neuron surnameCategory = m.createNeuron("C-surname");
        Neuron suppressingN = m.createNeuron("SUPPR");

        // The following three neurons are used to assign each word activation
        // a relational id (rid). Here, the relational id specifies the
        // word position within the sentence.
        InputNeuron spaceN = m.createOrLookupInputNeuron("SPACE");
        InputNeuron startSignal = m.createOrLookupInputNeuron("START-SIGNAL");
        Neuron ctNeuron = m.initCounterNeuron(m.createNeuron("RID Counter"),
                spaceN, // clock signal
                false, // direction of the clock signal (range end counts)
                startSignal, // start signal
                true, // direction of the start signal (range begin counts)
                false // direction of the counting neuron
        );
        // initCounterNeuron is just a convenience method which creates an ordinary neuron
        // with some input synapses.


        // The word input neurons which do not yet possess a relational id.
        HashMap<String, InputNeuron> inputNeurons = new HashMap<>();

        // The word input neurons with a relational id.
        HashMap<String, Neuron> relNeurons = new HashMap<>();


        String[] words = new String[] {
                "mr.", "jackson", "cook", "was", "born", "in", "new", "york"
        };
        for(String word: words) {
            InputNeuron in = m.createOrLookupInputNeuron("W-" + word);
            Neuron rn = m.initRelationalNeuron(
                    m.createNeuron("WR-" + word),
                    ctNeuron, // RID Counting neuron
                    in, // Input neuron
                    false // Direction of the input neuron
            );

            inputNeurons.put(word, in);
            relNeurons.put(word, rn);
        }

        // The entity neurons represent the concrete meanings of the input words.
        // The helper function 'initAndNeuron' computes the required bias for a
        // conjunction of the inputs.
        Neuron cookSurnameEntity = m.initAndNeuron(
                m.createNeuron("E-cook (surname)"),
                0.5, // adjusts the bias
                new Input() // Requires the word to be recognized
                        .setNeuron(relNeurons.get("cook"))
                        .setWeight(10.0)
                        // This input requires the input activation to have an
                        // activation value of at least 0.9
                        .setMinInput(0.9)
                        .setRelativeRid(0) // references the current word
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input() // The previous word needs to be a forename
                        .setNeuron(forenameCategory)
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(-1) // references the previous word
                        .setRecurrent(true) // this input is a positive feedback loop
                        .setRangeMatch(NONE),

                // This neuron may be suppressed by the E-cook (profession) neuron, but there is no
                // self suppression taking place even though 'E-cook (surname)' is also contained
                // in the suppressingN.
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true) // this input is a negative feedback loop
                        .setRangeMatch(CONTAINS)
        );

        Neuron cookProfessionEntity = m.initAndNeuron(
                m.createNeuron("E-cook (profession)"),
                0.2,
                new Input()
                        .setNeuron(relNeurons.get("cook"))
                        .setWeight(15.0)
                        .setMinInput(0.9)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
                        .setRangeMatch(CONTAINS)
        );

        Neuron jacksonForenameEntity = m.initAndNeuron(
                m.createNeuron("E-jackson (forename)"),
                0.5,
                new Input()
                        .setNeuron(relNeurons.get("jackson"))
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(surnameCategory)
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(1)
                        .setRecurrent(true)
                        .setRangeMatch(NONE),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
                        .setRangeMatch(CONTAINED_IN)
        );

        Neuron jacksonCityEntity = m.initAndNeuron(
                m.createNeuron("E-jackson (city)"),
                0.2,
                new Input()
                        .setNeuron(relNeurons.get("jackson"))
                        .setWeight(12.0)
                        .setMinInput(0.9)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-20.0)
                        .setMinInput(1.0)
                        .setRecurrent(true)
                        .setRangeMatch(CONTAINED_IN)
        );

        m.initOrNeuron(forenameCategory,
                new Input() // In this example there is only one forename considered.
                        .setNeuron(jacksonForenameEntity)
                        .setWeight(10.0)
                        .setRelativeRid(0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );
        m.initOrNeuron(surnameCategory,
                new Input()
                        .setNeuron(cookSurnameEntity)
                        .setWeight(10.0)
                        .setRelativeRid(0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        m.initOrNeuron(suppressingN,
                new Input().setNeuron(cookProfessionEntity).setWeight(10.0).setRangeMatch(EQUALS).setRangeOutput(true),
                new Input().setNeuron(cookSurnameEntity).setWeight(10.0).setRangeMatch(EQUALS).setRangeOutput(true),
                new Input().setNeuron(jacksonCityEntity).setWeight(10.0).setRangeMatch(EQUALS).setRangeOutput(true),
                new Input().setNeuron(jacksonForenameEntity).setWeight(10.0).setRangeMatch(EQUALS).setRangeOutput(true)
        );


        // Now that the model is complete, start processing an actual text.
        Document doc = m.createDocument("mr. jackson cook was born in new york ", 0);

        // The start signal is used as a starting point for relational id counter.
        startSignal.addInput(doc, 0, 1, 0);  // iteration, begin, end, relational id

        int i = 0;
        for(String w: doc.getContent().split(" ")) {
            int j = i + w.length();
            // The space is used as a clock signal to increase the relational id.
            spaceN.addInput(doc, j, j + 1);

            // Feed the individual words as inputs into the network.
            inputNeurons.get(w).addInput(doc, i, j);
            i = j + 1;
        }

        // Search for the best interpretation of this text.
        SearchNode.INCOMPLETE_OPTIMIZATION = true;
        doc.process();

        System.out.println(doc.networkStateToString(true, true, false, true));
        System.out.println();

        System.out.println("Selected Option: " + doc.bestInterpretation.toString());
        System.out.println();

        System.out.println("Activations of the Surname Category:");
        Node<?> n = surnameCategory.node.get();
        for(Activation act: n.getActivations(doc)) {
            if(act.finalState.value > 0.0) {
                System.out.print(act.key.r + " ");
                System.out.print(act.key.rid + " ");
                System.out.print(act.key.o + " ");
                System.out.print(act.key.n.neuron.get().label + " ");
                System.out.print(act.finalState.value);
            }
        }

        doc.clearActivations();
    }
}
