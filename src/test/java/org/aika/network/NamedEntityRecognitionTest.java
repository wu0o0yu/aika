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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 *
 * @author Lukas Molzberger
 */
public class NamedEntityRecognitionTest {

    Model m;
    Neuron ctNeuron;
    InputNeuron spaceN;
    InputNeuron sentenceN;
    Neuron suppressingN;
    Neuron salutationN;
    Neuron salutationRN;
    Neuron salutationHintN;
    Neuron phraseTermN = new Neuron("PHRASE-TERM");
    InputNeuron sentenceTermN;
    Neuron surnameN;
    Neuron professionN;


    HashMap<String, InputNeuron> words = new HashMap<>();


    @Before
    public void init() {
        m = new Model();
        Iteration t = m.startIteration(null, 0);

        spaceN = t.createOrLookupInputSignal("SPACE");
        sentenceN = t.createOrLookupInputSignal("SENTENCE");
        sentenceTermN = t.createOrLookupInputSignal("SENTENCE-TERM");
        suppressingN = new Neuron("SUPPR");
        salutationN = t.createOrNeuron(new Neuron("SALUTATION"),
                new Input().setNeuron(lookup(t, "Herr")).setWeight(1.0).setRecurrent(false).setRelativeRid(0).setMinInput(1.0),
                new Input().setNeuron(lookup(t, "Frau")).setWeight(1.0).setRecurrent(false).setRelativeRid(0).setMinInput(1.0)
        );

        ctNeuron = t.createCycleNeuron(new Neuron("CTN"), spaceN, false, salutationN, true, false);
        salutationRN = t.createRelationalNeuron(new Neuron("SALUTATION-RN"), ctNeuron, salutationN, false);
        salutationHintN = t.createAndNeuron(new Neuron("SALUTATION-HINT"),
                0.001,
                new Input()
                        .setNeuron(salutationRN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(1),
                new Input()
                        .setNeuron(ctNeuron)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
        );

        Set<Input> categoryNeurons = new TreeSet<>();

        surnameN = parseList(t, new String[] {"Schneider", "Schuster", "Koch", "Schmidt", "Strehl", "Kuntze"}, "SURNAME", 0.7);
        categoryNeurons.add(
                new Input()
                        .setNeuron(surnameN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        professionN = parseList(t, new String[] {"Schneider", "Schuster", "Koch", "Entwickler", "Arzt", "Pfleger"}, "PROFESSION", 0.8);
        categoryNeurons.add(
                new Input()
                        .setNeuron(professionN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        t.createOrNeuron(suppressingN, categoryNeurons);

        phraseTermN = t.createOrNeuron(
                phraseTermN,
                new Input()
                        .setNeuron(surnameN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(sentenceTermN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );
    }


    public InputNeuron lookup(Iteration t, String word) {
        word = word.toLowerCase();
        InputNeuron n = words.get(word);
        if(n == null) {
            n = t.createOrLookupInputSignal(word);
            words.put(word, n);
        }
        return n;
    }


    public Neuron parseList(Iteration t, String[] list, String label, double weight) {
        Set<Input> inputsHint = new TreeSet<>();
        for(String e: list) {
            inputsHint.add(
                    new Input()
                            .setNeuron(lookup(t, e))
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setRelativeRid(0)
                            .setMinInput(1.0)
            );
        }

        if(label.equals("SURNAME")) {
            inputsHint.add(
                    new Input()
                            .setNeuron(salutationHintN)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0)
                            .setRelativeRid(0)
            );
        }

        Neuron hint = t.createOrNeuron(new Neuron(label + "-HINT"), inputsHint);
        Neuron meaning = t.createAndNeuron(new Neuron(label),
                0.001,
                new Input()
                        .setNeuron(hint)
                        .setWeight(1.0)
                        .setMaxLowerWeightsSum(0.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(suppressingN)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );
        return meaning;
    }


    public void processWord(Iteration t, int begin, int end) {
        String word = t.doc.getContent().substring(begin, end);
        InputNeuron is = lookup(t, word.trim());
        is.addInput(t, begin, end);
    }


    public Iteration processTest(String txt) {
        Document doc = Document.create(txt);

        Iteration t = m.startIteration(doc, 0);

        for(int i = 0; i < doc.getContent().length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                spaceN.addInput(t, i, i + 1);
            } else if(c == '.') {
                sentenceTermN.addInput(t, i, i + 1);
            }
        }


        int begin = -1;
        for(int i = 0; i < doc.getContent().length(); i++) {
            char c = doc.getContent().charAt(i);

            if(c == ' ' || c == '.') {
                if(begin + 1 < i) {
                    processWord(t, begin + 1, i + (c == ' ' ? 1 : 0));
                }
                begin = i;
            }
        }
        if(begin + 1 < doc.getContent().length()) {
            processWord(t, begin + 1, doc.getContent().length());
        }

        t.process();

        return t;
    }


    @Test
    public void testNER() {
        Iteration t = processTest("Herr Schneider besuchte seinen Arzt.");

        System.out.println("Selected Option: " + t.doc.selectedOption.toString());

        System.out.println(t.networkStateToString(true, true));
        System.out.println();

        System.out.println(t.networkStateToString(false, true));
        System.out.println();

        Assert.assertNotNull(Activation.get(t, professionN.node, null, new Range(31, 35), Range.Relation.EQUALS, t.doc.selectedOption, Option.Relation.CONTAINED_IN));
        Assert.assertNotNull(Activation.get(t, surnameN.node, null, new Range(5, 15), Range.Relation.EQUALS, t.doc.selectedOption, Option.Relation.CONTAINED_IN));
    }
}
