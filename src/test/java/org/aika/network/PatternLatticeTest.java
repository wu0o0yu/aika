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


import org.aika.Activation;
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode;
import org.aika.lattice.AndNode.Refinement;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
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
        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");
        InputNeuron inC = m.createOrLookupInputSignal("C");
        InputNeuron inD = m.createOrLookupInputSignal("D");

        {
            m.createAndNeuron(new Neuron("ABC"),
                    0.001,
                    new Input()
                            .setNeuron(inA)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0),
                    new Input()
                            .setNeuron(inB)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0),
                    new Input()
                            .setNeuron(inC)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0)
            );

            Document doc = m.createDocument(null, 0);


            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null);

            AndNode pAB = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null)));
            Assert.assertEquals(pAB, pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pAC = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null)));
            Assert.assertEquals(pAC, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pBC = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null)));
            Assert.assertEquals(pBC, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));

            Assert.assertEquals(1, pAB.andChildren.size());
            Assert.assertEquals(1, pBC.andChildren.size());
            Assert.assertEquals(1, pAC.andChildren.size());

            AndNode pABC = pAB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null)));
            Assert.assertEquals(pABC, pAC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));
            Assert.assertEquals(pABC, pBC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            Assert.assertEquals(3, pABC.parents.size());
            Assert.assertEquals(pAB, pABC.parents.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null))));
            Assert.assertEquals(pAC, pABC.parents.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));
            Assert.assertEquals(pBC, pABC.parents.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));
        }
        {
            m.createAndNeuron(new Neuron("BCD"),
                    0.001,
                    new Input()
                            .setNeuron(inB)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0),
                    new Input()
                            .setNeuron(inC)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0),
                    new Input()
                            .setNeuron(inD)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0)
            );

            Document doc = m.createDocument(null, 0);

            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null);
            InputNode pD = TestHelper.addOutputNode(doc, inD, null, null);

            AndNode pAB = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null)));
            Assert.assertEquals(pAB, pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pAC = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null)));
            Assert.assertEquals(pAC, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pBC = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null)));
            Assert.assertEquals(pBC, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));

            AndNode pBD = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pBD, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));

            AndNode pCD = pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pCD, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null))));


            Assert.assertEquals(1, pAB.andChildren.size());
            Assert.assertEquals(1, pAC.andChildren.size());
            Assert.assertEquals(2, pBC.andChildren.size());
            Assert.assertEquals(1, pBD.andChildren.size());
            Assert.assertEquals(1, pCD.andChildren.size());

            AndNode pBCD = pBC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pBCD, pBD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null))));
            Assert.assertEquals(pBCD, pCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));

            Assert.assertEquals(3, pBCD.parents.size());
            Assert.assertEquals(pBC, pBCD.parents.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null))));
            Assert.assertEquals(pBD, pBCD.parents.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null))));
            Assert.assertEquals(pCD, pBCD.parents.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));
        }
        {
            m.createAndNeuron(new Neuron("ABCD"),
                    0.001,
                    new Input()
                            .setNeuron(inA)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0),
                    new Input()
                            .setNeuron(inB)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0),
                    new Input()
                            .setNeuron(inC)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0),
                    new Input()
                            .setNeuron(inD)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setMinInput(1.0)
            );

            Document doc = m.createDocument(null, 0);

            InputNode pA = TestHelper.addOutputNode(doc, inA, null, null);
            InputNode pB = TestHelper.addOutputNode(doc, inB, null, null);
            InputNode pC = TestHelper.addOutputNode(doc, inC, null, null);
            InputNode pD = TestHelper.addOutputNode(doc, inD, null, null);

            AndNode pAD = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pAD, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pAB = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null)));
            Assert.assertEquals(pAB, pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pAC = pA.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null)));
            Assert.assertEquals(pAC, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pBC = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null)));
            Assert.assertEquals(pBC, pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));

            AndNode pBD = pB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pBD, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));

            AndNode pCD = pC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pCD, pD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null))));

            AndNode pABC = pAB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null)));
            Assert.assertEquals(pABC, pAC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));
            Assert.assertEquals(pABC, pBC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pABD = pAB.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pABD, pAD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));
            Assert.assertEquals(pABD, pBD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pACD = pAC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pACD, pAD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null))));
            Assert.assertEquals(pACD, pCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

            AndNode pBCD = pBC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pBCD, pBD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null))));
            Assert.assertEquals(pBCD, pCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));

            AndNode pABCD = pABC.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inD, null, null)));
            Assert.assertEquals(pABCD, pABD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inC, null, null))));
            Assert.assertEquals(pABCD, pACD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inB, null, null))));
            Assert.assertEquals(pABCD, pBCD.andChildren.get(new Refinement(null, TestHelper.addOutputNode(doc, inA, null, null))));

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


        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");
        InputNeuron inC = m.createOrLookupInputSignal("C");
        InputNeuron inD = m.createOrLookupInputSignal("D");


        Document doc = m.createDocument("aaaaaaaaaa", 0);


        InputNode pANode = TestHelper.addOutputNode(doc, inA, 0, null);
        InputNode pBNode = TestHelper.addOutputNode(doc, inB, 0, null);
        InputNode pCNode = TestHelper.addOutputNode(doc, inC, 0, null);
        InputNode pDNode = TestHelper.addOutputNode(doc, inD, 0, null);


        doc.selectedOption = doc.bottom;
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

        AndNode pAB = pANode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null)));
        Assert.assertEquals(pAB, pBNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

        Assert.assertEquals(1, pAB.frequency, 0.01);
        Assert.assertEquals(2, pAB.parents.size());
        Assert.assertEquals(pANode, pAB.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));
        Assert.assertEquals(pBNode, pAB.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));


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

        AndNode pBC = pBNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null)));
        Assert.assertEquals(pBC, pCNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));

        Assert.assertEquals(1, pBC.frequency, 0.01);
        Assert.assertEquals(2, pBC.parents.size());
        Assert.assertEquals(pBNode, pBC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null))));
        Assert.assertEquals(pCNode, pBC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));


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

        AndNode pAC = pCNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null)));
        Assert.assertEquals(pAC, pANode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null))));

        Assert.assertEquals(1, pAC.frequency, 0.01);
        Assert.assertEquals(2, pAC.parents.size());
        Assert.assertEquals(pANode, pAC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null))));
        Assert.assertEquals(pCNode, pAC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));


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
        Assert.assertEquals(pANode, pAC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null))));
        Assert.assertEquals(pCNode, pAC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));


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

        AndNode pABC = pAB.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null)));
        Assert.assertEquals(pABC, pAC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));
        Assert.assertEquals(pABC, pBC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

//        Assert.assertEquals(1, pABC.frequency, 0.01);
        Assert.assertEquals(3, pABC.parents.size());
        Assert.assertEquals(pAB, pABC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null))));
        Assert.assertEquals(pAC, pABC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));
        Assert.assertEquals(pBC, pABC.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));


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

        AndNode pAD = pANode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pAD, pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

        AndNode pBD = pBNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pBD, pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));

        Assert.assertEquals(2, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, pBD.frequency, 0.01);
        Assert.assertEquals(2, pBD.parents.size());

        AndNode pABD = pAB.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pABD, pAD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));
        Assert.assertEquals(pABD, pBD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

//        Assert.assertEquals(2, pABD.frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());
        Assert.assertEquals(pAB, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null))));
        Assert.assertEquals(pAD, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));
        Assert.assertEquals(pBD, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

        Assert.assertEquals(1, pABC.andChildren.size());
        Assert.assertEquals(1, pABD.andChildren.size());



        Assert.assertEquals(3, pBNode.andChildren.size());
        Assert.assertEquals(3, pDNode.andChildren.size());

        pAD = pANode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pAD, pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

        pBD = pBNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pBD, pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));

        Assert.assertEquals(2, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, pBD.frequency, 0.01);
        Assert.assertEquals(2, pBD.parents.size());

        pABD = pAB.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pABD, pAD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));
        Assert.assertEquals(pABD, pBD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

        Assert.assertEquals(1, pABD.frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());
        Assert.assertEquals(pAB, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null))));
        Assert.assertEquals(pAD, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));
        Assert.assertEquals(pBD, pABD.parents.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

        Assert.assertEquals(1, pABC.andChildren.size());
        Assert.assertEquals(1, pABD.andChildren.size());



        inD.addInput(doc, 8, 9, 4);
        m.resetFrequency();
        doc.train();
        m.resetFrequency();
        doc.train();
        m.resetFrequency();
        doc.train();

        AndNode pACD = pAC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));

        Assert.assertEquals(3, pAD.frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, pABD.frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());

        AndNode pCD = pDNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null)));
        Assert.assertEquals(pCD, pCNode.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null))));

        Assert.assertEquals(2, pCD.frequency, 0.01);

        pACD = pAC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pACD, pAD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null))));
        Assert.assertEquals(pACD, pCD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

        Assert.assertEquals(1, pACD.frequency, 0.01);
        Assert.assertEquals(3, pACD.parents.size());

        AndNode pBCD = pBC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pBCD, pBD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null))));
        Assert.assertEquals(pBCD, pCD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));

        Assert.assertEquals(1, pACD.frequency, 0.01);
        Assert.assertEquals(3, pBCD.parents.size());

        AndNode pABCD = pABC.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inD, 0, null)));
        Assert.assertEquals(pABCD, pABD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inC, 0, null))));
        Assert.assertEquals(pABCD, pACD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inB, 0, null))));
        Assert.assertEquals(pABCD, pBCD.andChildren.get(new Refinement(0, TestHelper.addOutputNode(doc, inA, 0, null))));

        Assert.assertEquals(1, pABCD.frequency, 0.01);
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

        InputNeuron inA = m.createOrLookupInputSignal("A");
        Node inANode = inA.node;

        InputNeuron inB = m.createOrLookupInputSignal("B");
        Node inBNode = inB.node;

        InputNeuron inC = m.createOrLookupInputSignal("C");
        Node inCNode = inC.node;

        m.createAndNeuron(new Neuron("ABC"),
                0.001,
                new Input().setNeuron(inA).setWeight(1.0).setRecurrent(false).setMinInput(1.0),
                new Input().setNeuron(inB).setWeight(1.0).setRecurrent(false).setMinInput(1.0),
                new Input().setNeuron(inC).setWeight(1.0).setRecurrent(false).setMinInput(1.0)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);


        InputNode pANode = TestHelper.addOutputNode(doc, inA, null, null);
        InputNode pBNode = TestHelper.addOutputNode(doc, inB, null, null);
        InputNode pCNode = TestHelper.addOutputNode(doc, inC, null, null);

        Activation inA1 = new Activation(0, inANode, new Range(0, 1), null, doc.bottom);

        TestHelper.addActivation(pANode, doc, inA1);

        Activation inB1 = new Activation(1, inBNode, new Range(0, 1), null, doc.bottom);

        TestHelper.addActivation(pBNode, doc, inB1);

        Activation inC1 = new Activation(2, inCNode, new Range(0, 1), null, doc.bottom);

        TestHelper.addActivation(pCNode, doc, inC1);
    }
}
