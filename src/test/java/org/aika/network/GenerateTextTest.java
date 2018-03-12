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

import org.aika.ActivationFunction;
import org.aika.Document;
import org.aika.neuron.activation.Range;
import org.aika.neuron.Synapse;
import org.aika.Model;
import org.aika.neuron.Neuron;
import org.aika.neuron.INeuron;
import org.aika.neuron.activation.Range.Relation;
import org.aika.neuron.activation.Range.Output;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.neuron.activation.Range.Mapping.NONE;



/**
 *
 * @author Lukas Molzberger
 */
public class GenerateTextTest {

    @Test
    public void testTwoWords() {
        Model m = new Model();

        // The suppressing neuron should prevent that two neurons output overlapping text.
        Neuron suppr = m.createNeuron("SUPPR");

        // Some input neurons
        Neuron inA = m.createNeuron("IN-A");
        Neuron inB = m.createNeuron("IN-B");

        // Four test words that should be used to generate a text.
        Neuron outA = m.createNeuron("OUT-A", "aaaaaaa ");
        Neuron outB = m.createNeuron("OUT-B", "bbb ");
        Neuron outC = m.createNeuron("OUT-C", "ccccccccc ");
        Neuron outD = m.createNeuron("OUT-D", "ddddd ");

        // Word aaaaaaa is only added to the resulting text if input a is active and this neuron
        // is not suppressed by another neuron. Output aaaaaaa may start a text.
        Neuron.init(outA, 4.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Output.NONE),
                new Synapse.Builder()
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.OVERLAPS)
        );

        // Word bbb is only added to the resulting text if input b is active and this neuron
        // is not suppressed by another neuron. Output bbb may start a text.
        // Neuron outB has a slightly higher weight than outA.
        Neuron.init(outB, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Output.NONE),
                new Synapse.Builder()
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.OVERLAPS)
        );


        // OutC is only activated if the previous word was outA.
        Neuron.init(outC, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(outA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.END, NONE),
                new Synapse.Builder()
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.OVERLAPS)
        );

        // OutD is only activated if the previous word was outB.
        Neuron.init(outD, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(outB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.END, NONE),
                new Synapse.Builder()
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.OVERLAPS)
        );


        // All outputs suppress each other.
        Neuron.init(suppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setNeuron(outA)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(outB)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(outC)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(outD)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );


        Document doc = m.createDocument("Bla");

        // Add both input a and input b.
        inA.addInput(doc, 0, 3);
        inB.addInput(doc, 0, 3);

        System.out.println(doc.activationsToString(false, true, true));

        // Search for the best interpretation.
        doc.process();

        System.out.println(doc.activationsToString(true, true, true));

        System.out.println();

        // Generate the output text.
        System.out.println(doc.generateOutputText());

        Assert.assertEquals("bbb ddddd ", doc.generateOutputText());
    }
}
