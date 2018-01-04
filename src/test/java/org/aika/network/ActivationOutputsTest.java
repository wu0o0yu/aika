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
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.lattice.OrNode;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;
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

        Neuron pAB = m.initNeuron(m.createNeuron("pAB"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setAbsoluteRid(0)
                        .setBias(-1.0)
                        .setRangeMatch(Operator.EQUALS, NONE)
                        .setRangeOutput(Range.Output.BEGIN),
                new Input()
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

        Activation inA1 = TestHelper.get(doc, inA.get(), new Range(0, 1), doc.bottom);
        Activation inB1 = TestHelper.get(doc, inB.get(), new Range(0, 1), doc.bottom);

        Assert.assertTrue(containsOutputActivation(inA1.neuronOutputs, TestHelper.get(doc, pAB.get(), new Range(0, 1), null)));
        Assert.assertTrue(containsOutputActivation(inB1.neuronOutputs, TestHelper.get(doc, pAB.get(), new Range(0, 1), null)));

        Activation actAB = TestHelper.get(doc, pAB.get(), new Range(0, 1), null);
        Assert.assertEquals(
                TestHelper.get(doc, inA.get(), new Range(0, 1), null),
                selectInputActivation(actAB.neuronInputs, inA.get().node.get())
        );

        actAB = TestHelper.get(doc, pAB.get(), new Range(0, 1), null);
        Assert.assertEquals(
                TestHelper.get(doc,inB.get(), new Range(0, 1), null),
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

        Assert.assertTrue(containsOutputActivation(inA1.neuronOutputs, TestHelper.get(doc, pAB.get(), new Range(0, 1), null)));
        Assert.assertTrue(containsOutputActivation(inB1.neuronOutputs, TestHelper.get(doc, pAB.get(), new Range(0, 1), null)));

        actAB = TestHelper.get(doc, pAB.get(), new Range(0, 1), null);
        Assert.assertEquals(
                TestHelper.get(doc, inA.get(), new Range(0, 1), null),
                selectInputActivation(actAB.neuronInputs, inA.get().node.get())
        );

        actAB = TestHelper.get(doc, pAB.get(), new Range(0, 1), null);
        Assert.assertEquals(
                TestHelper.get(doc, inB.get(), new Range(0, 1), null),
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

        INeuron outB = m.initNeuron(m.createNeuron("B"), 0.5, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        ).get();


        InterprNode o1 = InterprNode.addPrimitive(doc);
        inA.addInput(doc, 0, 1, o1);

        Activation outB1 = Activation.get(doc, outB, null, new Range(0, 1), Range.Relation.CONTAINS, null, null);
        Assert.assertTrue(containsOutputActivation(inA.get().getFirstActivation(doc).neuronOutputs, outB1));
    }


    @Test
    public void simpleAddActivationTest2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        INeuron outB = m.initNeuron(m.createNeuron("B"), 0.5, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        ).get();


        inA.addInput(doc, 0, 1, InterprNode.addPrimitive(doc));

        Activation outB1 = Activation.get(doc, outB, null, new Range(0, 1), Range.Relation.CONTAINS, null, null);

        Assert.assertTrue(containsOutputActivation(inA.get().getFirstActivation(doc).neuronOutputs, outB1));
    }


    @Test
    public void removeRemoveDestinationActivation() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        INeuron outB = m.initNeuron(m.createNeuron("B"), 0.001, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        ).get();


        InterprNode o1 = InterprNode.addPrimitive(doc);
        inA.addInput(doc, 0, 1, 0, o1);
        Activation outB1 = Activation.get(doc, outB, null, new Range(0, 1), Range.Relation.CONTAINS, null, null);

        Assert.assertTrue(containsOutputActivation(inA.get().getFirstActivation(doc).neuronOutputs, outB1));
    }

}
