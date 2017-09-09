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
import org.junit.Test;

import static org.aika.corpus.Range.Operator.EQUALS;

public class RelationalNeuronTest {

    @Test
    public void testOutputNode() {
        Model m = new Model();

        Neuron in = m.createNeuron("INPUT");
        Neuron cn = m.createNeuron("CLOCK");
        Neuron sn = m.createNeuron("START");
        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), cn, false, sn, true, false);
        Neuron on = m.initRelationalNeuron(m.createNeuron("ON"), ctn, in, false);

        Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0);


        InterprNode o0 = InterprNode.addPrimitive(doc);
        InterprNode o1 = InterprNode.addPrimitive(doc);
        InterprNode o2 = InterprNode.addPrimitive(doc);
        InterprNode o01 = InterprNode.add(doc, false, o0, o1);
        InterprNode o012 = InterprNode.add(doc, false, o01, o2);

        sn.addInput(doc, 0, 1, 0, doc.bottom);
        cn.addInput(doc, 4, 5, o0);
        cn.addInput(doc, 19, 20, o0);
        in.addInput(doc, 10, 11, o0);

        System.out.println(doc.neuronActivationsToString(false, false, true));


        Assert.assertNotNull(getAct(doc, on.get().node.get(), 1, new Range(5, 20), null));

        cn.addInput(doc, 9, 10, o012);

        System.out.println(doc.neuronActivationsToString(false, false, true));
/*
        Assert.assertNotNull(getAct(doc, on.node, 0, new Range(0, 5), null));
        Assert.assertNotNull(getAct(doc, on.node, 1, new Range(5, 10), null));
        Assert.assertNotNull(getAct(doc, on.node, 1, new Range(5, 20), null));
        Assert.assertNotNull(getAct(doc, on.node, 2, new Range(10, 20), null));

        sn.addInput(doc, 15, 16, o01);

        System.out.println(doc.neuronActivationsToString(false));

        Assert.assertNotNull(getAct(doc, ctn.node, 0, new Range(15, 20), null));

        sn.removeInput(doc, 15, 16, o01);

        System.out.println(doc.neuronActivationsToString(false));

        Assert.assertNull(getAct(doc, ctn.node, 0, new Range(15, 20), null));
*/
    }

    
    private <T extends Node, A extends NodeActivation<T>> NodeActivation getAct(Document doc, T n, Integer rid, Range r, final InterprNode o) {
        return NodeActivation.select(doc, n, rid, r, EQUALS, EQUALS, null, null).filter(act -> o == null || act.key.o == o).findFirst().orElse(null);
    }
}
