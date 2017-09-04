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
import org.aika.corpus.Document;
import org.aika.neuron.Neuron;
import org.aika.neuron.Neuron;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class OptionalAndTest {

    @Test
    public void testOptionalAnd() {
        Model m = new Model(null, 2);

        Neuron wordEssen = new Neuron(m, "word:essen");
        Neuron wordHamburg = new Neuron(m, "word:hamburg");
        Neuron wordGehen = new Neuron(m, "word:gehen");
        Neuron upperCase = new Neuron(m, "upper case");

        Neuron suppr = new Neuron(m, "SUPPRESS");

        Neuron hintNoun = m.initOrNeuron(new Neuron(m, "HINT-NOUN"),
                new Input()
                        .setOptional(false)
                        .setNeuron(wordEssen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setOptional(false)
                        .setNeuron(wordHamburg)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );
        Neuron hintVerb = m.initOrNeuron(new Neuron(m, "HINT-VERB"),
                new Input()
                        .setOptional(false)
                        .setNeuron(wordEssen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setOptional(false)
                        .setNeuron(wordGehen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );


        Neuron noun = m.initAndNeuron(new Neuron(m, "NOUN"),
                0.001,
                new Input()
                        .setOptional(false)
                        .setNeuron(hintNoun)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setMaxLowerWeightsSum(0.0),
                new Input()
                        .setOptional(true)
                        .setNeuron(upperCase)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setMaxLowerWeightsSum(0.0),
                new Input()
                        .setOptional(false)
                        .setNeuron(suppr)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
        );

        Neuron verb = m.initAndNeuron(new Neuron(m, "VERB"),
                0.001,
                new Input()
                        .setOptional(false)
                        .setNeuron(hintVerb)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setMaxLowerWeightsSum(0.0),
                new Input()
                        .setOptional(false)
                        .setNeuron(suppr)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
        );

        m.initOrNeuron(suppr,
                new Input()
                        .setOptional(false)
                        .setNeuron(noun)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setOptional(false)
                        .setNeuron(verb)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
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
