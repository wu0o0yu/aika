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


import org.aika.Iteration;
import org.aika.Iteration.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
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
        Document doc = new Document("jaguar puma ");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron wJaguar = t.createOrLookupInputSignal("W-Jaguar");
        InputNeuron wPuma = t.createOrLookupInputSignal("W-Puma");

        Neuron eJaguar = new Neuron("E-Jaguar");
        Neuron ePuma = new Neuron("E-Puma");


        t.createAndNeuron(eJaguar, 0.9,
                new Input()
                        .setNeuron(wJaguar)
                        .setMatchRange(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(ePuma)
                        .setMatchRange(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE)
        );

        t.createAndNeuron(ePuma, 0.9,
                new Input()
                        .setNeuron(wPuma)
                        .setMatchRange(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(eJaguar)
                        .setMatchRange(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE)
        );


        wJaguar.addInput(t, 0, 6);

        System.out.println(t.networkStateToString(true, true));

        wPuma.addInput(t, 7, 11);

        System.out.println(t.networkStateToString(true, true));

        System.out.println("Process");
        t.process();

        System.out.println(t.networkStateToString(true, true));


        Assert.assertNotNull(eJaguar.node.getFirstActivation(t));
        Assert.assertNotNull(ePuma.node.getFirstActivation(t));

        Assert.assertEquals(0, eJaguar.node.getFirstActivation(t).key.o.primId);
        Assert.assertEquals(1, ePuma.node.getFirstActivation(t).key.o.primId);

        Assert.assertEquals(doc.bottom, eJaguar.node.getFirstActivation(t).key.o.orOptions.values().iterator().next());
        Assert.assertEquals(doc.bottom, ePuma.node.getFirstActivation(t).key.o.orOptions.values().iterator().next());

        Assert.assertEquals(1, eJaguar.node.getFirstActivation(t).key.o.orOptions.size());
        Assert.assertEquals(1, ePuma.node.getFirstActivation(t).key.o.orOptions.size());
    }




    @Test
    public void testERExampleWithCategories() {

        Model m = new Model();
        Document doc = new Document("jaguar puma ");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron wJaguar = t.createOrLookupInputSignal("W-Jaguar");
        InputNeuron wPuma = t.createOrLookupInputSignal("W-Puma");
        InputNeuron wLeopard = t.createOrLookupInputSignal("W-Leopard");

        Neuron eJaguar = new Neuron("E-Jaguar");
        Neuron ePuma = new Neuron("E-Puma");
        Neuron eLeopard = new Neuron("E-Leopard");

        Neuron cKatzen = new Neuron("C-Katzen");
        Neuron chKatzenOhneJaguar = new Neuron("CH-Katzen/Jaguar");
        Neuron chKatzenOhnePuma = new Neuron("CH-Katzen/Puma");
        Neuron chKatzenOhneLeopard = new Neuron("CH-Katzen/Leopard");

        t.createAndNeuron(eJaguar, 0.9,
                new Input()
                        .setNeuron(wJaguar)
                        .setMatchRange(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(chKatzenOhneJaguar)
                        .setMatchRange(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE)
        );

        t.createAndNeuron(ePuma, 0.9,
                new Input()
                        .setNeuron(wPuma)
                        .setMatchRange(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(chKatzenOhnePuma)
                        .setMatchRange(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE)
        );


        t.createAndNeuron(eLeopard, 0.9,
                new Input()
                        .setNeuron(wLeopard)
                        .setMatchRange(true)
                        .setRecurrent(false)
                        .setWeight(5.0)
                        .setMinInput(0.95),
                new Input()
                        .setNeuron(chKatzenOhneLeopard)
                        .setMatchRange(false)
                        .setRecurrent(true)
                        .setWeight(5.0)
                        .setMinInput(0.95)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE)
        );

        t.createOrNeuron(cKatzen,
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setRecurrent(false)
        );

        t.createAndNeuron(chKatzenOhneJaguar, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(true),
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(false)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE),
                new Input()
                        .setNeuron(eJaguar)
                        .setWeight(-30.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(true)
        );

        t.createAndNeuron(chKatzenOhnePuma, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(true),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(false)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE),
                new Input()
                        .setNeuron(ePuma)
                        .setWeight(-30.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(true)
        );

        t.createAndNeuron(chKatzenOhneLeopard, 0.5,
                new Input()
                        .setNeuron(cKatzen)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(true),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(false)
                        .setStartVisibility(Synapse.RangeVisibility.NONE)
                        .setEndVisibility(Synapse.RangeVisibility.NONE),
                new Input()
                        .setNeuron(eLeopard)
                        .setWeight(-30.0)
                        .setMinInput(0.95)
                        .setRecurrent(false)
                        .setMatchRange(true)
        );





        wJaguar.addInput(t, 0, 6);

        System.out.println(t.networkStateToString(true, true));

        wPuma.addInput(t, 7, 11);

        System.out.println(t.networkStateToString(true, true));

        System.out.println("Process");
        Iteration.OPTIMIZE_DEBUG_OUTPUT = true;
        t.process();

        System.out.println(t.networkStateToString(true, true));


        Assert.assertNotNull(eJaguar.node.getFirstActivation(t));
        Assert.assertNotNull(ePuma.node.getFirstActivation(t));

        Assert.assertEquals(0, eJaguar.node.getFirstActivation(t).key.o.primId);
        Assert.assertEquals(3, ePuma.node.getFirstActivation(t).key.o.primId);

        Assert.assertEquals(doc.bottom, eJaguar.node.getFirstActivation(t).key.o.orOptions.values().iterator().next());
        Assert.assertEquals(doc.bottom, ePuma.node.getFirstActivation(t).key.o.orOptions.values().iterator().next());

        Assert.assertEquals(1, eJaguar.node.getFirstActivation(t).key.o.orOptions.size());
        Assert.assertEquals(1, ePuma.node.getFirstActivation(t).key.o.orOptions.size());
    }


}
