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


import org.aika.neuron.*;
import org.aika.neuron.Activation.SynapseActivation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static org.aika.corpus.Range.Relation.EQUALS;
import static org.aika.corpus.Range.Operator.GREATER_THAN_EQUAL;
import static org.aika.corpus.Range.Operator.LESS_THAN_EQUAL;
import static org.aika.corpus.Range.Operator.NONE;


/**
 *
 * @author Lukas Molzberger
 */
public class ActivationOutputsTest {


    @Test
    public void addActivationsTest() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron pAB = Neuron.init(m.createNeuron("pAB"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setAbsoluteRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(Operator.EQUALS, NONE)
                        .setRangeOutput(Range.Output.BEGIN),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setAbsoluteRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(NONE, Operator.EQUALS)
                        .setRangeOutput(Range.Output.END)
        );


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1, 0);
        inB.addInput(doc, 0, 1, 0);

        Activation inA1 = TestHelper.get(doc, inA.get(), new Range(0, 1));
        Activation inB1 = TestHelper.get(doc, inB.get(), new Range(0, 1));

        Assert.assertTrue(containsOutputActivation(inA1.neuronOutputs, TestHelper.get(doc, pAB.get(), new Range(0, 1))));
        Assert.assertTrue(containsOutputActivation(inB1.neuronOutputs, TestHelper.get(doc, pAB.get(), new Range(0, 1))));

        Activation actAB = TestHelper.get(doc, pAB.get(), new Range(0, 1));
        Assert.assertEquals(
                TestHelper.get(doc, inA.get(), new Range(0, 1)),
                selectInputActivation(actAB.neuronInputs, inA.get().node.get())
        );

        actAB = TestHelper.get(doc, pAB.get(), new Range(0, 1));
        Assert.assertEquals(
                TestHelper.get(doc,inB.get(), new Range(0, 1)),
                selectInputActivation(actAB.neuronInputs, inB.get().node.get())
        );


        InputNode pC = new InputNode(m,
                new Synapse.Key(
                        false,
                        0,
                        null,
                        Range.Relation.create(LESS_THAN_EQUAL, GREATER_THAN_EQUAL),
                        Range.Output.DIRECT)
        );

        Assert.assertTrue(containsOutputActivation(inA1.neuronOutputs, TestHelper.get(doc, pAB.get(), new Range(0, 1))));
        Assert.assertTrue(containsOutputActivation(inB1.neuronOutputs, TestHelper.get(doc, pAB.get(), new Range(0, 1))));

        actAB = TestHelper.get(doc, pAB.get(), new Range(0, 1));
        Assert.assertEquals(
                TestHelper.get(doc, inA.get(), new Range(0, 1)),
                selectInputActivation(actAB.neuronInputs, inA.get().node.get())
        );

        actAB = TestHelper.get(doc, pAB.get(), new Range(0, 1));
        Assert.assertEquals(
                TestHelper.get(doc, inB.get(), new Range(0, 1)),
                selectInputActivation(actAB.neuronInputs, inB.get().node.get())
        );
    }


    private Activation selectInputActivation(Set<SynapseActivation> acts, Node n) {
        for(SynapseActivation sa: acts) {
            if(sa.input.key.node.compareTo(n) == 0) {
                return sa.input;
            }
        }
        return null;
    }


    public boolean containsOutputActivation(Set<SynapseActivation> outputActivations, Activation oAct) {
        for(SynapseActivation sa: outputActivations) {
            if(sa.output == oAct) return true;
        }
        return false;
    }


    @Test
    public void simpleAddActivationTest1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        INeuron outB = Neuron.init(m.createNeuron("B"), 0.5, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        ).get();


        inA.addInput(doc, new Activation.Builder()
                .setRange(0, 1)
                .setRelationalId(0)
        );

        Activation outB1 = Selector.get(doc, outB, null, new Range(0, 1), Range.Relation.CONTAINS);
        Assert.assertTrue(containsOutputActivation(inA.get().getFirstActivation(doc).neuronOutputs, outB1));
    }


    @Test
    public void simpleAddActivationTest2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        INeuron outB = Neuron.init(m.createNeuron("B"), 0.5, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        ).get();


        inA.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 1)
                        .setRelationalId(0)
        );

        Activation outB1 = Selector.get(doc, outB, null, new Range(0, 1), Range.Relation.CONTAINS);

        Assert.assertTrue(containsOutputActivation(inA.get().getFirstActivation(doc).neuronOutputs, outB1));
    }


    @Test
    public void removeRemoveDestinationActivation() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        INeuron outB = Neuron.init(m.createNeuron("B"), 0.001, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        ).get();


        inA.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 1)
                        .setRelationalId(0)
        );

        Activation outB1 = Selector.get(doc, outB, null, new Range(0, 1), Range.Relation.CONTAINS);

        Assert.assertTrue(containsOutputActivation(inA.get().getFirstActivation(doc).neuronOutputs, outB1));
    }

}
