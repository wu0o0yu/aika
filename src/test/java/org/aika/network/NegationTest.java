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
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode;
import org.aika.lattice.InputNode;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.RangeMatch;
import org.aika.neuron.Synapse.RangeSignal;

/**
 *
 * @author Lukas Molzberger
 */
public class NegationTest {


    @Test
    public void testTwoNegativeInputs1() {
        Model m = new Model();
        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");
        InputNeuron inC = m.createOrLookupInputSignal("C");

        Neuron abcN = new Neuron("ABC");

        m.createAndNeuron(abcN,
                0.5,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(inB)
                        .setWeight(-10.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inC)
                        .setWeight(-10.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        inA.addInput(doc, 0, 11);

        System.out.println(doc.networkStateToString(true, true));
        Assert.assertNotNull(Activation.get(doc, abcN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));

        Option o1 = Option.addPrimitive(doc);

        inB.addInput(doc, 2, 7, o1);

        System.out.println(doc.networkStateToString(true, true));

        Option o2 = Option.addPrimitive(doc);

        inC.addInput(doc, 4, 9, o2);

        System.out.println(doc.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(doc, abcN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
    }


    @Test
    public void testTwoNegativeInputs2() {
        Model m = new Model();

        InputNeuron inA = m.createOrLookupInputSignal("A");

        InputNeuron inB = m.createOrLookupInputSignal("B");

        InputNeuron inC = m.createOrLookupInputSignal("C");

        Neuron abcN = new Neuron("ABC");

        Neuron outN = m.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(abcN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        m.createAndNeuron(abcN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inB)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inC)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        inA.addInput(doc, 0, 11);

        System.out.println(doc.networkStateToString(true, true));

        Option ob = Option.addPrimitive(doc);
        inB.addInput(doc, 2, 7, ob);

        System.out.println(doc.networkStateToString(true, true));

        Option oc = Option.addPrimitive(doc);
        inC.addInput(doc, 4, 9, oc);

        System.out.println(doc.networkStateToString(true, true));

//        Assert.assertNull(Activation.get(t, outN.node, 0, new Range(0, 11), Range.Relation.EQUALS, null, null, null));

        inB.removeInput(doc, 2, 7, ob);

        System.out.println(doc.networkStateToString(true, true));

        inC.removeInput(doc, 4, 9, oc);

        System.out.println(doc.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
    }


    @Test
    public void testSimpleNegation1() {
        Model m = new Model();

        InputNeuron inA = m.createOrLookupInputSignal("A");

        Neuron asN = new Neuron("AS");

        InputNeuron inS = m.createOrLookupInputSignal("S");

        Neuron outN = m.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        m.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(Input.RangeRelation.NONE)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        Option o = Option.addPrimitive(doc);

        inS.addInput(doc, 3, 8, o);

        System.out.println(doc.networkStateToString(true, true));

        inA.addInput(doc, 0, 11);

        System.out.println(doc.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
        Assert.assertFalse(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inS.removeInput(doc, 3, 8, o);

        System.out.println(doc.networkStateToString(true, true));

        Assert.assertTrue(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.removeInput(doc, 0, 11);

        System.out.println(doc.networkStateToString(true, true));

        doc.clearActivations();
    }


    @Test
    public void testSimpleNegation2() {
        Model m = new Model();

        InputNeuron inA = m.createOrLookupInputSignal("A");

        Neuron asN = new Neuron("AS");

        InputNeuron inS = m.createOrLookupInputSignal("S");

        Neuron outN = m.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        m.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.CONTAINS)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        Option o = Option.addPrimitive(doc);

        inS.addInput(doc, 3, 8, o);

        System.out.println(doc.networkStateToString(true, true));

        inA.addInput(doc, 0, 11);

        System.out.println(doc.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
        Assert.assertFalse(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.removeInput(doc, 0, 11);

        System.out.println(doc.networkStateToString(true, true));

        inS.removeInput(doc, 3, 8, o);

        doc.clearActivations();
    }


    @Test
    public void testSimpleNegation3() {
        Model m = new Model();

        InputNeuron inA = m.createOrLookupInputSignal("A");

        Neuron asN = new Neuron("AS");

        InputNeuron inS = m.createOrLookupInputSignal("S");

        Neuron outN = m.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        m.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.CONTAINS)
        );

        Document doc = m.createDocument("aaaaaaaaaaa", 0);

        Option o = Option.addPrimitive(doc);

        inA.addInput(doc, 0, 11);

        System.out.println(doc.networkStateToString(true, true));

        inS.addInput(doc, 3, 8, o);

        System.out.println(doc.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
        Assert.assertFalse(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inS.removeInput(doc, 3, 8, o);

        System.out.println(doc.networkStateToString(true, true));

        Assert.assertTrue(Activation.get(doc, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.removeInput(doc, 0, 11);

        System.out.println(doc.networkStateToString(true, true));

        doc.clearActivations();
    }


    @Test
    public void testNegation1() {
        Model m = new Model();
        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");

        Neuron asN = new Neuron("AS");
        Neuron absN = new Neuron("ABS");
        Neuron bsN = new Neuron("BS");

        Neuron inS = m.createOrNeuron(new Neuron("S"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(absN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true)
        );

        m.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.CONTAINS)
        );
        m.createAndNeuron(absN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.CONTAINS)
        );

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.addInput(doc, 0, 6);
            System.out.println(doc.networkStateToString(true, true));

            inB.addInput(doc, 0, 6);

            System.out.println(doc.networkStateToString(true, true));

            Assert.assertNotNull(Activation.get(doc, inS.node, null, new Range(0, 6), Range.Relation.EQUALS, null, null));
            Assert.assertEquals(2, Activation.get(doc, inS.node, null, new Range(0, 6), Range.Relation.EQUALS, null, null).key.o.orOptions.size());

            doc.clearActivations();
        }

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.addInput(doc, 0, 6);
            System.out.println(doc.networkStateToString(true, true));

            inB.addInput(doc, 3, 9);

            System.out.println(doc.networkStateToString(true, true));

//            Assert.assertNotNull(Activation.get(t, inS.node, 0, new Range(0, 6), Range.Relation.EQUALS, null, null, null));
            Assert.assertNotNull(Activation.get(doc, inS.node, null, new Range(0, 9), Range.Relation.EQUALS, null, null));
//            Assert.assertEquals(1, Activation.get(t, inS.node, 0, new Range(0, 6), Range.Relation.EQUALS, null, null, null).key.o.orOptions.size());
            Assert.assertEquals(2, Activation.get(doc, inS.node, null, new Range(0, 9), Range.Relation.EQUALS, null, null).key.o.orOptions.size());

            doc.clearActivations();
        }
    }


    @Test
    public void testNegation2() {
        Model m = new Model();

        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");
        InputNeuron inC = m.createOrLookupInputSignal("C");

        Neuron asN = new Neuron("AS");
        Neuron ascN = new Neuron("ASC");
        Neuron bsN = new Neuron("BS");

        Neuron inS = m.createOrNeuron(new Neuron("S"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ascN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(bsN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true)
        );

        m.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true)
        );
        m.createAndNeuron(ascN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setOptional(false)
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true)
        );

        m.createAndNeuron(bsN,
                0.001,
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true)
        );

        Neuron outA = m.createOrNeuron(new Neuron("OUT A"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true)
        );
        Neuron outAC = m.createOrNeuron(new Neuron("OUT AC"),
                new Input()
                        .setNeuron(ascN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true)
        );
        Neuron outB = m.createOrNeuron(new Neuron("OUT B"),
                new Input()
                        .setNeuron(bsN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);


//        asN.node.weight = 0.45;
//        ascN.node.weight = 1.0;

//        bsN.node.weight = 0.5;


        inA.addInput(doc, 0, 6);

        System.out.println(doc.networkStateToString(true, true));

        inB.addInput(doc, 0, 6);

        System.out.println(doc.networkStateToString(true, true));

        inC.addInput(doc, 0, 6);

        System.out.println(doc.networkStateToString(true, true));

        doc.process();

        System.out.println(doc.networkStateToString(false, true));
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

        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNode inANode = (InputNode) inA.node;

        InputNeuron inB = m.createOrLookupInputSignal("B");
        InputNode inBNode = (InputNode) inB.node;


        Neuron pC = new Neuron("C");
        Neuron pD = new Neuron("D");

        m.createAndNeuron(pC,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(pD)
                        .setWeight(-2.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        m.createAndNeuron(pD,
                0.001,
                new Input()
                        .setNeuron(inB)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(pC)
                        .setWeight(-2.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );


        InputNeuron inG = m.createOrLookupInputSignal("G");
        InputNode inGNode = (InputNode) inG.node;

        Neuron pH = m.createAndNeuron(new Neuron("H"),
                0.001,
                new Input()
                        .setNeuron(pC)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(pD)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inG)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);
        inG.addInput(doc, 0, 1);

        System.out.println(doc.networkStateToString(true, true));

        Assert.assertNotNull(pC.node.getFirstActivation(doc));
        Assert.assertNotNull(pD.node.getFirstActivation(doc));

        // Die Optionen 0 und 2 stehen in Konflikt. Da sie aber jetzt in Oder Optionen eingebettet sind, werden sie nicht mehr ausgefiltert.
//        Assert.assertNull(pH.node.getFirstActivation(t));
    }


}
