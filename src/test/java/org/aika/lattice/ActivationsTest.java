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


import org.aika.neuron.*;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Relation;
import org.aika.network.TestHelper;
import org.junit.Assert;
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

        Neuron inA = m.createNeuron("A");

        Neuron pA = Neuron.init(m.createNeuron("pA"), 0.5, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1, 0);
        inA.addInput(doc, 2, 3, 0);

        Assert.assertNotNull(TestHelper.get(doc, pA.get(), new Range(0, 1)));
        Assert.assertNull(TestHelper.get(doc, pA.get(), new Range(1, 2)));
        Assert.assertNotNull(TestHelper.get(doc, pA.get(), new Range(2, 3)));

        inA.addInput(doc, 1, 2);

        Assert.assertEquals(Selector.get(doc, pA.get(), null, new Range(0, 1), Relation.EQUALS), TestHelper.get(doc, pA.get(), new Range(0, 1)));
        Assert.assertEquals(Selector.get(doc, pA.get(), null, new Range(1, 2), Relation.EQUALS), TestHelper.get(doc, pA.get(), new Range(1, 2)));
        Assert.assertEquals(Selector.get(doc, pA.get(), null, new Range(2, 3), Relation.EQUALS), TestHelper.get(doc, pA.get(), new Range(2, 3)));
    }


    @Test
    public void testGetActivationReturnsFirstFired() {
        Model m = new Model();

        Neuron in = m.createNeuron("A");
        OrNode inNode = in.get().node.get();

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inNode.processActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0), Collections.emptyList());

        inNode.processActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0), Collections.emptyList());

        inNode.processActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0), Collections.emptyList());

        inNode.processActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0), Collections.emptyList());

        inNode.processActivation(doc, new NodeActivation.Key(inNode, new Range(0, 1), 0), Collections.emptyList());

 //       Assert.assertEquals(1, Activation.get(t, inNode, new Range(0, 1), doc.bottom).key.fired);
    }

}
