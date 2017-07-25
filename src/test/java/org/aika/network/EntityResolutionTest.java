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
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.RangeMatch;
import org.aika.neuron.Synapse.RangeSignal;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class EntityResolutionTest {



    @Test
    public void testSimpleERExample() {

        Model m = new Model();

        InputNeuron wJaguar = m.createOrLookupInputSignal("W-Jaguar");
        InputNeuron wPuma = m.createOrLookupInputSignal("W-Puma");

        Neuron eJaguar = new Neuron("E-Jaguar");
        Neuron ePuma = new Neuron("E-Puma");


        m.createAndNeuron(eJaguar, 0.9,
                new Input()
                        .setNeuron(wJaguar)
                        .setRangeOutput(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(ePuma)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
        );

        m.createAndNeuron(ePuma, 0.9,
                new Input()
                        .setNeuron(wPuma)
                        .setRangeOutput(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(eJaguar)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
        );


        Document doc = m.createDocument("jaguar puma ", 0);

        wJaguar.addInput(doc, 0, 6);

        System.out.println(doc.networkStateToString(true, true));

        wPuma.addInput(doc, 7, 11);

        System.out.println(doc.networkStateToString(true, true));

        System.out.println("Process");
        doc.process();

        System.out.println(doc.networkStateToString(true, true));


        Assert.assertNotNull(eJaguar.node.getFirstActivation(doc));
        Assert.assertNotNull(ePuma.node.getFirstActivation(doc));

        Assert.assertEquals(0, eJaguar.node.getFirstActivation(doc).key.o.primId);
        Assert.assertEquals(1, ePuma.node.getFirstActivation(doc).key.o.primId);

        Assert.assertEquals(doc.bottom, eJaguar.node.getFirstActivation(doc).key.o.orOptions.values().iterator().next());
        Assert.assertEquals(doc.bottom, ePuma.node.getFirstActivation(doc).key.o.orOptions.values().iterator().next());

        Assert.assertEquals(1, eJaguar.node.getFirstActivation(doc).key.o.orOptions.size());
        Assert.assertEquals(1, ePuma.node.getFirstActivation(doc).key.o.orOptions.size());
    }




    @Test
    public void testERExampleWithCategories() {

        Model m = new Model();

        InputNeuron wJaguar = m.createOrLookupInputSignal("W-Jaguar");
        InputNeuron wPuma = m.createOrLookupInputSignal("W-Puma");
        InputNeuron wLeopard = m.createOrLookupInputSignal("W-Leopard");

        Neuron eJaguar = new Neuron("E-Jaguar");
        Neuron ePuma = new Neuron("E-Puma");
        Neuron eLeopard = new Neuron("E-Leopard");

        Neuron cKatzen = new Neuron("C-Katzen");
        Neuron chKatzenOhneJaguar = new Neuron("CH-Katzen/Jaguar");
        Neuron chKatzenOhnePuma = new Neuron("CH-Katzen/Puma");
        Neuron chKatzenOhneLeopard = new Neuron("CH-Katzen/Leopard");

        m.createAndNeuron(eJaguar, 0.9,
                new Input()
                        .setNeuron(wJaguar)
                        .setRangeOutput(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(chKatzenOhneJaguar)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
        );

        m.createAndNeuron(ePuma, 0.9,
                new Input()
                        .setNeuron(wPuma)
                        .setRangeOutput(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(chKatzenOhnePuma)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
        );


        m.createAndNeuron(eLeopard, 0.9,
                new Input()
                        .setNeuron(wLeopard)
                        .setRangeOutput(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(chKatzenOhneLeopard)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
        );

        m.createOrNeuron(cKatzen,
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setRangeOutput(true)
                        .setRecurrent(false),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setRangeOutput(true)
                        .setRecurrent(false),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setRangeOutput(true)
                        .setRecurrent(false)
        );

        m.createAndNeuron(chKatzenOhneJaguar, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(-30.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
        );

        m.createAndNeuron(chKatzenOhnePuma, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(-30.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
        );

        m.createAndNeuron(chKatzenOhneLeopard, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(-30.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
        );




        Document doc = m.createDocument("jaguar puma ", 0);

        wJaguar.addInput(doc, 0, 6);

        System.out.println(doc.networkStateToString(true, true));

        wPuma.addInput(doc, 7, 11);

        System.out.println(doc.networkStateToString(true, true));

        System.out.println("Process");
        Document.OPTIMIZE_DEBUG_OUTPUT = true;
        doc.process();

        System.out.println(doc.networkStateToString(true, true));


        Assert.assertNotNull(eJaguar.node.getFirstActivation(doc));
        Assert.assertNotNull(ePuma.node.getFirstActivation(doc));

        Assert.assertEquals(0, eJaguar.node.getFirstActivation(doc).key.o.primId);
        Assert.assertEquals(3, ePuma.node.getFirstActivation(doc).key.o.primId);

        Assert.assertEquals(doc.bottom, eJaguar.node.getFirstActivation(doc).key.o.orOptions.values().iterator().next());
        Assert.assertEquals(doc.bottom, ePuma.node.getFirstActivation(doc).key.o.orOptions.values().iterator().next());

        Assert.assertEquals(1, eJaguar.node.getFirstActivation(doc).key.o.orOptions.size());
        Assert.assertEquals(1, ePuma.node.getFirstActivation(doc).key.o.orOptions.size());
    }


}
