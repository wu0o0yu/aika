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


import org.aika.Provider;
import org.aika.lattice.NodeActivation;
import org.aika.Input;
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode;
import org.aika.lattice.Node;
import org.aika.lattice.OrNode;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.Input.RangeRelation.NONE;
import static org.aika.corpus.Range.Operator.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class NegationTest {


    @Test
    public void testTwoNegativeInputs1() {
        Model m = new Model();
        Provider<Neuron> inA = m.createNeuron("A");
        Provider<Neuron> inB = m.createNeuron("B");
        Provider<Neuron> inC = m.createNeuron("C");

        Provider<Neuron> abcN = m.createNeuron("ABC");

        m.initAndNeuron(abcN,
                0.5,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(0.95f),
                new Input()
                        .setNeuron(inB)
                        .setWeight(-10.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f),
                new Input()
                        .setNeuron(inC)
                        .setWeight(-10.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        inA.get().addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));
        Assert.assertNotNull(NodeActivation.get(doc, abcN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null));

        InterprNode o1 = InterprNode.addPrimitive(doc);

        inB.get().addInput(doc, 2, 7, o1);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        InterprNode o2 = InterprNode.addPrimitive(doc);

        inC.get().addInput(doc, 4, 9, o2);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(NodeActivation.get(doc, abcN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null));
    }


    @Test
    public void testTwoNegativeInputs2() {
        Model m = new Model();

        Provider<Neuron> inA = m.createNeuron("A");

        Provider<Neuron> inB = m.createNeuron("B");

        Provider<Neuron> inC = m.createNeuron("C");

        Provider<Neuron> abcN = m.createNeuron("ABC");

        Provider<Neuron> outN = m.initOrNeuron(m.createNeuron("OUT"),
                new Input()
                        .setNeuron(abcN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );

        m.initAndNeuron(abcN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f),
                new Input()
                        .setNeuron(inB)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f),
                new Input()
                        .setNeuron(inC)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        inA.get().addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        InterprNode ob = InterprNode.addPrimitive(doc);
        inB.get().addInput(doc, 2, 7, ob);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        InterprNode oc = InterprNode.addPrimitive(doc);
        inC.get().addInput(doc, 4, 9, oc);

        System.out.println(doc.neuronActivationsToString(true, false, true));

//        Assert.assertNull(Activation.get(t, outN.node, 0, new Range(0, 11), Range.Relation.EQUALS, null, null, null));

        inB.get().removeInput(doc, 2, 7, ob);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inC.get().removeInput(doc, 4, 9, oc);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null));
    }


    @Test
    public void testSimpleNegation1() {
        Model m = new Model();

        Provider<Neuron> inA = m.createNeuron("A");

        Provider<Neuron> asN = m.createNeuron("AS");

        Provider<Neuron> inS = m.createNeuron("S");

        Provider<Neuron> outN = m.initOrNeuron(m.createNeuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );

        m.initAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(NONE)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        InterprNode o = InterprNode.addPrimitive(doc);

        inS.get().addInput(doc, 3, 8, o);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inA.get().addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null));
        Assert.assertFalse(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inS.get().removeInput(doc, 3, 8, o);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertTrue(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.get().removeInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        doc.clearActivations();
    }


    @Test
    public void testSimpleNegation2() {
        Model m = new Model();

        Provider<Neuron> inA = m.createNeuron("A");

        Provider<Neuron> asN = m.createNeuron("AS");

        Provider<Neuron> inS = m.createNeuron("S");

        Provider<Neuron> outN = m.initOrNeuron(m.createNeuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );

        m.initAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.CONTAINS)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        InterprNode o = InterprNode.addPrimitive(doc);

        inS.get().addInput(doc, 3, 8, o);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inA.get().addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null));
        Assert.assertFalse(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.get().removeInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inS.get().removeInput(doc, 3, 8, o);

        doc.clearActivations();
    }


    @Test
    public void testSimpleNegation3() {
        Model m = new Model();

        Provider<Neuron> inA = m.createNeuron("A");

        Provider<Neuron> asN = m.createNeuron("AS");

        Provider<Neuron> inS = m.createNeuron("S");

        Provider<Neuron> outN = m.initOrNeuron(m.createNeuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );

        m.initAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.CONTAINS)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        InterprNode o = InterprNode.addPrimitive(doc);

        inA.get().addInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inS.get().addInput(doc, 3, 8, o);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null));
        Assert.assertFalse(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inS.get().removeInput(doc, 3, 8, o);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertTrue(NodeActivation.get(doc, outN.get().node.get(), null, new Range(0, 11), EQUALS, EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.get().removeInput(doc, 0, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        doc.clearActivations();
    }


    @Test
    public void testNegation1() {
        Model m = new Model();
        Provider<Neuron> inA = m.createNeuron("A");
        Provider<Neuron> inB = m.createNeuron("B");

        Provider<Neuron> asN = m.createNeuron("AS");
        Provider<Neuron> absN = m.createNeuron("ABS");
        Provider<Neuron> bsN = m.createNeuron("BS");

        Provider<Neuron> inS = m.initOrNeuron(m.createNeuron("S"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(absN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        m.initAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.CONTAINS)
        );
        m.initAndNeuron(absN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setStartRangeMatch(EQUALS)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setEndRangeMatch(EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.CONTAINS)
        );

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.get().addInput(doc, 0, 6);
            System.out.println(doc.neuronActivationsToString(true, false, true));

            inB.get().addInput(doc, 0, 6);

            System.out.println(doc.neuronActivationsToString(true, false, true));

            Assert.assertNotNull(NodeActivation.get(doc, inS.get().node.get(), null, new Range(0, 6), EQUALS, EQUALS, null, null));
            Assert.assertEquals(2, NodeActivation.get(doc, inS.get().node.get(), null, new Range(0, 6), EQUALS, EQUALS, null, null).key.o.orInterprNodes.size());

            doc.clearActivations();
        }

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.get().addInput(doc, 0, 6);
            System.out.println(doc.neuronActivationsToString(true, false, true));

            inB.get().addInput(doc, 3, 9);

            System.out.println(doc.neuronActivationsToString(true, false, true));

//            Assert.assertNotNull(Activation.get(t, inS.node, 0, new Range(0, 6), EQUALS, EQUALS, null, null, null));
            Assert.assertNotNull(NodeActivation.get(doc, inS.get().node.get(), null, new Range(0, 9), EQUALS, EQUALS, null, null));
//            Assert.assertEquals(1, Activation.get(t, inS.node, 0, new Range(0, 6), EQUALS, EQUALS, null, null, null).key.o.orInterprNodes.size());
            Assert.assertEquals(1, NodeActivation.get(doc, inS.get().node.get(), null, new Range(0, 6), EQUALS, EQUALS, null, null).key.o.orInterprNodes.size());
            Assert.assertEquals(1, NodeActivation.get(doc, inS.get().node.get(), null, new Range(0, 9), EQUALS, EQUALS, null, null).key.o.orInterprNodes.size());

            doc.clearActivations();
        }
    }


    @Test
    public void testNegation2() {
        Model m = new Model();

        Provider<Neuron> inA = m.createNeuron("A");
        Provider<Neuron> inB = m.createNeuron("B");
        Provider<Neuron> inC = m.createNeuron("C");

        Provider<Neuron> asN = m.createNeuron("AS");
        Provider<Neuron> ascN = m.createNeuron("ASC");
        Provider<Neuron> bsN = m.createNeuron("BS");

        Provider<Neuron> inS = m.initOrNeuron(m.createNeuron("S"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ascN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(bsN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        m.initAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );
        m.initAndNeuron(ascN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setOptional(false)
                        .setNeuron(inC)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        m.initAndNeuron(bsN,
                0.001,
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        Provider<Neuron> outA = m.initOrNeuron(m.createNeuron("OUT A"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );
        Provider<Neuron> outAC = m.initOrNeuron(m.createNeuron("OUT AC"),
                new Input()
                        .setNeuron(ascN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );
        Provider<Neuron> outB = m.initOrNeuron(m.createNeuron("OUT B"),
                new Input()
                        .setNeuron(bsN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);


//        asN.node.weight = 0.45;
//        ascN.node.weight = 1.0;

//        bsN.node.weight = 0.5;


        inA.get().addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inB.get().addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        inC.get().addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        doc.process();

        System.out.println(doc.nodeActivationsToString( false, true));
    }




    /**
     *
     *       -----
     *  A ---| &  |------
     *     -*| C  |     |       ------
     *     | ------     |   G---| &  |
     *      \           |       | H  |-----
     *       \/-----------------|    |
     *       /\-----------------|    |
     *      /           |       ------
     *     | ------     |
     *     -*| &  |------
     *  B ---| D  |
     *       ------
     *
     */

    @Test
    public void testOptions() {
        Model m = new Model();
        AndNode.minFrequency = 5;

        Provider<Neuron> inA = m.createNeuron("A");
        Node inANode = inA.get().node.get();

        Provider<Neuron> inB = m.createNeuron("B");
        Node inBNode = inB.get().node.get();


        Provider<Neuron> pC = m.createNeuron("C");
        Provider<Neuron> pD = m.createNeuron("D");

        m.initAndNeuron(pC,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f),
                new Input()
                        .setNeuron(pD)
                        .setWeight(-2.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );

        m.initAndNeuron(pD,
                0.001,
                new Input()
                        .setNeuron(inB)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f),
                new Input()
                        .setNeuron(pC)
                        .setWeight(-2.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );


        Provider<Neuron> inG = m.createNeuron("G");
        OrNode inGNode = inG.get().node.get();

        Provider<Neuron> pH = m.initAndNeuron(m.createNeuron("H"),
                0.001,
                new Input()
                        .setNeuron(pC)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f),
                new Input()
                        .setNeuron(pD)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f),
                new Input()
                        .setNeuron(inG)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0f)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.get().addInput(doc, 0, 1);
        inB.get().addInput(doc, 0, 1);
        inG.get().addInput(doc, 0, 1);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertNotNull(pC.get().node.get().getFirstActivation(doc));
        Assert.assertNotNull(pD.get().node.get().getFirstActivation(doc));

        // Die Optionen 0 und 2 stehen in Konflikt. Da sie aber jetzt in Oder Optionen eingebettet sind, werden sie nicht mehr ausgefiltert.
//        Assert.assertNull(pH.node.getFirstActivation(t));
    }


}
