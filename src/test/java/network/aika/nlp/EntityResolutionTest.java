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


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Range;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.activation.Range.Relation.EQUALS;
import static network.aika.neuron.activation.Range.Relation.NONE;

/**
 *
 * @author Lukas Molzberger
 */
public class EntityResolutionTest {



    @Test
    public void testSimpleERExample() {

        Model m = new Model();

        Neuron wJaguar = m.createNeuron("W-Jaguar");
        Neuron wPuma = m.createNeuron("W-Puma");

        Neuron eJaguar = m.createNeuron("E-Jaguar");
        Neuron ePuma = m.createNeuron("E-Puma");


        Neuron.init(eJaguar, 4.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setBias(-5.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(ePuma)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setBias(-5.0)
                        .addRangeRelation(Range.Relation.NONE, 0)
        );

        Neuron.init(ePuma, 4.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setBias(-5.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eJaguar)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setBias(-5.0)
                        .addRangeRelation(Range.Relation.NONE, 0)
        );


        Document doc = m.createDocument("jaguar puma ", 0);

        wJaguar.addInput(doc, 0, 6);

        System.out.println(doc.activationsToString(false, false, true));

        wPuma.addInput(doc, 7, 11);

        System.out.println(doc.activationsToString(false, false, true));

        System.out.println("Process");
        doc.process();

        System.out.println(doc.activationsToString(true, false, true));


        Assert.assertFalse(eJaguar.get().getActivations(doc, false).isEmpty());
        Assert.assertFalse(ePuma.get().getActivations(doc, false).isEmpty());

        Assert.assertEquals(2, eJaguar.getActivation(doc, new Range(0, 6), true).id);
        Assert.assertEquals(6, ePuma.getActivation(doc, new Range(7, 11), true).id);

        Assert.assertEquals(0, eJaguar.getActivation(doc, new Range(0, 6), true).neuronInputs.values().iterator().next().input.id);
        Assert.assertEquals(4, ePuma.getActivation(doc, new Range(7, 11), true).neuronInputs.values().iterator().next().input.id);

        Assert.assertEquals(2, eJaguar.getActivation(doc, new Range(0, 6), true).neuronInputs.size());
        Assert.assertEquals(2, ePuma.getActivation(doc, new Range(7, 11), true).neuronInputs.size());
    }




    @Test
    public void testERExampleWithCategories() {

        Model m = new Model();

        Neuron wJaguar = m.createNeuron("W-Jaguar");   // Word Neuron for Jaguar
        Neuron wPuma = m.createNeuron("W-Puma");       // Word Neuron for Mountain Lion
        Neuron wLeopard = m.createNeuron("W-Leopard"); // Word Neuron for Leopard

        Neuron eJaguar = m.createNeuron("E-Jaguar");   // Meaning of the word Jaguar as a Cat
        Neuron ePuma = m.createNeuron("E-Puma");
        Neuron eLeopard = m.createNeuron("E-Leopard");

        Neuron cCats = m.createNeuron("C-Katzen");     // Category Cats
        Neuron chCatsWithoutJaguar = m.createNeuron("CH-Katzen/Jaguar");
        Neuron chCatsWithoutPuma = m.createNeuron("CH-Katzen/Puma");
        Neuron chCatsWithoutLeopard = m.createNeuron("CH-Katzen/Leopard");

        Neuron.init(eJaguar, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(chCatsWithoutJaguar)
                        .setRecurrent(true)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Range.Relation.NONE, 0)
                        .setRangeOutput(false)
        );

        Neuron.init(ePuma, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(chCatsWithoutPuma)
                        .setRecurrent(true)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Range.Relation.NONE, 0)
                        .setRangeOutput(false)
        );


        Neuron.init(eLeopard, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wLeopard)
                        .setRecurrent(false)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(chCatsWithoutLeopard)
                        .setRecurrent(true)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Range.Relation.NONE, 0)
        );

        Neuron.init(cCats,
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        );

        Neuron.init(chCatsWithoutJaguar, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .addRangeRelation(Range.Relation.NONE, 0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(eJaguar)
                        .setWeight(-30.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );

        Neuron.init(chCatsWithoutPuma, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .addRangeRelation(Range.Relation.NONE, 0)
                        .setRangeOutput(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(ePuma)
                        .setWeight(-30.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );

        Neuron.init(chCatsWithoutLeopard, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .addRangeRelation(Range.Relation.NONE, 0)
                        .setRangeOutput(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(eLeopard)
                        .setWeight(-30.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );




        Document doc = m.createDocument("jaguar puma ", 0);

        wJaguar.addInput(doc, 0, 6);

        System.out.println(doc.activationsToString(false, false, true));

        wPuma.addInput(doc, 7, 11);

        System.out.println(doc.activationsToString(false, false, true));

        System.out.println("Process");
        doc.process();

        System.out.println(doc.activationsToString(true, false, true));


        Assert.assertNotNull(eJaguar.getActivation(doc, new Range(0, 6), true));
        Assert.assertNotNull(ePuma.getActivation(doc, new Range(7, 11), true));


        Assert.assertFalse(eJaguar.getActivations(doc, true).isEmpty());
        Assert.assertFalse(ePuma.getActivations(doc, true).isEmpty());
    }


}
