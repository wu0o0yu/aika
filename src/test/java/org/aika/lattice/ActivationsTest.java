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
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.network.TestHelper;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import static org.aika.corpus.Range.Operator.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class ActivationsTest {


    @Test
    public void addActivationsTest() {
        Model m = new Model();
        AndNode.minFrequency = 1;

        Neuron inA = new Neuron(m, "A");

        m.initAndNeuron(new Neuron(m, "pA"), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        InputNode pANode = TestHelper.addOutputNode(doc, inA, null, 0, Operator.LESS_THAN, Mapping.START, true, Operator.GREATER_THAN, Mapping.END, true);


        inA.addInput(doc, 0, 1, 0);
        inA.addInput(doc, 2, 3, 0);

        Assert.assertNotNull(TestHelper.get(doc, pANode, new Range(0, 1), null));
        Assert.assertNull(TestHelper.get(doc, pANode, new Range(1, 2), null));
        Assert.assertNotNull(TestHelper.get(doc, pANode, new Range(2, 3), null));

        inA.addInput(doc, 1, 2);

        Assert.assertEquals(NodeActivation.get(doc, pANode, null, new Range(0, 1), EQUALS, EQUALS, doc.bottom, InterprNode.Relation.EQUALS), TestHelper.get(doc, pANode, new Range(0, 1), doc.bottom));
        Assert.assertEquals(NodeActivation.get(doc, pANode, null, new Range(1, 2), EQUALS, EQUALS, doc.bottom, InterprNode.Relation.EQUALS), TestHelper.get(doc, pANode, new Range(1, 2), doc.bottom));
        Assert.assertEquals(NodeActivation.get(doc, pANode, null, new Range(2, 3), EQUALS, EQUALS, doc.bottom, InterprNode.Relation.EQUALS), TestHelper.get(doc, pANode, new Range(2, 3), doc.bottom));

        Neuron inB = new Neuron(m, "B");

        m.initAndNeuron(new Neuron(m, "pB"), 0.001,
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );
        InputNode pBNode = TestHelper.addOutputNode(doc, inB, null, 0, Operator.LESS_THAN, Mapping.START, true, Operator.GREATER_THAN, Mapping.END, true);

        inB.addInput(doc, 0, 1);
        inB.addInput(doc, 1, 2);

        inB.removeInput(doc, 1, 2);

        Assert.assertNull(TestHelper.get(doc, pBNode, new Range(1, 2), null));
    }


    @Test
    public void testGetActivationReturnsFirstFired() {
        Model m = new Model();
        AndNode.minFrequency = 1;


        Neuron in = new Neuron(m, "A");
        Node inNode = in.node.get();

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inNode.addActivationInternal(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

        inNode.addActivationInternal(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

        inNode.addActivationInternal(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

        inNode.addActivationInternal(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

        inNode.addActivationInternal(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

 //       Assert.assertEquals(1, Activation.get(t, inNode, new Range(0, 1), doc.bottom).key.fired);
    }

}
