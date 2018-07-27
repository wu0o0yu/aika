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
package network.aika.lattice;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range.Relation;
import network.aika.lattice.AndNode.Refinement;
import network.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternLatticeTest {

    @Test
    public void testPredefinedPatterns() {
        Model m = new Model();
        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");

        Neuron.init(m.createNeuron("ABC"),
                0.001,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .addRangeRelation(Relation.EQUALS, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .addRangeRelation(Relation.EQUALS, 2)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("", 0);


        InputNode pA = inA.get().outputNode.get();
        InputNode pB = inB.get().outputNode.get();
        InputNode pC = inC.get().outputNode.get();

        AndNode pAB = pA.andChildren.firstEntry().getValue().child.get();
        Assert.assertNotNull(pAB.provider);

        AndNode pBC = pC.andChildren.firstEntry().getValue().child.get();
        Assert.assertNotNull(pBC.provider);

        Assert.assertEquals(1, pAB.andChildren.size());
        Assert.assertEquals(1, pBC.andChildren.size());

        AndNode pABC = pAB.andChildren.firstEntry().getValue().child.get();
        Assert.assertNotNull(pABC);

        Assert.assertEquals(2, pABC.parents.size());
    }


    @Test
    public void testMultipleActivation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");

        Neuron nABC = Neuron.init(m.createNeuron("ABC"),
                0.001,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false)
                        .addRangeRelation(Relation.EQUALS, 1)
                        .setRangeOutput(true, false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false)
                        .addRangeRelation(Relation.EQUALS, 2),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false)
                        .setRangeOutput(false, true)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);
        inC.addInput(doc, 0, 1);

        Assert.assertFalse(nABC.getActivations(doc, false).isEmpty());
    }
}
