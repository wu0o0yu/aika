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


import org.aika.Iteration;
import org.aika.Model;
import org.aika.lattice.InputNode;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Synapse;
import org.junit.Assert;
import org.aika.corpus.Document;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.Activation;
import org.aika.neuron.Neuron;
import org.aika.Iteration.Input;
import org.aika.lattice.AndNode;
import org.junit.Test;

import java.util.Collections;

/**
 *
 * @author Lukas Molzberger
 */
public class ActivationsTest {


    @Test
    public void addActivationsTest() {
        Model m = new Model();
        AndNode.minFrequency = 1;
        Document doc = Document.create("aaaaaaaaaa");

        Iteration t = m.startIteration(doc, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");

        t.createAndNeuron(new Neuron("pA"), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );
        InputNode pANode = TestHelper.addOutputNode(t, inA, null, 0, true, Synapse.RangeSignal.START, Synapse.RangeVisibility.MATCH_INPUT, Synapse.RangeSignal.END, Synapse.RangeVisibility.MATCH_INPUT);

        inA.addInput(t, 0, 1, 0);
        inA.addInput(t, 2, 3, 0);

        Assert.assertNotNull(TestHelper.get(t, pANode, new Range(0, 1), null));
        Assert.assertNull(TestHelper.get(t, pANode, new Range(1, 2), null));
        Assert.assertNotNull(TestHelper.get(t, pANode, new Range(2, 3), null));

        inA.addInput(t, 1, 2);

        Assert.assertEquals(Activation.get(t, pANode, null, new Range(0, 1), Range.Relation.EQUALS, doc.bottom, Option.Relation.EQUALS), TestHelper.get(t, pANode, new Range(0, 1), doc.bottom));
        Assert.assertEquals(Activation.get(t, pANode, null, new Range(1, 2), Range.Relation.EQUALS, doc.bottom, Option.Relation.EQUALS), TestHelper.get(t, pANode, new Range(1, 2), doc.bottom));
        Assert.assertEquals(Activation.get(t, pANode, null, new Range(2, 3), Range.Relation.EQUALS, doc.bottom, Option.Relation.EQUALS), TestHelper.get(t, pANode, new Range(2, 3), doc.bottom));

        InputNeuron inB = t.createOrLookupInputSignal("B");

        t.createAndNeuron(new Neuron("pB"), 0.001,
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );
        InputNode pBNode = TestHelper.addOutputNode(t, inB, null, 0, true, Synapse.RangeSignal.START, Synapse.RangeVisibility.MATCH_INPUT, Synapse.RangeSignal.END, Synapse.RangeVisibility.MATCH_INPUT);

        inB.addInput(t, 0, 1);
        inB.addInput(t, 1, 2);

        inB.removeInput(t, 1, 2);

        Assert.assertNull(TestHelper.get(t, pBNode, new Range(1, 2), null));
    }


    @Test
    public void testGetActivationReturnsFirstFired() {
        Model m = new Model();
        AndNode.minFrequency = 1;
        Document doc = Document.create("aaaaaaaaaa");

        Iteration t = m.startIteration(doc, 0);

        Neuron in = t.createOrLookupInputSignal("A");
        InputNode inNode = (InputNode) in.node;

        inNode.addActivationInternal(t, new Activation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

        inNode.addActivationInternal(t, new Activation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

        inNode.addActivationInternal(t, new Activation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

        inNode.addActivationInternal(t, new Activation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

        inNode.addActivationInternal(t, new Activation.Key(inNode, new Range(0, 1), 0, doc.bottom), Collections.emptyList(), false);

 //       Assert.assertEquals(1, Activation.get(t, inNode, new Range(0, 1), doc.bottom).key.fired);
    }

}
