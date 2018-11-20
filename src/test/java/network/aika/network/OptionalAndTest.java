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
package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import network.aika.neuron.relation.Relation;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.EQUALS;
import static network.aika.neuron.relation.Relation.OVERLAPS;

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
                        .setSynapseId(0)
                        .setNeuron(wordEssen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(wordHamburg)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );
        Neuron hintVerb = Neuron.init(m.createNeuron("HINT-VERB"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wordEssen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(wordGehen)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        Neuron noun = Neuron.init(m.createNeuron("NOUN"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(hintNoun)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(upperCase)
                        .setWeight(0.5)
                        .setRecurrent(false)
                        .setBias(0.0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(suppr)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setBias(0.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(OVERLAPS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron verb = Neuron.init(m.createNeuron("VERB"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(hintVerb)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(suppr)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setBias(0.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(OVERLAPS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(suppr,
                0.0,
                INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(noun)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(verb)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
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

            System.out.println(doc.activationsToString());
            System.out.println();

            doc.clearActivations();
        }
    }
}
