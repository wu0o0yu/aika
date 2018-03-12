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


import org.aika.neuron.Synapse;
import org.aika.Model;
import org.aika.neuron.Neuron;
import org.aika.neuron.activation.Range;
import org.aika.Document;
import org.aika.neuron.INeuron;
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

        // Instantiate the suppressing neuron. Its inputs will be added later on.
        Neuron pSuppr = m.createNeuron("SUPPRESS");

        // Create three neurons that might be suppressed by the suppressing neuron.
        Neuron pA = Neuron.init(
                m.createNeuron("A"),
                3.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pSuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );

        Neuron pB = Neuron.init(
                m.createNeuron("B"),
                5.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pSuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );

        Neuron pC = Neuron.init(
                m.createNeuron("C"),
                2.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pSuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );

        // Finally addInput all the inputs to the suppressing neuron.
        Neuron.init(
                pSuppr,
                0.0,
                INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setNeuron(pA)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pB)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pC)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true)
        );

        Neuron outN = Neuron.init(m.createNeuron("OUT"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(pB)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true)
        );

        // Now that the model is complete, apply it to a document.

        Document doc = m.createDocument("foobar", 0);

        // Add input activations starting at char 0 and ending at char 1
        // These inputs will be immediately propagated through the network.
        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);
        inC.addInput(doc, 0, 1);

        // Computes the selected option
        doc.process();

        System.out.println(doc.activationsToString(true, false, true));

        Assert.assertTrue(pA.getFinalActivations(doc).isEmpty());
        Assert.assertFalse(pB.getFinalActivations(doc).isEmpty());
        Assert.assertTrue(pC.getFinalActivations(doc).isEmpty());

        Assert.assertFalse(outN.getFinalActivations(doc).isEmpty());
        doc.clearActivations();
    }
}
