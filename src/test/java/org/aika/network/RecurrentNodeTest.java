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

import static org.aika.neuron.Synapse.RangeMatch.EQUALS;

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

        InputNeuron cn = m.createOrLookupInputSignal("CLOCK");
        InputNeuron sn = m.createOrLookupInputSignal("START");

        Neuron ctn = m.createCounterNeuron(new Neuron("CTN"), cn, false, sn, true, false);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);

        sn.addInput(doc, 0, 1, doc.bottom);
        cn.addInput(doc, 9, 10, o012);
        cn.addInput(doc, 24, 25, o01);
//        sn.addInput(t, 0, 1, doc.bottom);

        System.out.println(doc.networkStateToString(true, false));

        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 10), null));
        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 25), null));
        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(10, 25), null));

        cn.addInput(doc, 4, 5, o0);
        System.out.println(doc.networkStateToString(true, false));

        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 5), null));
        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(5, 10), null));
        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(5, 25), null));
        Assert.assertNotNull(getAct(doc, ctn.node, 2, new Range(10, 25), null));

        cn.removeInput(doc, 4, 5, o0);
        System.out.println(doc.networkStateToString(true, false));

        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 10), null));
        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 25), null));
        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(10, 25), null));

        cn.removeInput(doc, 24, 25, o01);
        System.out.println(doc.networkStateToString(true, false));

        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 10), null));

        System.out.println();
    }


    private Activation getAct(Document doc, Node n, Integer rid, Range r, final Option o) {
        return Activation.select(doc, n, rid, r, EQUALS, EQUALS, null, null).filter(act -> o == null || act.key.o == o).findFirst().orElse(null);
    }


    @Test
    public void testE() {
        Model m = new Model();

        InputNeuron cn = m.createOrLookupInputSignal("CLOCK");
        InputNeuron sn = m.createOrLookupInputSignal("START");
        Neuron ctn = m.createCounterNeuron(new Neuron("CTN"), cn, false, sn, true, false);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);

        doc.propagate();

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);


        sn.addInput(doc, 0, 1, doc.bottom);

        cn.addInput(doc, 5, 6, o0);
        cn.addInput(doc, 20, 21, o0);
        cn.addInput(doc, 10, 11, o012);
        cn.addInput(doc, 15, 16, o01);

        System.out.println(doc.networkStateToString(true, false));

        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 6), null));
        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(6, 21), null));

        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(6, 11), null));

        Assert.assertNotNull(getAct(doc, ctn.node, 2, new Range(11, 16), null));

        Assert.assertNotNull(getAct(doc, ctn.node, 2, new Range(16, 21), null));

        System.out.println();
    }


    @Test
    public void testReverseDirection() {
        Model m = new Model();

        InputNeuron cn = m.createOrLookupInputSignal("CLOCK");
        InputNeuron sn = m.createOrLookupInputSignal("START");
        Neuron ctn = m.createCounterNeuron(new Neuron("CTN"), cn, false, sn, false, true);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);

        sn.addInput(doc, doc.length() - 1, doc.length(), doc.bottom);
        cn.addInput(doc, 19, 20, o0);
        cn.addInput(doc, 4, 5, o0);
        cn.addInput(doc, 14, 15, o012);


        Assert.assertNotNull(getAct(doc, ctn.node, 2, new Range(5, 15), o012));
        Assert.assertNotNull(getAct(doc, ctn.node, 3, new Range(0, 5), o012));

        System.out.println();
    }


    @Test
    public void testOutputNode() {
        Model m = new Model();

        InputNeuron in = m.createOrLookupInputSignal("INPUT");
        InputNeuron cn = m.createOrLookupInputSignal("CLOCK");
        InputNeuron sn = m.createOrLookupInputSignal("START");
        Neuron ctn = m.createCounterNeuron(new Neuron("CTN"), cn, false, sn, true, false);
        Neuron on = m.createRelationalNeuron(new Neuron("ON"), ctn, in, false);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);


        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);

        sn.addInput(doc, 0, 1, doc.bottom);
        cn.addInput(doc, 4, 5, o0);
        cn.addInput(doc, 19, 20, o0);
        in.addInput(doc, 0, 5, o0);

        System.out.println(doc.networkStateToString(true, false));


        Assert.assertNotNull(getAct(doc, on.node, 0, new Range(0, 5), null));
        Assert.assertNotNull(getAct(doc, on.node, 1, new Range(5, 20), null));

        cn.addInput(doc, 9, 10, o012);

        System.out.println(doc.networkStateToString(true, false));

        Assert.assertNotNull(getAct(doc, on.node, 0, new Range(0, 5), null));
        Assert.assertNotNull(getAct(doc, on.node, 1, new Range(5, 10), null));
        Assert.assertNotNull(getAct(doc, on.node, 1, new Range(5, 20), null));
        Assert.assertNotNull(getAct(doc, on.node, 2, new Range(10, 20), null));

        sn.addInput(doc, 15, 16, o01);

        System.out.println(doc.networkStateToString(true, false));

        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(15, 20), null));

        sn.removeInput(doc, 15, 16, o01);

        System.out.println(doc.networkStateToString(true, false));

        Assert.assertNull(getAct(doc, ctn.node, 0, new Range(15, 20), null));
    }


    @Test
    public void testOutputNodeRD() {
        Model m = new Model();

        InputNeuron in = m.createOrLookupInputSignal("INPUT"); //new InputNode(m, false);
        InputNeuron cn = m.createOrLookupInputSignal("CLOCK"); //new ControlNode(m, Type.CLOCK, false);
        Neuron ctn = null; //new CycleNode(m, true, Integer.MAX_VALUE);
//        m.clockTerminationNodes.add(ctn);
        InputNeuron sn = m.createOrLookupInputSignal("START"); //new ControlNode(m, Type.START, false);
        InputNeuron tn = m.createOrLookupInputSignal("TERMINATION"); //new ControlNode(m, Type.TERMINATION, false);
        Neuron on = null; //new OutputNode(m, false);


        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);
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

        doc.propagate();

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);

        sn.addInput(doc, doc.length() - 1, doc.length(), doc.bottom);

        cn.addInput(doc, 19, 20, o0);
        cn.addInput(doc, 4, 5, o0);
        in.addInput(doc, 20, 50, o0);

        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(0, 50), EQUALS, EQUALS, o0, Option.Relation.CONTAINS));

        cn.addInput(doc, 14, 15, o012);


        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(15, 50), EQUALS, EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(0, 15), EQUALS, EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(0, 15), EQUALS, EQUALS, o012, Option.Relation.CONTAINS));

        sn.addInput(doc, 9, 10, o01);

        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(15, 50), EQUALS, EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(10, 15), EQUALS, EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(10, 15), EQUALS, EQUALS, o012, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(0, 10), EQUALS, EQUALS, o0, Option.Relation.CONTAINS));

        sn.removeInput(doc, 9, 10, o01);

        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(15, 50), EQUALS, EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(0, 15), EQUALS, EQUALS, o0, Option.Relation.CONTAINS));
        Assert.assertNotNull(Activation.get(doc, on.node, 0, new Range(0, 15), EQUALS, EQUALS, o012, Option.Relation.CONTAINS));
    }


    @Test
    public void testOverlappingClockAndTermSignals() {
        Model m = new Model();

        InputNeuron in = m.createOrLookupInputSignal("INPUT"); //new InputNode(m, false);
        InputNeuron cn = m.createOrLookupInputSignal("CLOCK"); //ControlNode(m, ControlNode.Type.CLOCK, false);
        Neuron ctn = null; //new CycleNode(m, false, Integer.MAX_VALUE);
//        m.clockTerminationNodes.add(ctn);
        InputNeuron sn = m.createOrLookupInputSignal("START"); //ControlNode(m, Type.START, true);
        InputNeuron tn = m.createOrLookupInputSignal("TERMINATION"); //new ControlNode(m, Type.TERMINATION, false);
        Neuron on = null; //new OutputNode(m, false);


        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);
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


        doc.propagate();

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o01 = Option.add(doc, false, o0, o1);

        sn.addInput(doc, 0, 1, doc.bottom);
        tn.addInput(doc, 9, 10, o01);
        cn.addInput(doc, 9, 10, o0);


        // Es reicht wenn o5 mit o0 in Konflikt steht, da es damit automatisch auch mit o01 in Konflikt ist.

        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 10), doc.bottom));
        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(10, 50), doc.bottom));
        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(10, 50), o01));
        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(10, 50), o0));

        cn.addInput(doc, 4, 5, doc.bottom);


        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(0, 5), doc.bottom));
        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(5, 10), doc.bottom));
        Assert.assertNotNull(getAct(doc, ctn.node, 1, new Range(10, 50), doc.bottom));
        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(10, 50), o01));
        Assert.assertNotNull(getAct(doc, ctn.node, 2, new Range(10, 50), o0));
    }


    @Test
    public void testStartNode() {
        Model m = new Model();

        InputNeuron cn = m.createOrLookupInputSignal("CLOCK"); //new ControlNode(m, Type.CLOCK, false);
        Neuron ctn = null; //new CycleNode(m, false, Integer.MAX_VALUE);
//        m.clockTerminationNodes.add(ctn);
        InputNeuron sn = m.createOrLookupInputSignal("START"); //new ControlNode(m, Type.START, true);
        InputNeuron tn = m.createOrLookupInputSignal("TERMINATION"); //new ControlNode(m, Type.TERMINATION, false);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);

/*
        cn.ctNodes.put(sn, ctn);

        sn.ctNodes.put(cn, ctn);
        tn.ctNodes.put(cn, ctn);

        ctn.clockNode = cn;
        ctn.startNode = sn;
        ctn.terminationNode = tn;
*/


        doc.propagate();


        sn.addInput(doc, 0, 5, doc.bottom);
        cn.addInput(doc, 4, 5, doc.bottom);
        cn.addInput(doc, 9, 10, doc.bottom);
        cn.addInput(doc, 14, 15, doc.bottom);
        cn.addInput(doc, 19, 20, doc.bottom);
        cn.addInput(doc, 24, 25, doc.bottom);
        tn.addInput(doc, 24, 25, doc.bottom);
        sn.addInput(doc, 15, 20, doc.bottom);

        System.out.println();

        Assert.assertEquals(7, ctn.node.getActivations(doc).size());
    }
}