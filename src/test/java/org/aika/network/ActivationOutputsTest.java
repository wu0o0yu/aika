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
import org.aika.Activation.SynapseActivation;
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
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
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

        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");

        Neuron pAB = m.createAndNeuron(new Neuron("pAB"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setAbsoluteRid(0)
                        .setMinInput(1.0)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setAbsoluteRid(0)
                        .setMinInput(1.0)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1, 0);
        inB.addInput(doc, 0, 1, 0);

        Activation inA1 = TestHelper.get(doc, inA.node, new Range(0, 1), doc.bottom);
        Activation inB1 = TestHelper.get(doc, inB.node, new Range(0, 1), doc.bottom);

        Assert.assertTrue(containsOutputActivation(inA1.neuronOutputs, TestHelper.get(doc, pAB.node, new Range(0, 1), null)));
        Assert.assertTrue(containsOutputActivation(inB1.neuronOutputs, TestHelper.get(doc, pAB.node, new Range(0, 1), null)));

        Assert.assertEquals(
                TestHelper.get(doc, inA.node, new Range(0, 1), null),
                selectInputActivation(TestHelper.get(doc, pAB.node, new Range(0, 1), null).neuronInputs, inA.node)
        );
        Assert.assertEquals(
                TestHelper.get(doc,inB.node, new Range(0, 1), null),
                selectInputActivation(TestHelper.get(doc, pAB.node, new Range(0, 1), null).neuronInputs, inB.node)
        );


        InputNode pC = new InputNode(doc, new Synapse.Key(false, false, 0, null, LESS_THAN, Mapping.START, true, GREATER_THAN, Mapping.END, true));
        Activation pC1 = TestHelper.addActivation(pC, doc, TestHelper.get(doc, pAB.node, new Range(0, 1), null));

        Assert.assertTrue(containsOutputActivation(inA1.neuronOutputs, TestHelper.get(doc, pAB.node, new Range(0, 1), null)));
        Assert.assertTrue(containsOutputActivation(inB1.neuronOutputs, TestHelper.get(doc, pAB.node, new Range(0, 1), null)));

        Assert.assertEquals(
                TestHelper.get(doc, inA.node, new Range(0, 1), null),
                selectInputActivation(TestHelper.get(doc, pAB.node, new Range(0, 1), null).neuronInputs, inA.node)
        );
        Assert.assertEquals(
                TestHelper.get(doc, inB.node, new Range(0, 1), null),
                selectInputActivation(TestHelper.get(doc, pAB.node, new Range(0, 1), null).neuronInputs, inB.node)
        );
    }


    private Activation selectInputActivation(Collection<SynapseActivation> acts, Node n) {
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

        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");

        Neuron pAB = m.createAndNeuron(new Neuron("B-NA", true, false),
                0.5,
                new Input()
                        .setNeuron(inA)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setRelativeRid(0)
                        .setMinInput(1.0)
                        .setMaxLowerWeightsSum(0.0),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(0.95)
                        .setMaxLowerWeightsSum(0.0)
        );
        Node pABNode = pAB.node;


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inB.addInput(doc, 0, 1);
        inA.addInput(doc, 0, 1, InterprNode.addPrimitive(doc));

        Activation actAB = TestHelper.get(doc, pABNode, new Range(0, 1), null);

        System.out.println(doc.networkStateToString(true, false));

        Assert.assertTrue(!actAB.key.o.conflicts.primary.isEmpty());
    }


    @Test
    public void simpleAddActivationTest1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        InputNeuron inA = m.createOrLookupInputSignal("A");

        Node outBNode = m.createAndNeuron(new Neuron("B", true, false), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        ).node;


        InterprNode o1 = InterprNode.addPrimitive(doc);
        inA.addInput(doc, 0, 1, o1);

        Activation outB1 = Activation.get(doc, outBNode, null, new Range(0, 1), LESS_THAN, GREATER_THAN, null, null);
        Assert.assertTrue(containsOutputActivation(inA.node.getFirstActivation(doc).neuronOutputs, outB1));
    }


    @Test
    public void simpleAddActivationTest2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        InputNeuron inA = m.createOrLookupInputSignal("A");

        Node outBNode = m.createAndNeuron(new Neuron("B", true, false), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        ).node;


        inA.addInput(doc, 0, 1, InterprNode.addPrimitive(doc));

        Activation outB1 = Activation.get(doc, outBNode, null, new Range(0, 1), LESS_THAN, GREATER_THAN, null, null);

        Assert.assertTrue(containsOutputActivation(inA.node.getFirstActivation(doc).neuronOutputs, outB1));
    }


    @Test
    public void removeRemoveDestinationActivation() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        InputNeuron inA = m.createOrLookupInputSignal("A");

        OrNode outBNode = (OrNode) m.createAndNeuron(new Neuron("B", true, false), 0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        ).node;


        InterprNode o1 = InterprNode.addPrimitive(doc);
        inA.addInput(doc, 0, 1, 0, o1);
        Activation outB1 = Activation.get(doc, outBNode, null, new Range(0, 1), LESS_THAN, GREATER_THAN, null, null);

        Assert.assertTrue(containsOutputActivation(inA.node.getFirstActivation(doc).neuronOutputs, outB1));

        inA.removeInput(doc, 0, 1, 0, o1);

        Assert.assertNull(inA.node.getFirstActivation(doc));
        Assert.assertTrue(outB1.isRemoved);
    }

}
