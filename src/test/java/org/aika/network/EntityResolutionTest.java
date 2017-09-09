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
import org.aika.Provider;
import org.aika.corpus.Document;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.Input.RangeRelation.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class EntityResolutionTest {



    @Test
    public void testSimpleERExample() {

        Model m = new Model();

        Provider<INeuron> wJaguar = m.createNeuron("W-Jaguar");
        Provider<INeuron> wPuma = m.createNeuron("W-Puma");

        Provider<INeuron> eJaguar = m.createNeuron("E-Jaguar");
        Provider<INeuron> ePuma = m.createNeuron("E-Puma");


        m.initAndNeuron(eJaguar, 0.9,
                new Input()
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ePuma)
                        .setRecurrent(true)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
        );

        m.initAndNeuron(ePuma, 0.9,
                new Input()
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eJaguar)
                        .setRecurrent(true)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
        );


        Document doc = m.createDocument("jaguar puma ", 0);

        wJaguar.get().addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        wPuma.get().addInput(doc, 7, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        System.out.println("Process");
        doc.process();

        System.out.println(doc.neuronActivationsToString(true, false, true));


        Assert.assertNotNull(eJaguar.get().node.get().getFirstActivation(doc));
        Assert.assertNotNull(ePuma.get().node.get().getFirstActivation(doc));

        Assert.assertEquals(0, eJaguar.get().node.get().getFirstActivation(doc).key.o.primId);
        Assert.assertEquals(1, ePuma.get().node.get().getFirstActivation(doc).key.o.primId);

        Assert.assertEquals(doc.bottom, eJaguar.get().node.get().getFirstActivation(doc).key.o.orInterprNodes.values().iterator().next());
        Assert.assertEquals(doc.bottom, ePuma.get().node.get().getFirstActivation(doc).key.o.orInterprNodes.values().iterator().next());

        Assert.assertEquals(1, eJaguar.get().node.get().getFirstActivation(doc).key.o.orInterprNodes.size());
        Assert.assertEquals(1, ePuma.get().node.get().getFirstActivation(doc).key.o.orInterprNodes.size());
    }




    @Test
    public void testERExampleWithCategories() {

        Model m = new Model();

        Provider<INeuron> wJaguar = m.createNeuron("W-Jaguar");
        Provider<INeuron> wPuma = m.createNeuron("W-Puma");
        Provider<INeuron> wLeopard = m.createNeuron("W-Leopard");

        Provider<INeuron> eJaguar = m.createNeuron("E-Jaguar");
        Provider<INeuron> ePuma = m.createNeuron("E-Puma");
        Provider<INeuron> eLeopard = m.createNeuron("E-Leopard");

        Provider<INeuron> cKatzen = m.createNeuron("C-Katzen");
        Provider<INeuron> chKatzenOhneJaguar = m.createNeuron("CH-Katzen/Jaguar");
        Provider<INeuron> chKatzenOhnePuma = m.createNeuron("CH-Katzen/Puma");
        Provider<INeuron> chKatzenOhneLeopard = m.createNeuron("CH-Katzen/Leopard");

        m.initAndNeuron(eJaguar, 0.9,
                new Input()
                        .setNeuron(wJaguar)
                        .setRecurrent(false)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(chKatzenOhneJaguar)
                        .setRecurrent(true)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(false)
        );

        m.initAndNeuron(ePuma, 0.9,
                new Input()
                        .setNeuron(wPuma)
                        .setRecurrent(false)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(chKatzenOhnePuma)
                        .setRecurrent(true)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(false)
        );


        m.initAndNeuron(eLeopard, 0.9,
                new Input()
                        .setNeuron(wLeopard)
                        .setRecurrent(false)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(chKatzenOhneLeopard)
                        .setRecurrent(true)
                        .setWeight(5.0f)
                        .setMinInput(0.95f)
                        .setRangeOutput(false)
                        .setRangeMatch(EQUALS)
        );

        m.initOrNeuron(cKatzen,
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        m.initAndNeuron(chKatzenOhneJaguar, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(10.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(-30.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
        );

        m.initAndNeuron(chKatzenOhnePuma, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(10.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(-30.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
        );

        m.initAndNeuron(chKatzenOhneLeopard, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(10.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(-30.0f)
                        .setMinInput(0.95f)
                        .setRecurrent(false)
                        .setRangeMatch(EQUALS)
        );




        Document doc = m.createDocument("jaguar puma ", 0);

        wJaguar.get().addInput(doc, 0, 6);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        wPuma.get().addInput(doc, 7, 11);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        System.out.println("Process");
        Document.OPTIMIZE_DEBUG_OUTPUT = true;
        doc.process();

        System.out.println(doc.neuronActivationsToString(true, false, true));


        Assert.assertNotNull(eJaguar.get().node.get().getFirstActivation(doc));
        Assert.assertNotNull(ePuma.get().node.get().getFirstActivation(doc));

        Assert.assertEquals(0, eJaguar.get().node.get().getFirstActivation(doc).key.o.primId);
        Assert.assertEquals(3, ePuma.get().node.get().getFirstActivation(doc).key.o.primId);

        Assert.assertEquals(doc.bottom, eJaguar.get().node.get().getFirstActivation(doc).key.o.orInterprNodes.values().iterator().next());
        Assert.assertEquals(doc.bottom, ePuma.get().node.get().getFirstActivation(doc).key.o.orInterprNodes.values().iterator().next());

        Assert.assertEquals(1, eJaguar.get().node.get().getFirstActivation(doc).key.o.orInterprNodes.size());
        Assert.assertEquals(1, ePuma.get().node.get().getFirstActivation(doc).key.o.orInterprNodes.size());
    }


}
