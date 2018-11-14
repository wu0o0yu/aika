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

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.Converter;
import network.aika.neuron.INeuron;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Collectors;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.EQUALS;

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
                0.5,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(4.0)
                        .setBias(-4.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(3.0)
                        .setBias(-3.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(2)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(3)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, out.get().node.get().andParents.size());

        out.get().setBias(1.5);
        Converter.convert(0, null, out.get(), out.get().inputSynapses.values());

        System.out.println(out.get().node.get().logicToString());

        Assert.assertEquals(1, out.get().node.get().andParents.size());
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
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(2)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(3)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, inA.get().outputNode.get().orChildren.size());
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
                3.5,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(5.0)
                        .setBias(-5.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(5.0)
                        .setBias(-5.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(4)
                        .setNeuron(inE)
                        .setWeight(0.5)
                        .setBias(-0.5)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(2)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(3)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(4)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(4)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        System.out.println(out.get().node.get().logicToString());

        Assert.assertEquals(2, out.get().node.get().andParents.size());


        inD.inMemoryOutputSynapses.firstEntry().getValue().weightDelta = -1.5f;

        Converter.convert( 0, null, out.get(), out.get().inputSynapses.values());
        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, out.get().node.get().andParents.size());

    }


    @Test
    public void testConverter3() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");

        Neuron out = Neuron.init(m.createNeuron("ABCD"),
                5.5,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(50.0)
                        .setBias(-50.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(3.0)
                        .setBias(-3.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(2)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(3)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(3, out.get().node.get().andParents.size());

    }


    @Test
    public void testDuplicates() {
        Model m = new Model();

        Neuron in = m.createNeuron("IN");

        Neuron out = Neuron.init(m.createNeuron("OUT"),
                5.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(in)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        Document doc = m.createDocument("IN");

        in.addInput(doc, 0, 2);

        Assert.assertFalse(out.getActivations(doc, false).collect(Collectors.toList()).isEmpty());
    }

}
