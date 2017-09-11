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


import org.aika.Input;
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.Provider;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.network.TestHelper;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

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
            m.initAndNeuron(m.createNeuron("ABC"),
                    0.001,
                    new Input()
                            .setNeuron(inA)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true),
                    new Input()
                            .setNeuron(inB)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true),
                    new Input()
                            .setNeuron(inC)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true)
            );

            Document doc = m.createDocument(null, 0);


            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null);

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
            m.initAndNeuron(m.createNeuron("BCD"),
                    0.001,
                    new Input()
                            .setNeuron(inB)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true),
                    new Input()
                            .setNeuron(inC)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true),
                    new Input()
                            .setNeuron(inD)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true)
            );

            Document doc = m.createDocument(null, 0);

            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null);
            InputNode pD = TestHelper.addOutputNode(doc, inD, null, null);

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
            m.initAndNeuron(m.createNeuron("ABCD"),
                    0.001,
                    new Input()
                            .setNeuron(inA)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true),
                    new Input()
                            .setNeuron(inB)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true),
                    new Input()
                            .setNeuron(inC)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true),
                    new Input()
                            .setNeuron(inD)
                            .setWeight(1.0f)
                            .setRecurrent(false)
                            .setMinInput(1.0f)
                            .setRangeMatch(RangeRelation.EQUALS)
                            .setRangeOutput(true)
            );

            Document doc = m.createDocument(null, 0);

            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null);
            InputNode pD = TestHelper.addOutputNode(doc, inD, null, null);

            AndNode pAD = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null).provider)).get();
            Assert.assertEquals(pAD.provider, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null).provider)));

            AndNode pAB = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null).provider)).get();
            Assert.assertEquals(pAB.provider, pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null).provider)));

            AndNode pAC = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null).provider)).get();
            Assert.assertEquals(pAC.provider, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null).provider)));

            AndNode pBC = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null).provider)).get();
            Assert.assertEquals(pBC.provider, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null).provider)));

            AndNode pBD = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null).provider)).get();
            Assert.assertEquals(pBD.provider, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null).provider)));

            AndNode pCD = pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null).provider)).get();
            Assert.assertEquals(pCD.provider, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null).provider)));

            AndNode pABC = pAB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null).provider)).get();
            Assert.assertEquals(pABC.provider, pAC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null).provider)));
            Assert.assertEquals(pABC.provider, pBC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null).provider)));

            AndNode pABD = pAB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null).provider)).get();
            Assert.assertEquals(pABD.provider, pAD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null).provider)));
            Assert.assertEquals(pABD.provider, pBD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null).provider)));

            AndNode pACD = pAC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null).provider)).get();
            Assert.assertEquals(pACD.provider, pAD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null).provider)));
            Assert.assertEquals(pACD.provider, pCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null).provider)));

            AndNode pBCD = pBC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null).provider)).get();
            Assert.assertEquals(pBCD.provider, pBD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null).provider)));
            Assert.assertEquals(pBCD.provider, pCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null).provider)));

            AndNode pABCD = pABC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null).provider)).get();
            Assert.assertEquals(pABCD.provider, pABD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null).provider)));
            Assert.assertEquals(pABCD.provider, pACD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null).provider)));
            Assert.assertEquals(pABCD.provider, pBCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null).provider)));

            Assert.assertEquals(4, pABCD.parents.size());
            Assert.assertEquals(null, pABCD.andChildren);
        }
    }



    @Test
    public void buildPatternLattice() {
        Model m = new Model();

        AndNode.MAX_RID_RANGE = 1;
        AndNode.minFrequency = 1;
        m.numberOfPositions = 100;


        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");


        Document doc = m.createDocument("aaaaaaaaaa", 0);


        InputNode pANode = TestHelper.addOutputNode(doc, inA, 0, null);
        InputNode pBNode = TestHelper.addOutputNode(doc, inB, 0, null);
        InputNode pCNode = TestHelper.addOutputNode(doc, inC, 0, null);
        InputNode pDNode = TestHelper.addOutputNode(doc, inD, 0, null);


        doc.bestInterpretation = Arrays.asList(doc.bottom);
        doc.train();

        inA.addInput(doc, 0, 1, 0);

        doc.train();

        Assert.assertEquals(1, pANode.frequency, 0.01);
        Assert.assertEquals(null, pANode.andChildren);


        inB.addInput(doc, 0, 1, 0);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(1, pBNode.frequency, 0.01);
//        Assert.assertEquals(0, pBNode.andChildren.size());


        inB.addInput(doc, 2, 3, 1);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(2, pBNode.frequency, 0.01);
        Assert.assertEquals(1, pBNode.andChildren.size());


        inA.addInput(doc, 2, 3, 1);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(1, pANode.andChildren.size());
        Assert.assertEquals(1, pBNode.andChildren.size());

        Assert.assertEquals(2, pBNode.frequency, 0.01);

        AndNode pAB = pANode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)).get();
        Assert.assertEquals(pAB.provider, pBNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

        Assert.assertEquals(1, pAB.frequency, 0.01);
        Assert.assertEquals(2, pAB.parents.size());
        Assert.assertEquals(pANode.provider, pAB.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));
        Assert.assertEquals(pBNode.provider, pAB.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));


        inC.addInput(doc, 4, 5, 2);
        doc.train();

        Assert.assertEquals(1, pCNode.frequency, 0.01);
        Assert.assertEquals(null, pCNode.andChildren);

        Assert.assertEquals(2, pAB.parents.size());


        inB.addInput(doc, 4, 5, 2);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(3, pBNode.frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());


        inB.addInput(doc, 6, 7, 3);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(4, pBNode.frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());

        inC.addInput(doc, 6, 7, 3);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(4, pBNode.frequency, 0.01);
        Assert.assertEquals(2, pCNode.frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(1, pCNode.andChildren.size());

        AndNode pBC = pBNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)).get();
        Assert.assertEquals(pBC.provider, pCNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));

        Assert.assertEquals(1, pBC.frequency, 0.01);
        Assert.assertEquals(2, pBC.parents.size());
        Assert.assertEquals(pBNode.provider, pBC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)));
        Assert.assertEquals(pCNode.provider, pBC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));


        inA.addInput(doc, 4, 5, 2);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(3, pANode.frequency, 0.01);
        Assert.assertEquals(2, pAB.frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());


        inA.addInput(doc, 8, 9, 4);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(4, pANode.frequency, 0.01);
        Assert.assertEquals(2, pAB.frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

        inC.addInput(doc, 8, 9, 4);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(3, pCNode.frequency, 0.01);
        Assert.assertEquals(2, pAB.frequency, 0.01);
        Assert.assertEquals(1, pBC.frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

        AndNode pAC = pCNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)).get();
        Assert.assertEquals(pAC.provider, pANode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)));

        Assert.assertEquals(1, pAC.frequency, 0.01);
        Assert.assertEquals(2, pAC.parents.size());
        Assert.assertEquals(pANode.provider, pAC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)));
        Assert.assertEquals(pCNode.provider, pAC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));


        Assert.assertEquals(3, pCNode.frequency, 0.01);
        Assert.assertEquals(2, pAB.frequency, 0.01);
        Assert.assertEquals(1, pBC.frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

//        Assert.assertEquals(1, pAB.andChildren.size());
//        Assert.assertEquals(1, pAC.andChildren.size());
//        Assert.assertEquals(1, pBC.andChildren.size());

        Assert.assertEquals(1, pAC.frequency, 0.01);
        Assert.assertEquals(2, pAC.parents.size());
        Assert.assertEquals(pANode.provider, pAC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)));
        Assert.assertEquals(pCNode.provider, pAC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));


        inB.addInput(doc, 8, 9, 4);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(5, pBNode.frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());
        Assert.assertEquals(1, pAB.andChildren.size());
        Assert.assertEquals(3, pAB.frequency, 0.01);
        Assert.assertEquals(1, pBC.andChildren.size());
        Assert.assertEquals(1, pAC.andChildren.size());

        AndNode pABC = pAB.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)).get();
        Assert.assertEquals(pABC.provider, pAC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));
        Assert.assertEquals(pABC.provider, pBC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

//        Assert.assertEquals(1, pABC.frequency, 0.01);
        Assert.assertEquals(3, pABC.parents.size());
        Assert.assertEquals(pAB.provider, pABC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)));
        Assert.assertEquals(pAC.provider, pABC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));
        Assert.assertEquals(pBC.provider, pABC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));


        inD.addInput(doc, 0, 1, 0);
        m.resetFrequency();
        doc.train();

        inD.addInput(doc, 4, 5, 2);
        m.resetFrequency();
        doc.train();

        inA.addInput(doc, 10, 11, 5);
        inB.addInput(doc, 10, 11, 5);
        inC.addInput(doc, 10, 11, 5);
        inD.addInput(doc, 10, 11, 5);
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(3, pBNode.andChildren.size());
        Assert.assertEquals(3, pDNode.andChildren.size());

        AndNode pAD = pANode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pAD.provider, pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

        AndNode pBD = pBNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pBD.provider, pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));

        Assert.assertEquals(2, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, pBD.frequency, 0.01);
        Assert.assertEquals(2, pBD.parents.size());

        AndNode pABD = pAB.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pABD.provider, pAD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));
        Assert.assertEquals(pABD.provider, pBD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

//        Assert.assertEquals(2, pABD.frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());
        Assert.assertEquals(pAB.provider, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)));
        Assert.assertEquals(pAD.provider, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));
        Assert.assertEquals(pBD.provider, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

        Assert.assertEquals(1, pABC.andChildren.size());
        Assert.assertEquals(1, pABD.andChildren.size());



        Assert.assertEquals(3, pBNode.andChildren.size());
        Assert.assertEquals(3, pDNode.andChildren.size());

        pAD = pANode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pAD.provider, pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

        pBD = pBNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pBD.provider, pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));

        Assert.assertEquals(2, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, pBD.frequency, 0.01);
        Assert.assertEquals(2, pBD.parents.size());

        pABD = pAB.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pABD.provider, pAD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));
        Assert.assertEquals(pABD.provider, pBD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

        Assert.assertEquals(1, pABD.frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());
        Assert.assertEquals(pAB.provider, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)));
        Assert.assertEquals(pAD.provider, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));
        Assert.assertEquals(pBD.provider, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

        Assert.assertEquals(1, pABC.andChildren.size());
        Assert.assertEquals(1, pABD.andChildren.size());



        inD.addInput(doc, 8, 9, 4);
        m.resetFrequency();
        doc.train();
        m.resetFrequency();
        doc.train();
        m.resetFrequency();
        doc.train();

        AndNode pACD = pAC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();

        Assert.assertEquals(3, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, pABD.frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());

        AndNode pCD = pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)).get();
        Assert.assertEquals(pCD.provider, pCNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)));

        Assert.assertEquals(2, pCD.frequency, 0.01);

        pACD = pAC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pACD.provider, pAD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)));
        Assert.assertEquals(pACD.provider, pCD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

        Assert.assertEquals(1, pACD.frequency, 0.01);
        Assert.assertEquals(3, pACD.parents.size());

        AndNode pBCD = pBC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pBCD.provider, pBD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)));
        Assert.assertEquals(pBCD.provider, pCD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));

        Assert.assertEquals(1, pACD.frequency, 0.01);
        Assert.assertEquals(3, pBCD.parents.size());

        AndNode pABCD = pABC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null).provider)).get();
        Assert.assertEquals(pABCD.provider, pABD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null).provider)));
        Assert.assertEquals(pABCD.provider, pACD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null).provider)));
        Assert.assertEquals(pABCD.provider, pBCD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null).provider)));

