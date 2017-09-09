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
import org.aika.Provider;
import org.aika.corpus.Document;
import org.aika.neuron.INeuron;
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

        Neuron hintNoun = m.initOrNeuron(m.createNeuron("HINT-NOUN"),
                new Input()
                        .setOptional(false)
                        .setNeuron(wordEssen)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f),
                new Input()
                        .setOptional(false)
                        .setNeuron(wordHamburg)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
        );
        Neuron hintVerb = m.initOrNeuron(m.createNeuron("HINT-VERB"),
                new Input()
                        .setOptional(false)
                        .setNeuron(wordEssen)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f),
                new Input()
                        .setOptional(false)
                        .setNeuron(wordGehen)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
        );


        Neuron noun = m.initAndNeuron(m.createNeuron("NOUN"),
                0.001,
                new Input()
                        .setOptional(false)
                        .setNeuron(hintNoun)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setMaxLowerWeightsSum(0.0f),
                new Input()
                        .setOptional(true)
                        .setNeuron(upperCase)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setMaxLowerWeightsSum(0.0f),
                new Input()
                        .setOptional(false)
                        .setNeuron(suppr)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setMinInput(1.0f)
        );

        Neuron verb = m.initAndNeuron(m.createNeuron("VERB"),
                0.001,
                new Input()
                        .setOptional(false)
                        .setNeuron(hintVerb)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setMaxLowerWeightsSum(0.0f),
                new Input()
                        .setOptional(false)
                        .setNeuron(suppr)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setMinInput(1.0f)
        );

        m.initOrNeuron(suppr,
                new Input()
                        .setOptional(false)
                        .setNeuron(noun)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f),
                new Input()
                        .setOptional(false)
                        .setNeuron(verb)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
        );


        Document doc1 = m.createDocument("Essen");
        Document doc2 = m.createDocument("essen", 1);

        for(Document doc: new Document[] {doc1, doc2}) {
            String txt = doc.getContent();
            int begin = txt.toLowerCase().indexOf("essen");
            int end = begin + 5;
            wordEssen.get().addInput(doc, begin, end);

            if(Character.isUpperCase(txt.charAt(begin))) {
                upperCase.get().addInput(doc, begin, end);
            }

            doc.process();

            System.out.println(doc.neuronActivationsToString(true, false, true));
            System.out.println();

            System.out.println(doc.nodeActivationsToString(false, true));

            doc.clearActivations();
        }
    }
}
