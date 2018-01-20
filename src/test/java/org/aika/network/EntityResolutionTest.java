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
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.corpus.Range.Relation.EQUALS;
import static org.aika.corpus.Range.Relation.NONE;

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


        Neuron.init(eJaguar, 2.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setBias(-4.75)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(ePuma)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setBias(-4.75)
                        .setRangeMatch(EQUALS)
        );

        Neuron.init(ePuma, 2.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setBias(-4.75)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(eJaguar)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setBias(-4.75)
                        .setRangeMatch(EQUALS)
        );


        Document doc = m.createDocument("jaguar puma ", 0);

        wJaguar.addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        wPuma.addInput(doc, 7, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        System.out.println("Process");
        doc.process();

        System.out.println(doc.neuronActivationsToString(true, false, true));


        Assert.assertNotNull(eJaguar.get().getFirstActivation(doc));
        Assert.assertNotNull(ePuma.get().getFirstActivation(doc));

        Assert.assertEquals(0, eJaguar.get().getFirstActivation(doc).key.interpretation.primId);
        Assert.assertEquals(1, ePuma.get().getFirstActivation(doc).key.interpretation.primId);

        Assert.assertEquals(doc.bottom, eJaguar.get().getFirstActivation(doc).key.interpretation.orInterpretationNodes.iterator().next());
        Assert.assertEquals(doc.bottom, ePuma.get().getFirstActivation(doc).key.interpretation.orInterpretationNodes.iterator().next());

        Assert.assertEquals(1, eJaguar.get().getFirstActivation(doc).key.interpretation.orInterpretationNodes.size());
        Assert.assertEquals(1, ePuma.get().getFirstActivation(doc).key.interpretation.orInterpretationNodes.size());
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
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(chCatsWithoutJaguar)
                        .setRecurrent(true)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false)
        );

        Neuron.init(ePuma, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(chCatsWithoutPuma)
                        .setRecurrent(true)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false)
        );


        Neuron.init(eLeopard, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(wLeopard)
                        .setRecurrent(false)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(chCatsWithoutLeopard)
                        .setRecurrent(true)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(false)
                        .setRangeMatch(NONE)
        );

        Neuron.init(cCats,
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        Neuron.init(chCatsWithoutJaguar, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false),
                new Synapse.Builder()
                        .setNeuron(eJaguar)
                        .setWeight(-30.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
        );

        Neuron.init(chCatsWithoutPuma, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false),
                new Synapse.Builder()
                        .setNeuron(ePuma)
                        .setWeight(-30.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
        );

        Neuron.init(chCatsWithoutLeopard, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(cCats)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false),
                new Synapse.Builder()
                        .setNeuron(eLeopard)
                        .setWeight(-30.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
        );




        Document doc = m.createDocument("jaguar puma ", 0);

        wJaguar.addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        wPuma.addInput(doc, 7, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        System.out.println("Process");
        doc.process();

        System.out.println(doc.neuronActivationsToString(true, false, true));


        Assert.assertNotNull(eJaguar.get().getFirstActivation(doc));
        Assert.assertNotNull(ePuma.get().getFirstActivation(doc));


        Assert.assertFalse(eJaguar.getFinalActivations(doc).isEmpty());
        Assert.assertFalse(ePuma.getFinalActivations(doc).isEmpty());
    }


}
