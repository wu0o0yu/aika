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
package org.aika.corpus;


import org.aika.Iteration;
import org.aika.Input;
import org.aika.Model;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class SimpleWeightsTest {

    @Test
    public void testWeightsOR() {
        Model m = new Model();

        Iteration t = m.startIteration(null, 0);
        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");

        Neuron pC = new Neuron("C");
        m.createOrNeuron(pC,
                new Input()
                        .setOptional(false)
                        .setNeuron(inA)
                        .setWeight(0.3)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setOptional(false)
                        .setNeuron(inB)
                        .setWeight(0.4)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        {
            Document doc = Document.create("aaaaaaaaaa");
            t = m.startIteration(doc, 0);

            inA.addInput(t, 0, 6);

            t.process();

            System.out.println(doc.selectedOptionsToString());
            System.out.println(t.networkStateToString(true, true));

            t.clearActivations();
        }
        {
            Document doc = Document.create("aaaaaaaaaa");
            t = m.startIteration(doc, 0);

            inA.addInput(t, 0, 6);
            inB.addInput(t, 0, 6);

            t.process();

            System.out.println(doc.selectedOptionsToString());
            System.out.println(t.networkStateToString(true, true));

            t.clearActivations();
        }
    }


    @Test
    public void testWeightsAND() {
        Model m = new Model();

        Iteration t = m.startIteration(null, 0);
        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");

        Neuron pC = new Neuron("C");
        m.createAndNeuron(pC,
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(3.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inB)
                        .setWeight(3.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        {
            Document doc = Document.create("aaaaaaaaaa");
            t = m.startIteration(doc, 0);

            inA.addInput(t, 0, 6);
            inB.addInput(t, 0, 6);

            t.process();

            System.out.println(doc.selectedOptionsToString());
            System.out.println(t.networkStateToString(true, true));

            t.clearActivations();
        }
    }
}
