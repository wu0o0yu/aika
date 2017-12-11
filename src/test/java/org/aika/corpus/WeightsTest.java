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


import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.lattice.AndNode;
import org.junit.Test;

import static org.aika.Input.RangeRelation.CONTAINED_IN;
import static org.aika.Input.RangeRelation.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class WeightsTest {


    Neuron inAA;
    Neuron inBA;
    Neuron inCA;
    Neuron inAB;
    Neuron inBB;
    Neuron inCB;

    Neuron pDA;
    Neuron pDB;


    @Test
    public void testAndWithMultipleIO() {
        Model m = new Model();

        Neuron pSuppr = m.createNeuron("SUPPR");

        inAA = m.createNeuron("AA");
        inBA = m.createNeuron("BA");
        inCA = m.createNeuron("CA");

        Neuron pOrA = m.createNeuron("pOrA");
        m.initNeuron(pOrA,
                -0.001,
                new Input()
                        .setNeuron(inAA)
                        .setWeight(3.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inBA)
                        .setWeight(4.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        pDA = m.createNeuron("DA");

        m.initNeuron(pDA,
                0.001,
                new Input()
                        .setNeuron(pOrA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(-0.6)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inCA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-2.0f)
                        .setRecurrent(true)
                        .setBiasDelta(-2.0)
                        .setRangeMatch(CONTAINED_IN)
        );


        inAB = m.createNeuron("AB");
        inBB = m.createNeuron("BB");
        inCB = m.createNeuron("CB");

        Neuron pOrB = m.createNeuron("pOrB");
        m.initNeuron(pOrB,
                -0.001,
                new Input()
                        .setNeuron(inAB)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inBB)
                        .setWeight(5.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        pDB = m.createNeuron("DB");
        m.initNeuron(pDB,
                0.001,
                new Input()
                        .setNeuron(pOrB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(-0.6)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inCB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(-1.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-2.0f)
                        .setRecurrent(true)
                        .setBiasDelta(-2.0)
                        .setRangeMatch(CONTAINED_IN)
        );


        m.initNeuron(pSuppr,
                -0.001,
                new Input()
                        .setNeuron(pDA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(pDB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        testVariant(m, 9); // 17

        for (int i = 0; i < 32; i++) {
            System.out.println("Variant:" + i);
            testVariant(m, i);
        }
    }


    private void testVariant(Model m, int i) {
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        if(getBit(i, 0)) {
            inAA.addInput(doc, 0, 6);
        }

        if(getBit(i, 1)) {
            inBA.addInput(doc, 0, 6);
        }

        if(getBit(i, 2)) {
            inCA.addInput(doc, 0, 6);
        }


        if(getBit(i, 3)) {
            inAB.addInput(doc, 0, 6);
        }

        if(getBit(i, 4)) {
            inBB.addInput(doc, 0, 6);
        }

        inCB.addInput(doc, 0, 6);

        doc.process();

        doc.clearActivations();
    }


    private boolean getBit(int i, int pos) {
        return ((i >> pos) & 1) > 0;
    }

}
