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
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.lattice.Node;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
@Ignore
public class RecurrentNodeTest {


    @Test
    public void testFork() {

        System.out.println("Start =====================");
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron cn = t.createOrLookupInputSignal("CLOCK");
        InputNeuron sn = t.createOrLookupInputSignal("START");

        Neuron ctn = t.createCounterNeuron(new Neuron("CTN"), cn, false, sn, true, false);

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);

        sn.addInput(t, 0, 1, doc.bottom);
        cn.addInput(t, 9, 10, o012);
        cn.addInput(t, 24, 25, o01);
//        sn.addInput(t, 0, 1, doc.bottom);

        System.out.println(t.networkStateToString(true, false));

        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 10), null));
        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 25), null));
        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(10, 25), null));

        cn.addInput(t, 4, 5, o0);
        System.out.println(t.networkStateToString(true, false));

        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 5), null));
        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(5, 10), null));
        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(5, 25), null));
        Assert.assertNotNull(getAct(t, ctn.node, 2, new Range(10, 25), null));

        cn.removeInput(t, 4, 5, o0);
        System.out.println(t.networkStateToString(true, false));

        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 10), null));
        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 25), null));
        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(10, 25), null));

        cn.removeInput(t, 24, 25, o01);
        System.out.println(t.networkStateToString(true, false));

        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 10), null));

        System.out.println();
    }


    private Activation getAct(Iteration t, Node n, Integer rid, Range r, final Option o) {
        return Activation.select(t, n, rid, r, Range.Relation.EQUALS, null, null).filter(act -> o == null || act.key.o == o).findFirst().orElse(null);
    }


    @Test
    public void testE() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron cn = t.createOrLookupInputSignal("CLOCK");
        InputNeuron sn = t.createOrLookupInputSignal("START");
        Neuron ctn = t.createCounterNeuron(new Neuron("CTN"), cn, false, sn, true, false);

        t.propagate();

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);


        sn.addInput(t, 0, 1, doc.bottom);

        cn.addInput(t, 5, 6, o0);
        cn.addInput(t, 20, 21, o0);
        cn.addInput(t, 10, 11, o012);
        cn.addInput(t, 15, 16, o01);

        System.out.println(t.networkStateToString(true, false));

        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 6), null));
        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(6, 21), null));

        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(6, 11), null));

        Assert.assertNotNull(getAct(t, ctn.node, 2, new Range(11, 16), null));

        Assert.assertNotNull(getAct(t, ctn.node, 2, new Range(16, 21), null));

        System.out.println();
    }


    @Test
    public void testReverseDirection() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron cn = t.createOrLookupInputSignal("CLOCK");
        InputNeuron sn = t.createOrLookupInputSignal("START");
        Neuron ctn = t.createCounterNeuron(new Neuron("CTN"), cn, false, sn, false, true);

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);

        sn.addInput(t, doc.length() - 1, doc.length(), doc.bottom);
        cn.addInput(t, 19, 20, o0);
        cn.addInput(t, 4, 5, o0);
        cn.addInput(t, 14, 15, o012);


        Assert.assertNotNull(getAct(t, ctn.node, 2, new Range(5, 15), o012));
        Assert.assertNotNull(getAct(t, ctn.node, 3, new Range(0, 5), o012));

        System.out.println();
    }


    @Test
    public void testOutputNode() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);


        InputNeuron in = t.createOrLookupInputSignal("INPUT");
        InputNeuron cn = t.createOrLookupInputSignal("CLOCK");
        InputNeuron sn = t.createOrLookupInputSignal("START");
        Neuron ctn = t.createCounterNeuron(new Neuron("CTN"), cn, false, sn, true, false);
        Neuron on = t.createRelationalNeuron(new Neuron("ON"), ctn, in, false);

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);

        sn.addInput(t, 0, 1, doc.bottom);
        cn.addInput(t, 4, 5, o0);
        cn.addInput(t, 19, 20, o0);
        in.addInput(t, 0, 5, o0);

        System.out.println(t.networkStateToString(true, false));


        Assert.assertNotNull(getAct(t, on.node, 0, new Range(0, 5), null));
        Assert.assertNotNull(getAct(t, on.node, 1, new Range(5, 20), null));

        cn.addInput(t, 9, 10, o012);

        System.out.println(t.networkStateToString(true, false));

        Assert.assertNotNull(getAct(t, on.node, 0, new Range(0, 5), null));
        Assert.assertNotNull(getAct(t, on.node, 1, new Range(5, 10), null));
        Assert.assertNotNull(getAct(t, on.node, 1, new Range(5, 20), null));
        Assert.assertNotNull(getAct(t, on.node, 2, new Range(10, 20), null));

        sn.addInput(t, 15, 16, o01);

        System.out.println(t.networkStateToString(true, false));

        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(15, 20), null));

        sn.removeInput(t, 15, 16, o01);

        System.out.println(t.networkStateToString(true, false));

        Assert.assertNull(getAct(t, ctn.node, 0, new Range(15, 20), null));
    }


    @Test
    public void testOutputNodeRD() {
        Model m = new Model();
        Document doc = Document.create("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron in = t.createOrLookupInputSignal("INPUT"); //new InputNode(m, false);
        InputNeuron cn = t.createOrLookupInputSignal("CLOCK"); //new ControlNode(m, Type.CLOCK, false);
        Neuron ctn = null; //new CycleNode(m, true, Integer.MAX_VALUE);
//        m.clockTerminationNodes.add(ctn);
        InputNeuron sn = t.createOrLookupInputSignal("START"); //new ControlNode(m, Type.START, false);
        InputNeuron tn = t.createOrLookupInputSignal("TERMINATION"); //new ControlNode(m, Type.TERMINATION, false);
        Neuron on = null; //new OutputNode(m, false);
/*
        on.direction = true;

        in.outputNodes.put(ctn, on);

        cn.ctNodes.put(sn, ctn);

        sn.ctNodes.put(cn, ctn);
        tn.ctNodes.put(cn, ctn);

        ctn.clockNode = cn;
        ctn.startNode = sn;
        ctn.terminationNode = tn;
        ctn.outputNodes.put(in, on);

        on.ctNode = ctn;
        on.inputNode = in;
*/

        t.propagate();

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);

        sn.addInput(t, doc.length() - 1, doc.length(), doc.bottom);

        cn.addInput(t, 19, 20, o0);
        cn.addInput(t, 4, 5, o0);
        in.addInput(t, 20, 50, o0);

        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(0, 50), Range.Relation.EQUALS, o0, Option.Relation.CONTAINS));

        cn.addInput(t, 14, 15, o012);


        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(15, 50), Range.Relation.EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(0, 15), Range.Relation.EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(0, 15), Range.Relation.EQUALS, o012, Option.Relation.CONTAINS));

        sn.addInput(t, 9, 10, o01);

        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(15, 50), Range.Relation.EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(10, 15), Range.Relation.EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(10, 15), Range.Relation.EQUALS, o012, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(0, 10), Range.Relation.EQUALS, o0, Option.Relation.CONTAINS));

        sn.removeInput(t, 9, 10, o01);

        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(15, 50), Range.Relation.EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(0, 15), Range.Relation.EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(t, on.node, 0, new Range(0, 15), Range.Relation.EQUALS, o012, Option.Relation.CONTAINS));
    }


    @Test
    public void testOverlappingClockAndTermSignals() {
        Model m = new Model();

        Document doc = Document.create("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron in = t.createOrLookupInputSignal("INPUT"); //new InputNode(m, false);
        InputNeuron cn = t.createOrLookupInputSignal("CLOCK"); //ControlNode(m, ControlNode.Type.CLOCK, false);
        Neuron ctn = null; //new CycleNode(m, false, Integer.MAX_VALUE);
//        m.clockTerminationNodes.add(ctn);
        InputNeuron sn = t.createOrLookupInputSignal("START"); //ControlNode(m, Type.START, true);
        InputNeuron tn = t.createOrLookupInputSignal("TERMINATION"); //new ControlNode(m, Type.TERMINATION, false);
        Neuron on = null; //new OutputNode(m, false);
/*
        in.outputNodes.put(ctn, on);

        cn.ctNodes.put(sn, ctn);

        sn.ctNodes.put(cn, ctn);
        tn.ctNodes.put(cn, ctn);

        ctn.clockNode = cn;
        ctn.startNode = sn;
        ctn.terminationNode = tn;
        ctn.outputNodes.put(in, on);

        on.ctNode = ctn;
        on.inputNode = in;
*/


        t.propagate();

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);

        sn.addInput(t, 0, 1, doc.bottom);
        tn.addInput(t, 9, 10, o01);
        cn.addInput(t, 9, 10, o0);


        // Es reicht wenn o5 mit o0 in Konflikt steht, da es damit automatisch auch mit o01 in Konflikt ist.

        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 10), doc.bottom));
        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(10, 50), doc.bottom));
        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(10, 50), o01));
        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(10, 50), o0));

        cn.addInput(t, 4, 5, doc.bottom);


        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(0, 5), doc.bottom));
        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(5, 10), doc.bottom));
        Assert.assertNotNull(getAct(t, ctn.node, 1, new Range(10, 50), doc.bottom));
        Assert.assertNotNull(getAct(t, ctn.node, 0, new Range(10, 50), o01));
        Assert.assertNotNull(getAct(t, ctn.node, 2, new Range(10, 50), o0));
    }


    @Test
    public void testStartNode() {
        Model m = new Model();

        Document doc = Document.create("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron cn = t.createOrLookupInputSignal("CLOCK"); //new ControlNode(m, Type.CLOCK, false);
        Neuron ctn = null; //new CycleNode(m, false, Integer.MAX_VALUE);
//        m.clockTerminationNodes.add(ctn);
        InputNeuron sn = t.createOrLookupInputSignal("START"); //new ControlNode(m, Type.START, true);
        InputNeuron tn = t.createOrLookupInputSignal("TERMINATION"); //new ControlNode(m, Type.TERMINATION, false);

/*
        cn.ctNodes.put(sn, ctn);

        sn.ctNodes.put(cn, ctn);
        tn.ctNodes.put(cn, ctn);

        ctn.clockNode = cn;
        ctn.startNode = sn;
        ctn.terminationNode = tn;
*/


        t.propagate();


        sn.addInput(t, 0, 5, doc.bottom);
        cn.addInput(t, 4, 5, doc.bottom);
        cn.addInput(t, 9, 10, doc.bottom);
        cn.addInput(t, 14, 15, doc.bottom);
        cn.addInput(t, 19, 20, doc.bottom);
        cn.addInput(t, 24, 25, doc.bottom);
        tn.addInput(t, 24, 25, doc.bottom);
        sn.addInput(t, 15, 20, doc.bottom);

        System.out.println();

        Assert.assertEquals(7, ctn.node.getActivations(t).size());
    }
}