// The Pattern ABC has no Activation yet, since it has just been created.
//        Assert.assertEquals(1, pABCD.frequency, 0.01);

        Assert.assertEquals(4, pABCD.parents.size());
        Assert.assertEquals(null, pABCD.andChildren);

        Assert.assertNull(TestHelper.get(doc, pCNode, new Range(0, 1), doc.bottom));

// ======================================================================

        inB.removeInput(doc, 4, 5, 2);

        m.resetFrequency();
        doc.train();
        m.resetFrequency();
        doc.train();

        Assert.assertNull(TestHelper.get(doc, pABC, new Range(0, 1), doc.bottom));

        Assert.assertEquals(3, pAB.frequency, 0.01);
        Assert.assertEquals(2, pAB.andChildren.size());

        Assert.assertEquals(2, pAC.frequency, 0.01);
        Assert.assertEquals(2, pAC.andChildren.size());

        Assert.assertEquals(3, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.andChildren.size());

        Assert.assertEquals(3, pBC.frequency, 0.01);
        Assert.assertEquals(2, pBC.andChildren.size());

        Assert.assertEquals(2, pBD.frequency, 0.01);
        Assert.assertEquals(2, pBD.andChildren.size());

        Assert.assertEquals(2, pCD.frequency, 0.01);
        Assert.assertEquals(2, pCD.andChildren.size());

        Assert.assertEquals(2, pABD.frequency, 0.01);
        Assert.assertEquals(1, pABD.andChildren.size());
        Assert.assertEquals(3, pABD.parents.size());

        Assert.assertEquals(1, pACD.frequency, 0.01);
        Assert.assertEquals(1, pACD.andChildren.size());
        Assert.assertEquals(3, pACD.parents.size());


        Assert.assertEquals(3, pAB.frequency, 0.01);
        Assert.assertEquals(2, pAB.andChildren.size());

        Assert.assertEquals(2, pAC.frequency, 0.01);
        Assert.assertEquals(2, pAB.andChildren.size());

        Assert.assertEquals(3, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.andChildren.size());

        Assert.assertEquals(3, pBC.frequency, 0.01);
        Assert.assertEquals(2, pBC.andChildren.size());

        Assert.assertEquals(2, pBD.frequency, 0.01);
        Assert.assertEquals(2, pBD.andChildren.size());

        Assert.assertEquals(2, pCD.frequency, 0.01);
        Assert.assertEquals(2, pCD.andChildren.size());

        Assert.assertEquals(2, pABD.frequency, 0.01);
        Assert.assertEquals(1, pABD.andChildren.size());
        Assert.assertEquals(3, pABD.parents.size());

        Assert.assertEquals(1, pACD.frequency, 0.01);
        Assert.assertEquals(1, pACD.andChildren.size());
        Assert.assertEquals(3, pACD.parents.size());


        inD.removeInput(doc, 0, 1, 0);
        m.resetFrequency();
        doc.train();
        m.resetFrequency();
        doc.train();

        Assert.assertEquals(3, pDNode.andChildren.size());

        Assert.assertEquals(3, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.andChildren.size());

        Assert.assertEquals(2, pCD.frequency, 0.01);
        Assert.assertEquals(2, pCD.andChildren.size());

        Assert.assertEquals(1, pACD.frequency, 0.01);
        Assert.assertEquals(1, pACD.andChildren.size());
        Assert.assertEquals(3, pACD.parents.size());

    }


    @Test
    public void testMultipleActivation() {
        Model m = new Model();
        AndNode.minFrequency = 10;

        Neuron inA = m.createNeuron("A");
        Node inANode = inA.get().node.get();

        Neuron inB = m.createNeuron("B");
        Node inBNode = inB.get().node.get();

        Neuron inC = m.createNeuron("C");
        Node inCNode = inC.get().node.get();

        m.initAndNeuron(m.createNeuron("ABC"),
                0.001,
                new Input().setNeuron(inA).setWeight(1.0f).setRecurrent(false).setMinInput(1.0f),
                new Input().setNeuron(inB).setWeight(1.0f).setRecurrent(false).setMinInput(1.0f),
                new Input().setNeuron(inC).setWeight(1.0f).setRecurrent(false).setMinInput(1.0f)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);


        InputNode pANode = TestHelper.addOutputNode(doc, inA, null, null);
        InputNode pBNode = TestHelper.addOutputNode(doc, inB, null, null);
        InputNode pCNode = TestHelper.addOutputNode(doc, inC, null, null);

        NodeActivation inA1 = new NodeActivation(0, inANode, new Range(0, 1), null, doc.bottom);

        TestHelper.addActivation(pANode, doc, inA1);

        NodeActivation inB1 = new NodeActivation(1, inBNode, new Range(0, 1), null, doc.bottom);

        TestHelper.addActivation(pBNode, doc, inB1);

        NodeActivation inC1 = new NodeActivation(2, inCNode, new Range(0, 1), null, doc.bottom);

        TestHelper.addActivation(pCNode, doc, inC1);
    }
}
