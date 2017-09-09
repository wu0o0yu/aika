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


import org.aika.Neuron;
import org.aika.Provider;
import org.aika.lattice.NodeActivation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.lattice.Node;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.stream.Stream;

import static org.aika.corpus.Range.Operator.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class CounterNeuronTest {


    @Test
    public void testFork() {

        System.out.println("Start =====================");
        Model m = new Model();

        Neuron cn = m.createNeuron("CLOCK");
        Neuron sn = m.createNeuron("START");


        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), cn, false, sn, true, false);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);

        InterprNode o0 = InterprNode.addPrimitive(doc);
        InterprNode o1 = InterprNode.addPrimitive(doc);
        InterprNode o2 = InterprNode.addPrimitive(doc);
        InterprNode o01 = InterprNode.add(doc, false, o0, o1);
        InterprNode o012 = InterprNode.add(doc, false, o01, o2);

        sn.get().addInput(doc, 0, 1, 0, doc.bottom);
        cn.get().addInput(doc, 9, 10, o012);
        cn.get().addInput(doc, 24, 25, o01);
//        sn.addInput(t, 0, 1, doc.bottom);

        System.out.println(doc.neuronActivationsToString(false, false, true));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 0, new Range(0, 10), null));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 0, new Range(0, 25), null));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 1, new Range(10, 25), null));

        cn.get().addInput(doc, 4, 5, o0);
        System.out.println(doc.neuronActivationsToString(false, false, true));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 0, new Range(0, 5), null));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 1, new Range(5, 10), null));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 1, new Range(5, 25), null));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 2, new Range(10, 25), null));

        cn.get().removeInput(doc, 4, 5, o0);
        System.out.println(doc.neuronActivationsToString(false, false, true));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 0, new Range(0, 10), null));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 0, new Range(0, 25), null));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 1, new Range(10, 25), null));

        cn.get().removeInput(doc, 24, 25, o01);
        System.out.println(doc.neuronActivationsToString(false, false, true));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 0, new Range(0, 10), null));

        System.out.println();
    }


    @Test
    public void testE() {
        Model m = new Model();

        Neuron cn = m.createNeuron("CLOCK");
        Neuron sn = m.createNeuron("START");
        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), cn, false, sn, true, false);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);

        doc.propagate();

        InterprNode o0 = InterprNode.addPrimitive(doc);
        InterprNode o1 = InterprNode.addPrimitive(doc);
        InterprNode o2 = InterprNode.addPrimitive(doc);
        InterprNode o01 = InterprNode.add(doc, false, o0, o1);
        InterprNode o012 = InterprNode.add(doc, false, o01, o2);


        sn.get().addInput(doc, 0, 1, 0, doc.bottom);

        cn.get().addInput(doc, 5, 6, o0);
        cn.get().addInput(doc, 20, 21, o0);
        cn.get().addInput(doc, 10, 11, o012);
        cn.get().addInput(doc, 15, 16, o01);

        System.out.println(doc.neuronActivationsToString(false, false, true));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 0, new Range(0, 6), null));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 1, new Range(6, 21), null));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 1, new Range(6, 11), null));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 2, new Range(11, 16), null));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 2, new Range(16, 21), null));

        System.out.println();
    }


    @Ignore
    @Test
    public void testReverseDirection() {
        Model m = new Model();

        Neuron cn = m.createNeuron("CLOCK");
        Neuron sn = m.createNeuron("START");
        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), cn, false, sn, false, true);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);

        InterprNode o0 = InterprNode.addPrimitive(doc);
        InterprNode o1 = InterprNode.addPrimitive(doc);
        InterprNode o2 = InterprNode.addPrimitive(doc);
        InterprNode o01 = InterprNode.add(doc, false, o0, o1);
        InterprNode o012 = InterprNode.add(doc, false, o01, o2);

        sn.get().addInput(doc, doc.length() - 1, doc.length(), 0, doc.bottom);
        cn.get().addInput(doc, 19, 20, o0);
        cn.get().addInput(doc, 4, 5, o0);
        cn.get().addInput(doc, 14, 15, o012);

        System.out.println(doc.neuronActivationsToString(false, false, true));

        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 2, new Range(5, 15), o012));
        Assert.assertNotNull(getAct(doc, ctn.get().node.get(), 3, new Range(0, 5), o012));

        System.out.println();
    }


    @Test
    public void testStartNode() {
        Model m = new Model();

        Neuron sn = m.createNeuron("START");
        Neuron cn = m.createNeuron("CLOCK");
        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), cn, false, sn, true, false);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);

        sn.get().addInput(doc, 0, 5, 0, doc.bottom);
        cn.get().addInput(doc, 4, 5, doc.bottom);
        cn.get().addInput(doc, 9, 10, doc.bottom);
        cn.get().addInput(doc, 14, 15, doc.bottom);
        cn.get().addInput(doc, 19, 20, doc.bottom);
        cn.get().addInput(doc, 24, 25, doc.bottom);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Assert.assertEquals(5, ctn.get().node.get().getActivations(doc).size());
    }


    private <T extends Node, A extends NodeActivation<T>> A getAct(Document doc, T n, Integer rid, Range r, final InterprNode o) {
        Stream<A> s = NodeActivation.select(doc, n, rid, r, EQUALS, EQUALS, null, null);
        return s.filter(act -> o == null || act.key.o == o).findFirst().orElse(null);
    }
}