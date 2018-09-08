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
package network.aika.network;


import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import network.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author Lukas Molzberger
 */
public class MutualExclusionTest {


    /**
     *     |----------------------------|
     *     | ------                     |
     *     -*| &  |                     |
     *  A ---| PA |------\   -------    |
     *       ------       \  |     |    |
     *                     \-| OR  |    |
     *       ------          |     |    |
     *  B ---| &  |----------| All |----|
     *     -*| PB |         -|     |    |
     *     | ------        / -------    |
     *     |----------------------------|
     *                   /              |
     *       ------     /               |
     *  C ---| &  |----/                |
     *     -*| PC |                     |
     *     | ------                     |
     *     |----------------------------|
     */


    @Test
    public void testMutualExclusion() {
        Model m = new Model();

        // Create the input neurons for the network.
        Neuron inA = m.createNeuron("IN-A");
        Neuron inB = m.createNeuron("IN-B");
        Neuron inC = m.createNeuron("IN-C");

        // Instantiate the inhibitory neuron. Its inputs will be added later on.
        Neuron inhibN = m.createNeuron("INHIB");

        // Create three neurons that might be suppressed by the inhibitory neuron.
        Neuron pA = Neuron.init(
                m.createNeuron("A"),
                3.0,
                ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhibN)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );

        Neuron pB = Neuron.init(
                m.createNeuron("B"),
                5.0,
                ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhibN)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );

        Neuron pC = Neuron.init(
                m.createNeuron("C"),
                2.0,
                ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhibN)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );

        // Finally addInput all the inputs to the suppressing neuron.
        Neuron.init(
                inhibN,
                0.0,
                ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT,
                INeuron.Type.INHIBITORY,
                INeuron.LogicType.DISJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(pA)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(pB)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(pC)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        );

        Neuron outN = Neuron.init(m.createNeuron("OUT"),
                0.0,
                ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(pB)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        );

        // Now that the model is complete, apply it to a document.

        Document doc = m.createDocument("foobar", 0);

        // Add input activations starting at char 0 and ending at char 1
        // These inputs will be immediately propagated through the network.
        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);
        inC.addInput(doc, 0, 1);

        // Computes the best interpretation
        doc.process();

        System.out.println(doc.activationsToString(true, false, true));

        Assert.assertTrue(pA.getActivations(doc, true).isEmpty());
        Assert.assertFalse(pB.getActivations(doc, true).isEmpty());
        Assert.assertTrue(pC.getActivations(doc, true).isEmpty());

        Assert.assertFalse(outN.getActivations(doc, true).isEmpty());
        doc.clearActivations();
    }
}
