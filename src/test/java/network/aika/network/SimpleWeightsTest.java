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
import network.aika.neuron.range.Range;
import network.aika.neuron.relation.Relation;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class SimpleWeightsTest {

    @Test
    public void testWeightsOR() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron pC = m.createNeuron("C");
        Neuron.init(pC,
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(0.3f)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(0.4f)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeOutput(Range.Output.DIRECT)
        );

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.addInput(doc, 0, 6);

            doc.process();

            System.out.println(doc.activationsToString(true, false, true));

            doc.clearActivations();
        }
        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.addInput(doc, 0, 6);
            inB.addInput(doc, 0, 6);

            doc.process();

            System.out.println(doc.activationsToString(true, false, true));

            doc.clearActivations();
        }
    }


    @Test
    public void testWeightsAND() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron pC = m.createNeuron("C");
        Neuron.init(pC,
                0.01,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(3.0)
                        .setBias(-3.0)
                        .setRecurrent(false)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(3.0)
                        .setBias(-3.0)
                        .setRecurrent(false)
                        .addRangeRelation(Range.Relation.EQUALS, 0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRangeRelation(Range.Relation.EQUALS)
        );

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.addInput(doc, 0, 6);
            inB.addInput(doc, 0, 6);

            doc.process();

            System.out.println(doc.activationsToString(true, false, true));

            doc.clearActivations();
        }
    }
}
