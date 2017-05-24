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
import org.aika.Model;
import org.aika.Iteration.Input;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.lattice.AndNode;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class WeightsTest {


    InputNeuron inAA;
    InputNeuron inBA;
    InputNeuron inCA;
    InputNeuron inAB;
    InputNeuron inBB;
    InputNeuron inCB;

    Neuron pDA;
    Neuron pDB;


    @Test
    public void testAndWithMultipleIO() {
        Model m = new Model();
        Iteration t = m.startIteration(null, 0);
        AndNode.minFrequency = 5;

        Neuron pSuppr = new Neuron("SUPPR");

        inAA = t.createOrLookupInputSignal("AA");
        inBA = t.createOrLookupInputSignal("BA");
        inCA = t.createOrLookupInputSignal("CA");

        Neuron pOrA = new Neuron("pOrA");
        t.createOrNeuron(pOrA,
                new Input()
                        .setNeuron(inAA)
                        .setWeight(3.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inBA)
                        .setWeight(4.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        pDA = new Neuron("DA");

        t.createAndNeuron(pDA,
                0.001,
                new Input()
                        .setNeuron(pOrA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(0.6),
                new Input()
                        .setOptional(true)
                        .setNeuron(inCA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-2.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
        );


        inAB = t.createOrLookupInputSignal("AB");
        inBB = t.createOrLookupInputSignal("BB");
        inCB = t.createOrLookupInputSignal("CB");

        Neuron pOrB = new Neuron("pOrB");
        t.createOrNeuron(pOrB,
                new Input()
                        .setNeuron(inAB)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(inBB)
                        .setWeight(5.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        pDB = new Neuron("DB");
        t.createAndNeuron(pDB,
                0.001,
                new Input()
                        .setNeuron(pOrB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(0.6),
                new Input()
                        .setNeuron(inCB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-2.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
        );


        t.createOrNeuron(pSuppr,
                new Input()
                        .setNeuron(pDA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(pDB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        testVariant(m, 9); // 17

        for (int i = 0; i < 32; i++) {
            System.out.println("Variant:" + i);
            testVariant(m, i);
        }
    }


    private void testVariant(Model m, int i) {
        Document doc = Document.create("aaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        if(getBit(i, 0)) {
            inAA.addInput(t, 0, 6);
        }

        if(getBit(i, 1)) {
            inBA.addInput(t, 0, 6);
        }

        if(getBit(i, 2)) {
            inCA.addInput(t, 0, 6);
        }


        if(getBit(i, 3)) {
            inAB.addInput(t, 0, 6);
        }

        if(getBit(i, 4)) {
            inBB.addInput(t, 0, 6);
        }

        inCB.addInput(t, 0, 6);

        t.process();

        t.clearActivations();
    }


    private boolean getBit(int i, int pos) {
        return ((i >> pos) & 1) > 0;
    }

}
