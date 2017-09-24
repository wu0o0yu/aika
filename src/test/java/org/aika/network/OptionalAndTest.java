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


import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class OptionalAndTest {

    @Test
    public void testOptionalAnd() {
        Model m = new Model(null, 2);

        Neuron wordEssen = m.createNeuron("word:essen");
        Neuron wordHamburg = m.createNeuron("word:hamburg");
        Neuron wordGehen = m.createNeuron("word:gehen");
        Neuron upperCase = m.createNeuron("upper case");

        Neuron suppr = m.createNeuron("SUPPRESS");

        Neuron hintNoun = m.initNeuron(m.createNeuron("HINT-NOUN"),
                -0.001,
                new Input()
                        .setNeuron(wordEssen)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0),
                new Input()
                        .setNeuron(wordHamburg)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
        );
        Neuron hintVerb = m.initNeuron(m.createNeuron("HINT-VERB"),
                -0.001,
                new Input()
                        .setNeuron(wordEssen)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0),
                new Input()
                        .setNeuron(wordGehen)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
        );


        Neuron noun = m.initNeuron(m.createNeuron("NOUN"),
                0.001,
                new Input()
                        .setNeuron(hintNoun)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(1.0)
                        .setMaxLowerWeightsSum(0.0f),
                new Input()
                        .setNeuron(upperCase)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setMaxLowerWeightsSum(0.0f),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setBiasDelta(1.0)
        );

        Neuron verb = m.initNeuron(m.createNeuron("VERB"),
                0.001,
                new Input()
                        .setNeuron(hintVerb)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(1.0)
                        .setMaxLowerWeightsSum(0.0f),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setBiasDelta(1.0)
        );

        m.initNeuron(suppr,
                -0.001,
                new Input()
                        .setNeuron(noun)
                        .setWeight(1.0f)
                        .setBiasDelta(0.0)
                        .setRecurrent(false),
                new Input()
                        .setNeuron(verb)
                        .setWeight(1.0f)
                        .setBiasDelta(0.0)
                        .setRecurrent(false)
        );


        Document doc1 = m.createDocument("Essen");
        Document doc2 = m.createDocument("essen", 1);

        for(Document doc: new Document[] {doc1, doc2}) {
            String txt = doc.getContent();
            int begin = txt.toLowerCase().indexOf("essen");
            int end = begin + 5;
            wordEssen.addInput(doc, begin, end);

            if(Character.isUpperCase(txt.charAt(begin))) {
                upperCase.addInput(doc, begin, end);
            }

            doc.process();

            System.out.println(doc.neuronActivationsToString(true, false, true));
            System.out.println();

            System.out.println(doc.nodeActivationsToString(false, true));

            doc.clearActivations();
        }
    }
}
