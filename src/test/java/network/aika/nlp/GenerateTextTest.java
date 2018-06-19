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
package network.aika.nlp;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Range.Relation;
import network.aika.neuron.activation.Range.Output;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.Synapse.Builder.OUTPUT;
import static network.aika.neuron.activation.Range.Mapping.NONE;



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
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Output.NONE),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.OVERLAPS, OUTPUT)
        );

        // Word bbb is only added to the resulting text if input b is active and this neuron
        // is not suppressed by another neuron. Output bbb may start a text.
        // Neuron outB has a slightly higher weight than outA.
        Neuron.init(outB, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Output.NONE),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.OVERLAPS, OUTPUT)
        );


        // OutC is only activated if the previous word was outA.
        Neuron.init(outC, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(outA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.END, NONE),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.OVERLAPS, OUTPUT)
        );

        // OutD is only activated if the previous word was outB.
        Neuron.init(outD, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(outB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.END, NONE),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.OVERLAPS, OUTPUT)
        );


        // All outputs suppress each other.
        Neuron.init(suppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(outA)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(outB)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(outC)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(outD)
                        .setWeight(1.0)
                        .setBias(0.0)
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

        String outputText = doc.generateOutputText();
        // Generate the output text.
        System.out.println(outputText);

        Assert.assertEquals("bbb ddddd ", outputText);


        System.out.println(doc.activationsToString(true, true, true));
    }


    @Test
    public void intermediateOutputNeuron() {

        Model m = new Model();

        Neuron in = m.createNeuron("IN");

        Neuron intermediate = m.createNeuron("INTERMEDIATE");

        Neuron outA = m.createNeuron("OUT A", "aaaaaaa ");
        Neuron outB = m.createNeuron("OUT B", "bbb ");


        Neuron.init(intermediate, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setIdentity(true)
                        .setRangeOutput(Output.NONE)
        );

        Neuron.init(outA, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(intermediate)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Output.NONE)
        );


        Neuron.init(outB, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(intermediate)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.END, NONE)
        );

        Document doc = m.createDocument("in ");

        in.addInput(doc, 0, 1);

        doc.process();

        System.out.println(doc.activationsToString(true, true, true));

        Assert.assertEquals("aaaaaaa bbb ", doc.generateOutputText());
    }
}
