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

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.Converter;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.Range.Relation;
import network.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class ConverterTest {


    @Test
    public void testConverter() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");

        Neuron out = Neuron.init(m.createNeuron("ABCD"),
                -9.5,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(4.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(3.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 2)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 3)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, out.get().node.get().parents.size());

        out.get().setBias(-8.5);
        Converter.convert(0, null, out.get(), out.get().inputSynapses.values());

        System.out.println(out.get().node.get().logicToString());

        Assert.assertEquals(1, out.get().node.get().parents.size());
    }


    @Test
    public void testConverter1() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");

        Neuron out = Neuron.init(m.createNeuron("ABCD"),
                -5.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 1)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 2)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 3)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeOutput(Range.Output.DIRECT)
        );

        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, out.get().node.get().parents.size());
    }


    @Test
    public void testConverter2() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");
        Neuron inE = m.createNeuron("E");

        Neuron out = Neuron.init(m.createNeuron("ABCD"),
                -11.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(5.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 1)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(5.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 2)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 3)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 4)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(4)
                        .setNeuron(inE)
                        .setWeight(0.5f)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeOutput(Range.Output.DIRECT)
        );

        System.out.println(out.get().node.get().logicToString());

        Assert.assertEquals(2, out.get().node.get().parents.size());


        inD.inMemoryOutputSynapses.firstEntry().getValue().weightDelta = -1.5f;

        Converter.convert( 0, null, out.get(), out.get().inputSynapses.values());
        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, out.get().node.get().parents.size());

    }


    @Test
    public void testConverter3() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");

        Neuron out = Neuron.init(m.createNeuron("ABCD"),
                -50.5,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(3.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 2)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .addRangeRelation(Relation.EQUALS, 3)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(3, out.get().node.get().parents.size());

    }

}
