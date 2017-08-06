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


import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
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


        InputNeuron inA = m.createOrLookupInputNeuron("A");
        InputNeuron inB = m.createOrLookupInputNeuron("B");

        Neuron pC = m.createAndNeuron(new Neuron("pC"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );


        InputNeuron inStart = m.createOrLookupInputNeuron("START");
        InputNeuron inClock = m.createOrLookupInputNeuron("CLOCK");


        Neuron ctn = m.createCounterNeuron(new Neuron("CTN"), inClock, false, inStart, true, false);


        Document doc0 = m.createDocument("aaaaaaaaaa", 0);
        Document doc1 = m.createDocument("bbbbbbbbbb", 1);
        Document doc2 = m.createDocument("cccccccccc", 2);
        Document doc3 = m.createDocument("dddddddddd", 3);
        Document doc4 = m.createDocument("eeeeeeeeee", 4);


        inA.addInput(doc2, 0, 4);
        inB.addInput(doc2, 2, 6);

        Assert.assertEquals(1, pC.node.getActivations(doc2).size());

        inA.addInput(doc4, 0, 6);

        Assert.assertEquals(0, pC.node.getActivations(doc4).size());

        inB.addInput(doc0, 4, 8);
        inB.addInput(doc4, 2, 8);

        Assert.assertEquals(1, pC.node.getActivations(doc4).size());

        inA.addInput(doc0, 0, 6);

        inStart.addInput(doc3, 0, 1);
        inClock.addInput(doc3, 4, 5);
        inClock.addInput(doc3, 6, 7);

        Assert.assertEquals(2, ctn.node.getThreadState(doc3, true).activations.size());

        inStart.addInput(doc1, 0, 1);
        inClock.addInput(doc1, 3, 4);
        inClock.addInput(doc1, 7, 8);

        Assert.assertEquals(2, ctn.node.getThreadState(doc1, true).activations.size());
        Assert.assertEquals(2, ctn.node.getActivations(doc1).size());

        Assert.assertEquals(1, pC.node.getActivations(doc2).size());

        inA.removeInput(doc2, 0, 4);

        Assert.assertEquals(0, pC.node.getActivations(doc2).size());

        Assert.assertEquals(2, ctn.node.getThreadState(doc3, true).activations.size());
    }


    @Test
    public void testMultithreading() {
        final Model m = new Model();
        AndNode.minFrequency = 1;

        final InputNeuron inA = m.createOrLookupInputNeuron("A");
        final InputNeuron inB = m.createOrLookupInputNeuron("B");

        final Neuron pC = m.createAndNeuron(new Neuron("pC"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );

        for (int i = 0; i < 5; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 20; j++) {
                        Document doc = m.createDocument("          ", 0);

                        inA.addInput(doc, 0, 6);
                        inB.addInput(doc, 4, 10);

                        Assert.assertEquals(1, pC.node.getActivations(doc).size());
                        Assert.assertNotNull(TestHelper.get(doc, pC.node, new Range(4, 6), null));

                        inB.removeInput(doc, 4, 10);
                        Assert.assertEquals(0, pC.node.getActivations(doc).size());

                        doc.clearActivations();

                        Assert.assertEquals(0, inA.node.getActivations(doc).size());
                    }
                }
            }).run();
        }
    }
}
