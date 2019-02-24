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
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Activation.Link;
import network.aika.lattice.Node;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Stream;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.BEGIN_EQUALS;
import static network.aika.neuron.relation.Relation.END_EQUALS;
import static network.aika.neuron.relation.Relation.EQUALS;


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

        Neuron pAB = Neuron.init(m.createNeuron("pAB"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(Relation.EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(END_EQUALS)
        );


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);

        Activation inA1 = inA.getActivation(doc, 0, 1, false);
        Activation inB1 = inB.getActivation(doc, 0, 1, false);

        Assert.assertTrue(containsOutputActivation(inA1.getOutputLinks(), pAB.getActivation(doc, 0, 1, false)));
        Assert.assertTrue(containsOutputActivation(inB1.getOutputLinks(), pAB.getActivation(doc, 0, 1, false)));

        Activation actAB = pAB.getActivation(doc, 0, 1, false);
        Assert.assertEquals(
                inA.getActivation(doc, 0, 1, false),
                selectInputActivation(actAB.getInputLinks(false), inA)
        );

        actAB = pAB.getActivation(doc, 0, 1, false);
        Assert.assertEquals(
                inB.getActivation(doc, 0, 1, false),
                selectInputActivation(actAB.getInputLinks(false), inB)
        );


        Assert.assertTrue(containsOutputActivation(inA1.getOutputLinks(), pAB.getActivation(doc, 0, 1, false)));
        Assert.assertTrue(containsOutputActivation(inB1.getOutputLinks(), pAB.getActivation(doc, 0, 1, false)));

        actAB = pAB.getActivation(doc, 0, 1, false);
        Assert.assertEquals(
                inA.getActivation(doc, 0, 1, false),
                selectInputActivation(actAB.getInputLinks(false), inA)
        );

        actAB = pAB.getActivation(doc, 0, 1, false);
        Assert.assertEquals(
                inB.getActivation(doc, 0, 1, false),
                selectInputActivation(actAB.getInputLinks(false), inB)
        );
    }


    private Activation selectInputActivation(Stream<Link> acts, Neuron n) {
        return acts.filter(l -> l.getInput().getNeuron().compareTo(n) == 0).map(l -> l.getInput())
                .findAny()
                .orElse(null);
    }


    public boolean containsOutputActivation(Stream<Link> outputActivations, Activation oAct) {
        return outputActivations.anyMatch(l -> l.getOutput() == oAct);
    }


    @Test
    public void simpleAddActivationTest1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron outB = Neuron.init(m.createNeuron("B"), 0.5, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        inA.addInput(doc, new Activation.Builder()
                .setRange(0, 1)
        );

        Activation outB1 = outB.getActivation(doc, 0, 1, false);
        Assert.assertTrue(containsOutputActivation(inA.getActivation(doc, 0, 1, false).getOutputLinks(), outB1));
    }



    @Test
    public void removeRemoveDestinationActivation() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron outB = Neuron.init(m.createNeuron("B"), 0.001, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        inA.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 1)
        );

        Activation outB1 = outB.getActivation(doc, 0, 1, false);

        Assert.assertTrue(containsOutputActivation(inA.getActivation(doc, 0, 1, false).getOutputLinks(), outB1));
    }

}
