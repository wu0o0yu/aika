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
import org.aika.Iteration;
import org.aika.Iteration.Input;
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

/**
 *
 * @author Lukas Molzberger
 */
public class NegationTest {


    @Test
    public void testTwoNegativeInputs1() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");
        InputNeuron inB = t.createOrLookupInputSignal("B");
        InputNeuron inC = t.createOrLookupInputSignal("C");

        Neuron abcN = new Neuron("ABC");

        t.createAndNeuron(abcN,
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


        inA.addInput(t, 0, 11);

        System.out.println(t.networkStateToString(true, true));
        Assert.assertNotNull(Activation.get(t, abcN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));

        Option o1 = Option.addPrimitive(doc);

        inB.addInput(t, 2, 7, o1);

        System.out.println(t.networkStateToString(true, true));

        Option o2 = Option.addPrimitive(doc);

        inC.addInput(t, 4, 9, o2);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(t, abcN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
    }


    @Test
    public void testTwoNegativeInputs2() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");

        InputNeuron inB = t.createOrLookupInputSignal("B");

        InputNeuron inC = t.createOrLookupInputSignal("C");

        Neuron abcN = new Neuron("ABC");

        Neuron outN = t.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(abcN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        t.createAndNeuron(abcN,
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


        inA.addInput(t, 0, 11);

        System.out.println(t.networkStateToString(true, true));

        Option ob = Option.addPrimitive(doc);
        inB.addInput(t, 2, 7, ob);

        System.out.println(t.networkStateToString(true, true));

        Option oc = Option.addPrimitive(doc);
        inC.addInput(t, 4, 9, oc);

        System.out.println(t.networkStateToString(true, true));

//        Assert.assertNull(Activation.get(t, outN.node, 0, new Range(0, 11), Range.Relation.EQUALS, null, null, null));

        inB.removeInput(t, 2, 7, ob);

        System.out.println(t.networkStateToString(true, true));

        inC.removeInput(t, 4, 9, oc);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
    }


    @Test
    public void testSimpleNegation1() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");

        Neuron asN = new Neuron("AS");

        InputNeuron inS = t.createOrLookupInputSignal("S");

        Neuron outN = t.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        t.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );


        Option o = Option.addPrimitive(doc);

        inS.addInput(t, 3, 8, o);

        System.out.println(t.networkStateToString(true, true));

        inA.addInput(t, 0, 11);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
        Assert.assertFalse(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inS.removeInput(t, 3, 8, o);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertTrue(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.removeInput(t, 0, 11);

        System.out.println(t.networkStateToString(true, true));

        t.clearActivations();
    }


    @Test
    public void testSimpleNegation2() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");

        Neuron asN = new Neuron("AS");

        InputNeuron inS = t.createOrLookupInputSignal("S");

        Neuron outN = t.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        t.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );

        Option o = Option.addPrimitive(doc);

        inS.addInput(t, 3, 8, o);

        System.out.println(t.networkStateToString(true, true));

        inA.addInput(t, 0, 11);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
        Assert.assertFalse(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.removeInput(t, 0, 11);

        System.out.println(t.networkStateToString(true, true));

        inS.removeInput(t, 3, 8, o);

        t.clearActivations();
    }


    @Test
    public void testSimpleNegation3() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");

        Neuron asN = new Neuron("AS");

        InputNeuron inS = t.createOrLookupInputSignal("S");

        Neuron outN = t.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        t.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );


        Option o = Option.addPrimitive(doc);

        inA.addInput(t, 0, 11);

        System.out.println(t.networkStateToString(true, true));

