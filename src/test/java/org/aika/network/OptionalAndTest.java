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


import org.aika.neuron.Synapse;
import org.aika.Model;
import org.aika.neuron.Neuron;
import org.aika.Document;
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

        Neuron hintNoun = Neuron.init(m.createNeuron("HINT-NOUN"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(wordEssen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0),
                new Synapse.Builder()
                        .setNeuron(wordHamburg)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
        );
        Neuron hintVerb = Neuron.init(m.createNeuron("HINT-VERB"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(wordEssen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0),
                new Synapse.Builder()
                        .setNeuron(wordGehen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
        );


        Neuron noun = Neuron.init(m.createNeuron("NOUN"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(hintNoun)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Synapse.Builder()
                        .setNeuron(upperCase)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0),
                new Synapse.Builder()
                        .setNeuron(suppr)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setBias(0.0)
        );

        Neuron verb = Neuron.init(m.createNeuron("VERB"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(hintVerb)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Synapse.Builder()
                        .setNeuron(suppr)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setBias(0.0)
        );

        Neuron.init(suppr,
                0.0,
                INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setNeuron(noun)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setNeuron(verb)
                        .setWeight(1.0)
                        .setBias(0.0)
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

            System.out.println(doc.activationsToString(false, true));
            System.out.println();

            doc.clearActivations();
        }
    }
}
