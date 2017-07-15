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
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class ConcurrencyTest {

    @Test
    public void testMultipleDocuments() {
        Model m = new Model(8);
        AndNode.minFrequency = 1;

        Iteration t = m.startIteration(null, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");
        InputNeuron inB = t.createOrLookupInputSignal("B");

        Neuron pC = t.createAndNeuron(new Neuron("pC"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );


        InputNeuron inStart = t.createOrLookupInputSignal("START");
        InputNeuron inClock = t.createOrLookupInputSignal("CLOCK");


        Neuron ctn = t.createCounterNeuron(new Neuron("CTN"), inClock, false, inStart, true, false);


        Document doc0 = Document.create("aaaaaaaaaa");
        Iteration t0 = m.startIteration(doc0, 0);

        Document doc1 = Document.create("bbbbbbbbbb");
        Iteration t1 = m.startIteration(doc1, 1);

        Document doc2 = Document.create("cccccccccc");
        Iteration t2 = m.startIteration(doc2, 2);

        Document doc3 = Document.create("dddddddddd");
        Iteration t3 = m.startIteration(doc3, 3);

        Document doc4 = Document.create("eeeeeeeeee");
        Iteration t4 = m.startIteration(doc4, 4);


        inA.addInput(t2, 0, 4);
        inB.addInput(t2, 2, 6);

        Assert.assertEquals(1, pC.node.getActivations(t2).size());

        inA.addInput(t4, 0, 6);

        Assert.assertEquals(0, pC.node.getActivations(t4).size());

        inB.addInput(t0, 4, 8);
        inB.addInput(t4, 2, 8);

        Assert.assertEquals(1, pC.node.getActivations(t4).size());

        inA.addInput(t0, 0, 6);

        inStart.addInput(t3, 0, 1);
        inClock.addInput(t3, 4, 5);
        inClock.addInput(t3, 6, 7);

        Assert.assertEquals(2, ctn.node.getThreadState(t3).activations.size());

        inStart.addInput(t1, 0, 1);
        inClock.addInput(t1, 3, 4);
        inClock.addInput(t1, 7, 8);

        Assert.assertEquals(2, ctn.node.getThreadState(t1).activations.size());
        Assert.assertEquals(2, ctn.node.getActivations(t1).size());

        Assert.assertEquals(1, pC.node.getActivations(t2).size());

        inA.removeInput(t2, 0, 4);

        Assert.assertEquals(0, pC.node.getActivations(t2).size());

        Assert.assertEquals(2, ctn.node.getThreadState(t3).activations.size());
    }


    @Test
    public void testMultithreading() {
        final Model m = new Model();
        AndNode.minFrequency = 1;

        Iteration t = m.startIteration(null, 0);

        final InputNeuron inA = t.createOrLookupInputSignal("A");
        final InputNeuron inB = t.createOrLookupInputSignal("B");

        final Neuron pC = t.createAndNeuron(new Neuron("pC"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        for (int i = 0; i < 5; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 20; j++) {
                        Document doc = Document.create("          ");
                        Iteration t = m.startIteration(doc, 0);

                        inA.addInput(t, 0, 6);
                        inB.addInput(t, 4, 10);

                        Assert.assertEquals(1, pC.node.getActivations(t).size());
                        Assert.assertNotNull(TestHelper.get(t, pC.node, new Range(4, 6), null));

                        inB.removeInput(t, 4, 10);
                        Assert.assertEquals(0, pC.node.getActivations(t).size());

                        t.clearActivations();

                        Assert.assertEquals(0, inA.node.getActivations(t).size());
                    }
                }
            }).run();
        }
    }
}
