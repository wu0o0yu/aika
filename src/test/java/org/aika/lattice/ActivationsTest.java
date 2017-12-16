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
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.network.TestHelper;
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

        Neuron inA = m.createNeuron("A");

        m.initNeuron(m.createNeuron("pA"), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBias(-1.0)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        InputNode pANode = TestHelper.addOutputNode(doc, inA, null, 0, Operator.LESS_THAN_EQUAL, Mapping.START, true, Operator.GREATER_THAN_EQUAL, Mapping.END, true);


        inA.addInput(doc, 0, 1, 0);
        inA.addInput(doc, 2, 3, 0);

        Assert.assertNotNull(TestHelper.get(doc, pANode, new Range(0, 1), null));
        Assert.assertNull(TestHelper.get(doc, pANode, new Range(1, 2), null));
        Assert.assertNotNull(TestHelper.get(doc, pANode, new Range(2, 3), null));

        inA.addInput(doc, 1, 2);

        Assert.assertEquals(NodeActivation.get(doc, pANode, null, new Range(0, 1), EQUALS, EQUALS, doc.bottom, InterprNode.Relation.EQUALS), TestHelper.get(doc, pANode, new Range(0, 1), doc.bottom));
        Assert.assertEquals(NodeActivation.get(doc, pANode, null, new Range(1, 2), EQUALS, EQUALS, doc.bottom, InterprNode.Relation.EQUALS), TestHelper.get(doc, pANode, new Range(1, 2), doc.bottom));
        Assert.assertEquals(NodeActivation.get(doc, pANode, null, new Range(2, 3), EQUALS, EQUALS, doc.bottom, InterprNode.Relation.EQUALS), TestHelper.get(doc, pANode, new Range(2, 3), doc.bottom));
    }


    @Test
    public void testGetActivationReturnsFirstFired() {
        Model m = new Model();

        Neuron in = m.createNeuron("A");
        OrNode inNode = in.get().node.get();

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inNode.processAddedActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList());

        inNode.processAddedActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList());

        inNode.processAddedActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList());

        inNode.processAddedActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList());

        inNode.processAddedActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList());

 //       Assert.assertEquals(1, Activation.get(t, inNode, new Range(0, 1), doc.bottom).key.fired);
    }

}
