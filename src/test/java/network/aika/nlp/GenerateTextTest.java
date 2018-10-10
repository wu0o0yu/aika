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
import network.aika.neuron.range.Range;
import network.aika.neuron.INeuron;
import network.aika.neuron.range.Range.Relation;
import network.aika.neuron.range.Range.Output;
import network.aika.neuron.relation.InstanceRelation;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.Synapse.Builder.OUTPUT;
import static network.aika.neuron.range.Range.Mapping.NONE;
import static network.aika.neuron.relation.InstanceRelation.Type.CONTAINED_IN;
import static network.aika.neuron.relation.InstanceRelation.Type.CONTAINS;


/**
 *
 * @author Lukas Molzberger
 */
public class GenerateTextTest {

    @Test
    public void testTwoWords() {
        Model m = new Model();

        // The suppressing neuron should prevent that two neurons output overlapping text.
        Neuron inhib = m.createNeuron("INHIB");

        // Some input neurons
        Neuron inA = m.createNeuron("IN-A");
        Neuron inB = m.createNeuron("IN-B");
        Neuron inOut = m.createNeuron("IN-OUT");

        Neuron phraseA = m.createNeuron("PHRASE-A");
        Neuron phraseB = m.createNeuron("PHRASE-B");

        Neuron outputFrame = m.createNeuron("OUTPUT-FRAME");

        // Four test words that should be used to generate a text.
        Neuron outA = m.createNeuron("OUT-A", "aaaaaaa ");
        Neuron outB = m.createNeuron("OUT-B", "bbb ");
        Neuron outC = m.createNeuron("OUT-C", "ccccccccc ");
        Neuron outD = m.createNeuron("OUT-D", "ddddd ");



        // Word aaaaaaa is only added to the resulting text if input a is active and this neuron
        // is not suppressed by another neuron. Output aaaaaaa may start a text.
        Neuron.init(phraseA, 4.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(outputFrame)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Relation.NONE, 0)
                        .setRangeOutput(Output.NONE),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, OUTPUT)
        );

        Neuron.init(phraseB, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(outputFrame)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Relation.NONE, 0)
                        .setRangeOutput(Output.NONE),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, OUTPUT)
        );


        Neuron.init(outputFrame, 4.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inOut)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.END, Range.Mapping.CREATE)
        );


        Neuron.init(outA, 4.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(phraseA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setIdentity(true)
                        .setRangeOutput(Range.Mapping.BEGIN, Range.Mapping.CREATE)
        );



        // OutC is only activated if the previous word was outA.
        Neuron.init(outC, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(phraseA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.NONE, Range.Mapping.END),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(outA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addInstanceRelation(CONTAINS, 0)
                        .setRangeOutput(Range.Mapping.END, Range.Mapping.NONE)
        );


        // Word bbb is only added to the resulting text if input b is active and this neuron
        // is not suppressed by another neuron. Output bbb may start a text.
        // Neuron outB has a slightly higher weight than outA.
        Neuron.init(outB, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(phraseB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setIdentity(true)
                        .setRangeOutput(Range.Mapping.BEGIN, Range.Mapping.CREATE)
        );


        // OutD is only activated if the previous word was outB.
        Neuron.init(outD, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(phraseB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.NONE, Range.Mapping.END),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(outB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addInstanceRelation(CONTAINS, 0)
                        .setRangeOutput(Range.Mapping.END, Range.Mapping.NONE)
        );


        // All outputs suppress each other.
        Neuron.init(inhib, 0.0, ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(phraseA)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(phraseB)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );


        Document doc = m.createDocument("Bla");

        // Add both input a and input b.
        inA.addInput(doc, 0, 3);
        inB.addInput(doc, 0, 3);

        inOut.addInput(doc, 0, 3);

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
                        .setRangeOutput(Range.Mapping.END, Range.Mapping.CREATE)
        );

        Neuron.init(outA, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(intermediate)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Output.DIRECT)
        );


        Neuron.init(outB, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(intermediate)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Mapping.END, Range.Mapping.CREATE)
        );

        Document doc = m.createDocument("in ");

        in.addInput(doc, 0, 3);

        doc.process();

        String outputText = doc.generateOutputText();

        System.out.println(doc.activationsToString(true, true, true));

        Assert.assertEquals("aaaaaaa bbb ", outputText);
    }
}