        inS.addInput(t, 3, 8, o);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertNotNull(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null));
        Assert.assertFalse(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inS.removeInput(t, 3, 8, o);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertTrue(Activation.get(t, outN.node, null, new Range(0, 11), Range.Relation.EQUALS, null, null).key.o.largestCommonSubset.conflicts.primary.isEmpty());

        inA.removeInput(t, 0, 11);

        System.out.println(t.networkStateToString(true, true));

        t.clearActivations();
    }


    @Test
    public void testNegation1() {
        Model m = new Model();
        Iteration t = m.startIteration(null, 0);
        InputNeuron inA = t.createOrLookupInputSignal("A");
        InputNeuron inB = t.createOrLookupInputSignal("B");

        Neuron asN = new Neuron("AS");
        Neuron absN = new Neuron("ABS");
        Neuron bsN = new Neuron("BS");

        Neuron inS = t.createOrNeuron(new Neuron("S"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(absN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );

        t.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );
        t.createAndNeuron(absN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );

        {
            Document doc = Document.create("aaaaaaaaaa");
            t = m.startIteration(doc, 0);

            inA.addInput(t, 0, 6);
            System.out.println(t.networkStateToString(true, true));

            inB.addInput(t, 0, 6);

            System.out.println(t.networkStateToString(true, true));

            Assert.assertNotNull(Activation.get(t, inS.node, null, new Range(0, 6), Range.Relation.EQUALS, null, null));
            Assert.assertEquals(2, Activation.get(t, inS.node, null, new Range(0, 6), Range.Relation.EQUALS, null, null).key.o.orOptions.size());

            t.clearActivations();
        }

        {
            Document doc = Document.create("aaaaaaaaaa");
            t = m.startIteration(doc, 0);

            inA.addInput(t, 0, 6);
            System.out.println(t.networkStateToString(true, true));

            inB.addInput(t, 3, 9);

            System.out.println(t.networkStateToString(true, true));

//            Assert.assertNotNull(Activation.get(t, inS.node, 0, new Range(0, 6), Range.Relation.EQUALS, null, null, null));
            Assert.assertNotNull(Activation.get(t, inS.node, null, new Range(0, 9), Range.Relation.EQUALS, null, null));
//            Assert.assertEquals(1, Activation.get(t, inS.node, 0, new Range(0, 6), Range.Relation.EQUALS, null, null, null).key.o.orOptions.size());
            Assert.assertEquals(2, Activation.get(t, inS.node, null, new Range(0, 9), Range.Relation.EQUALS, null, null).key.o.orOptions.size());

            t.clearActivations();
        }
    }


    @Test
    public void testNegation2() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);


        InputNeuron inA = t.createOrLookupInputSignal("A");
        InputNeuron inB = t.createOrLookupInputSignal("B");
        InputNeuron inC = t.createOrLookupInputSignal("C");

        Neuron asN = new Neuron("AS");
        Neuron ascN = new Neuron("ASC");
        Neuron bsN = new Neuron("BS");

        Neuron inS = t.createOrNeuron(new Neuron("S"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(ascN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(bsN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        t.createAndNeuron(asN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );
        t.createAndNeuron(ascN,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setOptional(false)
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        t.createAndNeuron(bsN,
                0.001,
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inS)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

        Neuron outA = t.createOrNeuron(new Neuron("OUT A"),
                new Input()
                        .setNeuron(asN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );
        Neuron outAC = t.createOrNeuron(new Neuron("OUT AC"),
                new Input()
                        .setNeuron(ascN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );
        Neuron outB = t.createOrNeuron(new Neuron("OUT B"),
                new Input()
                        .setNeuron(bsN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
        );

//        asN.node.weight = 0.45;
//        ascN.node.weight = 1.0;

//        bsN.node.weight = 0.5;


        inA.addInput(t, 0, 6);

        System.out.println(t.networkStateToString(true, true));

        inB.addInput(t, 0, 6);

        System.out.println(t.networkStateToString(true, true));

        inC.addInput(t, 0, 6);

        System.out.println(t.networkStateToString(true, true));

        t.process();

        System.out.println(t.networkStateToString(false, true));
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
        Document doc = Document.create("aaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");
        InputNode inANode = (InputNode) inA.node;

        InputNeuron inB = t.createOrLookupInputSignal("B");
        InputNode inBNode = (InputNode) inB.node;


        Neuron pC = new Neuron("C");
        Neuron pD = new Neuron("D");

        t.createAndNeuron(pC,
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

        t.createAndNeuron(pD,
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


        InputNeuron inG = t.createOrLookupInputSignal("G");
        InputNode inGNode = (InputNode) inG.node;

        Neuron pH = t.createAndNeuron(new Neuron("H"),
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


        inA.addInput(t, 0, 1);
        inB.addInput(t, 0, 1);
        inG.addInput(t, 0, 1);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertNotNull(pC.node.getFirstActivation(t));
        Assert.assertNotNull(pD.node.getFirstActivation(t));

        // Die Optionen 0 und 2 stehen in Konflikt. Da sie aber jetzt in Oder Optionen eingebettet sind, werden sie nicht mehr ausgefiltert.
//        Assert.assertNull(pH.node.getFirstActivation(t));
    }


}
