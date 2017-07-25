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
import org.aika.Input;
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode;
import org.aika.lattice.OrNode;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.RangeMatch;
import org.aika.neuron.Synapse.RangeSignal;

/**
 *
 * @author Lukas Molzberger
 */
public class PublicRangeTest {


    @Ignore
    @Test
    public void testPublicRange() {
        Model m = new Model();
        AndNode.minFrequency = 5;

        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");

        Neuron pC = new Neuron("C");
        Neuron pD = new Neuron("D");

        m.createAndNeuron(pC,
                0.001,
                new Input().setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.NONE)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.NONE)
                        .setEndRangeOutput(true)
        );
        OrNode pCNode = (OrNode) pC.node;

        m.createAndNeuron(pD,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.NONE)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.NONE)
                        .setStartRangeOutput(true)
        );
        OrNode pDNode = (OrNode) pD.node;

        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inA.addInput(doc, 2, 8, 0);
            inB.addInput(doc, 4, 6, 0);

            Assert.assertNotNull(Activation.get(doc, pCNode, 0, new Range(4, 6), Range.Relation.EQUALS, null, null));
//            Assert.assertNotNull(Activation.get(t, pDNode, 0, new Range(2, 8), Range.Relation.EQUALS, null, null));

            doc.clearActivations();
        }
        {
            Document doc = m.createDocument("aaaaaaaaaa", 0);

            inB.addInput(doc, 4, 6);
            inA.addInput(doc, 2, 8);

            Assert.assertNotNull(Activation.get(doc, pCNode, 0, new Range(4, 6), Range.Relation.EQUALS, null, null));
//            Assert.assertNotNull(Activation.get(t, pDNode, 0, new Range(2, 8), Range.Relation.EQUALS, null, null));
            doc.clearActivations();
        }
    }

}
