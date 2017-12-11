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


import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.Input.RangeRelation.EQUALS;
import static org.aika.Input.RangeRelation.NONE;

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


        m.initNeuron(eJaguar, 2.0,
                new Input()
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(5.0f)
                        .setBiasDelta(-4.75)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ePuma)
                        .setRecurrent(true)
                        .setWeight(5.0f)
                        .setBiasDelta(-4.75)
                        .setRangeMatch(EQUALS)
        );

        m.initNeuron(ePuma, 2.0,
                new Input()
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(5.0f)
                        .setBiasDelta(-4.75)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eJaguar)
                        .setRecurrent(true)
                        .setWeight(5.0f)
                        .setBiasDelta(-4.75)
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


        Assert.assertNotNull(eJaguar.get().node.get().getFirstActivation(doc));
        Assert.assertNotNull(ePuma.get().node.get().getFirstActivation(doc));

        Assert.assertEquals(0, eJaguar.get().node.get().getFirstActivation(doc).key.interpretation.primId);
        Assert.assertEquals(1, ePuma.get().node.get().getFirstActivation(doc).key.interpretation.primId);

        Assert.assertEquals(doc.bottom, eJaguar.get().node.get().getFirstActivation(doc).key.interpretation.orInterprNodes.iterator().next());
        Assert.assertEquals(doc.bottom, ePuma.get().node.get().getFirstActivation(doc).key.interpretation.orInterprNodes.iterator().next());

        Assert.assertEquals(1, eJaguar.get().node.get().getFirstActivation(doc).key.interpretation.orInterprNodes.size());
        Assert.assertEquals(1, ePuma.get().node.get().getFirstActivation(doc).key.interpretation.orInterprNodes.size());
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

        m.initNeuron(eJaguar, 5.0,
                new Input()
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(chCatsWithoutJaguar)
                        .setRecurrent(true)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false)
        );

        m.initNeuron(ePuma, 5.0,
                new Input()
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(chCatsWithoutPuma)
                        .setRecurrent(true)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false)
        );


        m.initNeuron(eLeopard, 5.0,
                new Input()
                        .setNeuron(wLeopard)
                        .setRecurrent(false)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(chCatsWithoutLeopard)
                        .setRecurrent(true)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRangeOutput(false)
                        .setRangeMatch(NONE)
        );

        m.initNeuron(cCats,
                0.0,
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(10.0f)
                        .setBiasDelta(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(10.0f)
                        .setBiasDelta(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(10.0f)
                        .setBiasDelta(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        m.initNeuron(chCatsWithoutJaguar, 5.0,
                new Input()
                        .setNeuron(cCats)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(-30.0f)
                        .setBiasDelta(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
        );

        m.initNeuron(chCatsWithoutPuma, 5.0,
                new Input()
                        .setNeuron(cCats)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(-30.0f)
                        .setBiasDelta(0.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
        );

        m.initNeuron(chCatsWithoutLeopard, 5.0,
                new Input()
                        .setNeuron(cCats)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(10.0f)
                        .setBiasDelta(-10.0)
                        .setRecurrent(false)
                        .setRangeMatch(NONE)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(-30.0f)
                        .setBiasDelta(0.0)
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


        Assert.assertNotNull(eJaguar.get().node.get().getFirstActivation(doc));
        Assert.assertNotNull(ePuma.get().node.get().getFirstActivation(doc));


        Assert.assertFalse(eJaguar.getFinalActivations(doc).isEmpty());
        Assert.assertFalse(ePuma.getFinalActivations(doc).isEmpty());
    }


}
