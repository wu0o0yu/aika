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
package org.aika.lattice;


import org.aika.*;
import org.aika.neuron.activation.Range.Relation;
import org.aika.Document;
import org.aika.lattice.AndNode.Refinement;
import org.aika.network.TestHelper;
import org.aika.neuron.INeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
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
        Neuron inD = m.createNeuron("D");

        {
            Neuron.init(m.createNeuron("ABC"),
                    0.001,
                    INeuron.Type.EXCITATORY,
                    new Synapse.Builder()
                            .setNeuron(inA)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true),
                    new Synapse.Builder()
                            .setNeuron(inB)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true),
                    new Synapse.Builder()
                            .setNeuron(inC)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true)
            );

            Document doc = m.createDocument(null, 0);


            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null, true);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null, true);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null, true);

            AndNode pAB = pA.andChildren.get(new Refinement(null, pB.provider)).get();
            Assert.assertEquals(pAB.provider, pB.andChildren.get(new Refinement(null, pA.provider)));

            AndNode pAC = pA.andChildren.get(new Refinement(null, pC.provider)).get();
            Assert.assertEquals(pAC.provider, pC.andChildren.get(new Refinement(null, pA.provider)));

            AndNode pBC = pB.andChildren.get(new Refinement(null, pC.provider)).get();
            Assert.assertEquals(pBC.provider, pC.andChildren.get(new Refinement(null, pB.provider)));

            Assert.assertEquals(1, pAB.andChildren.size());
            Assert.assertEquals(1, pBC.andChildren.size());
            Assert.assertEquals(1, pAC.andChildren.size());

            AndNode pABC = pAB.andChildren.get(new Refinement(null, pC.provider)).get();
            Assert.assertEquals(pABC.provider, pAC.andChildren.get(new Refinement(null, pB.provider)));
            Assert.assertEquals(pABC.provider, pBC.andChildren.get(new Refinement(null, pA.provider)));

            Assert.assertEquals(3, pABC.parents.size());
            Assert.assertEquals(pAB.provider, pABC.parents.get(new Refinement(null, pC.provider)));
            Assert.assertEquals(pAC.provider, pABC.parents.get(new Refinement(null, pB.provider)));
            Assert.assertEquals(pBC.provider, pABC.parents.get(new Refinement(null, pA.provider)));
        }
        {
            Neuron.init(m.createNeuron("BCD"),
                    0.001,
                    INeuron.Type.EXCITATORY,
                    new Synapse.Builder()
                            .setNeuron(inB)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true),
                    new Synapse.Builder()
                            .setNeuron(inC)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true),
                    new Synapse.Builder()
                            .setNeuron(inD)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true)
            );

            Document doc = m.createDocument(null, 0);

            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null, true);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null, true);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null, true);
            InputNode pD = TestHelper.addOutputNode(doc, inD, null, null, true);

            AndNode pAB = pA.andChildren.get(new Refinement(null, pB.provider)).get();
            Assert.assertEquals(pAB.provider, pB.andChildren.get(new Refinement(null, pA.provider)));

            AndNode pAC = pA.andChildren.get(new Refinement(null, pC.provider)).get();
            Assert.assertEquals(pAC.provider, pC.andChildren.get(new Refinement(null, pA.provider)));

            AndNode pBC = pB.andChildren.get(new Refinement(null, pC.provider)).get();
            Assert.assertEquals(pBC.provider, pC.andChildren.get(new Refinement(null, pB.provider)));

            AndNode pBD = pB.andChildren.get(new Refinement(null, pD.provider)).get();
            Assert.assertEquals(pBD.provider, pD.andChildren.get(new Refinement(null, pB.provider)));

            AndNode pCD = pC.andChildren.get(new Refinement(null, pD.provider)).get();
            Assert.assertEquals(pCD.provider, pD.andChildren.get(new Refinement(null, pC.provider)));


            Assert.assertEquals(1, pAB.andChildren.size());
            Assert.assertEquals(1, pAC.andChildren.size());
            Assert.assertEquals(2, pBC.andChildren.size());
            Assert.assertEquals(1, pBD.andChildren.size());
            Assert.assertEquals(1, pCD.andChildren.size());

            AndNode pBCD = pBC.andChildren.get(new Refinement(null, pD.provider)).get();
            Assert.assertEquals(pBCD.provider, pBD.andChildren.get(new Refinement(null, pC.provider)));
            Assert.assertEquals(pBCD.provider, pCD.andChildren.get(new Refinement(null, pB.provider)));

            Assert.assertEquals(3, pBCD.parents.size());
            Assert.assertEquals(pBC.provider, pBCD.parents.get(new Refinement(null, pD.provider)));
            Assert.assertEquals(pBD.provider, pBCD.parents.get(new Refinement(null, pC.provider)));
            Assert.assertEquals(pCD.provider, pBCD.parents.get(new Refinement(null, pB.provider)));
        }
        {
            Neuron.init(m.createNeuron("ABCD"),
                    0.001,
                    INeuron.Type.EXCITATORY,
                    new Synapse.Builder()
                            .setNeuron(inA)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true),
                    new Synapse.Builder()
                            .setNeuron(inB)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true),
                    new Synapse.Builder()
                            .setNeuron(inC)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true),
                    new Synapse.Builder()
                            .setNeuron(inD)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setBias(-1.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true)
            );

            Document doc = m.createDocument(null, 0);

            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null, true);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null, true);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null, true);
            InputNode pD = TestHelper.addOutputNode(doc, inD, null, null, true);

            AndNode pAD = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null, true).provider)).get();
            Assert.assertEquals(pAD.provider, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null, true).provider)));

            AndNode pAB = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null, true).provider)).get();
            Assert.assertEquals(pAB.provider, pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null, true).provider)));

            AndNode pAC = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null, true).provider)).get();
            Assert.assertEquals(pAC.provider, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null, true).provider)));

            AndNode pBC = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null, true).provider)).get();
            Assert.assertEquals(pBC.provider, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null, true).provider)));

            AndNode pBD = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null, true).provider)).get();
            Assert.assertEquals(pBD.provider, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null, true).provider)));

            AndNode pCD = pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null, true).provider)).get();
            Assert.assertEquals(pCD.provider, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null, true).provider)));

            AndNode pABC = pAB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null, true).provider)).get();
            Assert.assertEquals(pABC.provider, pAC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null, true).provider)));
            Assert.assertEquals(pABC.provider, pBC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null, true).provider)));

            AndNode pABD = pAB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null, true).provider)).get();
            Assert.assertEquals(pABD.provider, pAD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null, true).provider)));
            Assert.assertEquals(pABD.provider, pBD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null, true).provider)));

            AndNode pACD = pAC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null, true).provider)).get();
            Assert.assertEquals(pACD.provider, pAD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null, true).provider)));
            Assert.assertEquals(pACD.provider, pCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null, true).provider)));

            AndNode pBCD = pBC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null, true).provider)).get();
            Assert.assertEquals(pBCD.provider, pBD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null, true).provider)));
            Assert.assertEquals(pBCD.provider, pCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null, true).provider)));

            AndNode pABCD = pABC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null, true).provider)).get();
            Assert.assertEquals(pABCD.provider, pABD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null, true).provider)));
            Assert.assertEquals(pABCD.provider, pACD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null, true).provider)));
            Assert.assertEquals(pABCD.provider, pBCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null, true).provider)));

            Assert.assertEquals(4, pABCD.parents.size());
            Assert.assertEquals(null, pABCD.andChildren);
        }
    }


    @Test
    public void testMultipleActivation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");

        Neuron.init(m.createNeuron("ABC"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA
                        ).setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Synapse.Builder()
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);
        inC.addInput(doc, 0, 1);
    }
}
