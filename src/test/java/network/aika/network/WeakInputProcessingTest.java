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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.range.Range;
import network.aika.neuron.range.Range.Relation;
import network.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class WeakInputProcessingTest {


    @Test
    public void testWeakInputProcessing() {
        Model m = new Model();

        Neuron strongInput = m.createNeuron("Strong Input");

        Neuron weakInputA = m.createNeuron("Weak Input A");
        Neuron weakInputB = m.createNeuron("Weak Input B");
        Neuron weakInputC = m.createNeuron("Weak Input C");

        Neuron suppr = m.createNeuron("suppr");

        Neuron patternA = Neuron.init(
                m.createNeuron("Pattern A"),
                0.4,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setBias(-45.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(weakInputA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .addRangeRelation(Relation.EQUALS, 0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setBias(0.0)
                        .addRangeRelation(Relation.OVERLAPS, 0)
        );

        Neuron patternB = Neuron.init(
                m.createNeuron("Pattern B"),
                0.4,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setBias(-45.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(weakInputB)
                        .setWeight(1.5f)
                        .setRecurrent(false)
                        .setBias(-1.5)
                        .addRangeRelation(Relation.EQUALS, 0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 0)
                        .setRangeOutput(true)
        );

        Neuron patternC = Neuron.init(
                m.createNeuron("Pattern C"),
                0.4,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setBias(-45.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(weakInputC)
                        .setWeight(0.5f)
                        .setRecurrent(false)
                        .setBias(-0.5)
                        .addRangeRelation(Relation.EQUALS, 0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 0)
                        .setRangeOutput(true)
        );


        Neuron.init(suppr,
                0.0,
                INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(patternA)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(patternB)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(patternC)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("a ");

        strongInput.addInput(doc,0,1);

        weakInputB.addInput(doc, 0, 1);

        doc.process();

        System.out.println(doc.activationsToString(true, false, true));

        Activation act = patternA.getActivation(doc, new Range(0, 1), false);
        Assert.assertTrue(act.getFinalState().value < 0.5);

        act = patternB.getActivation(doc, new Range(0, 1), false);
        Assert.assertTrue(act.getFinalState().value > 0.5);

        act = patternC.getActivation(doc, new Range(0, 1), false);
        Assert.assertTrue(act.getFinalState().value < 0.5);
    }

}
