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
import org.aika.Iteration.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode;
import org.aika.lattice.OrNode;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class PublicRangeTest {


    @Test
    public void testPublicRange() {
        Model m = new Model();
        AndNode.minFrequency = 5;

        Iteration t = m.startIteration(null, 0);
        InputNeuron inA = t.createOrLookupInputSignal("A");
        InputNeuron inB = t.createOrLookupInputSignal("B");

        Neuron pC = new Neuron("C");
        Neuron pD = new Neuron("D");

        t.createAndNeuron(pC,
                0.001,
                new Input().setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setStartVisibility(Synapse.RangeVisibility.MATCH_INPUT)
                        .setEndVisibility(Synapse.RangeVisibility.MATCH_INPUT)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setStartVisibility(Synapse.RangeVisibility.MATCH_INPUT)
                        .setEndVisibility(Synapse.RangeVisibility.MATCH_INPUT)
                        .setMatchRange(false)
        );
        OrNode pCNode = (OrNode) pC.node;

        t.createAndNeuron(pD,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );
        OrNode pDNode = (OrNode) pD.node;

        {
            Document doc = Document.create("aaaaaaaaaa");
            t = m.startIteration(doc, 0);

            inA.addInput(t, 2, 8, 0);
            inB.addInput(t, 4, 6, 0);

            Assert.assertNotNull(Activation.get(t, pCNode, 0, new Range(4, 6), Range.Relation.EQUALS, null, null));
//            Assert.assertNotNull(Activation.get(t, pDNode, 0, new Range(2, 8), Range.Relation.EQUALS, null, null));

            t.clearActivations();
        }
        {
            Document doc = Document.create("aaaaaaaaaa");
            t = m.startIteration(doc, 0);

            inB.addInput(t, 4, 6);
            inA.addInput(t, 2, 8);

            Assert.assertNotNull(Activation.get(t, pCNode, 0, new Range(4, 6), Range.Relation.EQUALS, null, null));
//            Assert.assertNotNull(Activation.get(t, pDNode, 0, new Range(2, 8), Range.Relation.EQUALS, null, null));
            t.clearActivations();
        }
    }

}
