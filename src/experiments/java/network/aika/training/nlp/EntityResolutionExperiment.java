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
package network.aika.training.nlp;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Collectors;

import static network.aika.neuron.INeuron.Type.*;
import static network.aika.neuron.relation.Relation.EQUALS;
import static network.aika.neuron.relation.Relation.ANY;

/**
 *
 * @author Lukas Molzberger
 */
public class EntityResolutionExperiment {



    @Test
    public void testSimpleERExample() {

        Model m = new Model();

        Neuron wJaguar = m.createNeuron("W-Jaguar", INPUT);
        Neuron wPuma = m.createNeuron("W-Puma", INPUT);

        Neuron eJaguar = m.createNeuron("E-Jaguar", EXCITATORY);
        Neuron ePuma = m.createNeuron("E-Puma", EXCITATORY);


        Neuron.init(eJaguar,
                4.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(5.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(ePuma)
                        .setRecurrent(true)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(ePuma, 4.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(5.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eJaguar)
                        .setRecurrent(true)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );


        Document doc = new Document(m, "jaguar puma ");

        wJaguar.addInput(doc, 0, 6);
        wPuma.addInput(doc, 7, 11);

        System.out.println("Process");
        doc.process();

        System.out.println(doc.activationsToString());


        Assert.assertFalse(eJaguar.get().getActivations(doc, false).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(ePuma.get().getActivations(doc, false).collect(Collectors.toList()).isEmpty());

        Assert.assertEquals(1, eJaguar.getActivation(doc, 0, 6, true).getId());
        Assert.assertEquals(3, ePuma.getActivation(doc, 7, 11, true).getId());

        Assert.assertEquals(0, eJaguar.getActivation(doc, 0, 6, true).getInputLinks().findFirst().orElse(null).getInput().getId());
        Assert.assertEquals(2, ePuma.getActivation(doc, 7, 11, true).getInputLinks().findFirst().orElse(null).getInput().getId());

        Assert.assertEquals(2, eJaguar.getActivation(doc, 0, 6, true).getInputLinks().count());
        Assert.assertEquals(2, ePuma.getActivation(doc, 7, 11, true).getInputLinks().count());
    }




    @Test
    public void testERExampleWithCategories() {

        Model m = new Model();

        Neuron wJaguar = m.createNeuron("W-Jaguar", INPUT);   // Word Neuron for Jaguar
        Neuron wPuma = m.createNeuron("W-Puma", INPUT);       // Word Neuron for Mountain Lion
        Neuron wLeopard = m.createNeuron("W-Leopard", INPUT); // Word Neuron for Leopard

        Neuron eJaguar = m.createNeuron("E-Jaguar", EXCITATORY);   // Meaning of the word Jaguar as a Cat
        Neuron ePuma = m.createNeuron("E-Puma", EXCITATORY);
        Neuron eLeopard = m.createNeuron("E-Leopard", EXCITATORY);

        Neuron cCats = m.createNeuron("C-Katzen", INHIBITORY);     // Category Cats
        Neuron chCatsWithoutJaguar = m.createNeuron("CH-Katzen/Jaguar", EXCITATORY);
        Neuron chCatsWithoutPuma = m.createNeuron("CH-Katzen/Puma", EXCITATORY);
        Neuron chCatsWithoutLeopard = m.createNeuron("CH-Katzen/Leopard", EXCITATORY);

        Neuron.init(eJaguar,
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(chCatsWithoutJaguar)
                        .setRecurrent(true)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(ePuma,
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(chCatsWithoutPuma)
                        .setRecurrent(true)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );


        Neuron.init(eLeopard,
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wLeopard)
                        .setRecurrent(false)
                        .setWeight(10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(chCatsWithoutLeopard)
                        .setRecurrent(true)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(cCats,
                0.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(chCatsWithoutJaguar,
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(eJaguar)
                        .setWeight(-30.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(chCatsWithoutPuma,
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(ePuma)
                        .setWeight(-30.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(chCatsWithoutLeopard,
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(eLeopard)
                        .setWeight(-30.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );




        Document doc = new Document(m, "jaguar puma ", 0);

        wJaguar.addInput(doc, 0, 6);
        wPuma.addInput(doc, 7, 11);

        System.out.println("Process");
        doc.process();

        System.out.println(doc.activationsToString());


        Assert.assertNotNull(eJaguar.getActivation(doc, 0, 6, true));
        Assert.assertNotNull(ePuma.getActivation(doc, 7, 11, true));


        Assert.assertFalse(eJaguar.getActivations(doc, true).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(ePuma.getActivations(doc, true).collect(Collectors.toList()).isEmpty());
    }


}
