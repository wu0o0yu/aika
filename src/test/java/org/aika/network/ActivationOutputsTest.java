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
import org.aika.lattice.NodeActivation;
import org.aika.neuron.Activation;
import org.aika.neuron.Activation.SynapseActivation;
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.lattice.AndNode;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.lattice.OrNode;
import org.aika.neuron.Synapse;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static org.aika.Input.RangeRelation.EQUALS;
import static org.aika.corpus.Range.Operator.GREATER_THAN;
import static org.aika.corpus.Range.Operator.LESS_THAN;

/**
 *
 * @author Lukas Molzberger
 */
public class ActivationOutputsTest {


    @Test
    public void addActivationsTest() {
        Model m = new Model();
        AndNode.minFrequency = 5;

        m.numberOfPositions = 10;

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron pAB = m.initNeuron(m.createNeuron("pAB"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setAbsoluteRid(0)
                        .setBiasDelta(1.0)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setAbsoluteRid(0)
                        .setBiasDelta(1.0)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1, 0);
        inB.addInput(doc, 0, 1, 0);

        Activation inA1 = TestHelper.get(doc, inA.get().node.get(), new Range(0, 1), doc.bottom);
        Activation inB1 = TestHelper.get(doc, inB.get().node.get(), new Range(0, 1), doc.bottom);

        Assert.assertTrue(containsOutputActivation(inA1.neuronOutputs, TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null)));
        Assert.assertTrue(containsOutputActivation(inB1.neuronOutputs, TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null)));

        Activation actAB = TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null);
        Assert.assertEquals(
                TestHelper.get(doc, inA.get().node.get(), new Range(0, 1), null),
                selectInputActivation(actAB.neuronInputs, inA.get().node.get())
        );

        actAB = TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null);
        Assert.assertEquals(
                TestHelper.get(doc,inB.get().node.get(), new Range(0, 1), null),
                selectInputActivation(actAB.neuronInputs, inB.get().node.get())
        );


        InputNode pC = new InputNode(m, new Synapse.Key(false, 0, null, LESS_THAN, Mapping.START, true, GREATER_THAN, Mapping.END, true));
        NodeActivation pC1 = TestHelper.addActivation(pC, doc, TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null));

        Assert.assertTrue(containsOutputActivation(inA1.neuronOutputs, TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null)));
        Assert.assertTrue(containsOutputActivation(inB1.neuronOutputs, TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null)));

        actAB = TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null);
        Assert.assertEquals(
                TestHelper.get(doc, inA.get().node.get(), new Range(0, 1), null),
                selectInputActivation(actAB.neuronInputs, inA.get().node.get())
        );

        actAB = TestHelper.get(doc, pAB.get().node.get(), new Range(0, 1), null);
        Assert.assertEquals(
                TestHelper.get(doc, inB.get().node.get(), new Range(0, 1), null),
                selectInputActivation(actAB.neuronInputs, inB.get().node.get())
        );
    }


    private Activation selectInputActivation(Set<SynapseActivation> acts, Node n) {
        for(SynapseActivation sa: acts) {
            if(sa.input.key.n.compareTo(n) == 0) {
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
    public void removeActivationsTest() {
        Model m = new Model();
        AndNode.minFrequency = 10;

        m.numberOfPositions = 10;

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron pAB = m.initNeuron(m.createNeuron("B-NA", true, false),
                0.5,
                new Input()
                        .setNeuron(inA)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setBiasDelta(1.0)
                        .setMaxLowerWeightsSum(0.0f),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBiasDelta(0.95)
                        .setMaxLowerWeightsSum(0.0f)
        );
        OrNode pABNode = pAB.get().node.get();


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inB.addInput(doc, 0, 1);
        inA.addInput(doc, 0, 1, InterprNode.addPrimitive(doc));

        NodeActivation actAB = TestHelper.get(doc, pABNode, new Range(0, 1), null);

        System.out.println(doc.neuronActivationsToString(false, false, true));

        Assert.assertTrue(!actAB.key.o.conflicts.primary.isEmpty());
    }


    @Test
    public void simpleAddActivationTest1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        OrNode outBNode = m.initNeuron(m.createNeuron("B", true, false), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setBiasDelta(1.0)
        ).get().node.get();


        InterprNode o1 = InterprNode.addPrimitive(doc);
        inA.addInput(doc, 0, 1, o1);

        Activation outB1 = NodeActivation.get(doc, outBNode, null, new Range(0, 1), LESS_THAN, GREATER_THAN, null, null);
        Assert.assertTrue(containsOutputActivation(inA.get().node.get().getFirstActivation(doc).neuronOutputs, outB1));
    }


    @Test
    public void simpleAddActivationTest2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        OrNode outBNode = m.initNeuron(m.createNeuron("B", true, false), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setBiasDelta(1.0f)
        ).get().node.get();


        inA.addInput(doc, 0, 1, InterprNode.addPrimitive(doc));

        Activation outB1 = NodeActivation.get(doc, outBNode, null, new Range(0, 1), LESS_THAN, GREATER_THAN, null, null);

        Assert.assertTrue(containsOutputActivation(inA.get().node.get().getFirstActivation(doc).neuronOutputs, outB1));
    }


    @Test
    public void removeRemoveDestinationActivation() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        OrNode outBNode = m.initNeuron(m.createNeuron("B", true, false), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0f)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setBiasDelta(1.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        ).get().node.get();


        InterprNode o1 = InterprNode.addPrimitive(doc);
        inA.addInput(doc, 0, 1, 0, o1);
        Activation outB1 = NodeActivation.get(doc, outBNode, null, new Range(0, 1), LESS_THAN, GREATER_THAN, null, null);

        Assert.assertTrue(containsOutputActivation(inA.get().node.get().getFirstActivation(doc).neuronOutputs, outB1));

        inA.get().removeInput(doc, 0, 1, 0, o1);

        Assert.assertNull(inA.get().node.get().getFirstActivation(doc));
        Assert.assertTrue(outB1.isRemoved);
    }

}
