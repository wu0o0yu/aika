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
package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import org.junit.Test;

import static network.aika.neuron.activation.Range.Relation.CONTAINED_IN;
import static network.aika.neuron.activation.Range.Relation.EQUALS;

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
        Neuron.init(pOrA,
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inAA)
                        .setWeight(3.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inBA)
                        .setWeight(4.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        pDA = m.createNeuron("DA");

        Neuron.init(pDA,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(pOrA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-0.6)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inCA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pSuppr)
                        .setWeight(-2.0)
                        .setRecurrent(true)
                        .setBias(0.0)
                        .setRangeMatch(CONTAINED_IN)
        );


        inAB = m.createNeuron("AB");
        inBB = m.createNeuron("BB");
        inCB = m.createNeuron("CB");

        Neuron pOrB = m.createNeuron("pOrB");
        Neuron.init(pOrB,
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inAB)
                        .setWeight(2.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inBB)
                        .setWeight(5.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );

        pDB = m.createNeuron("DB");
        Neuron.init(pDB,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(pOrB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-0.6)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inCB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pSuppr)
                        .setWeight(-2.0)
                        .setRecurrent(true)
                        .setBias(0.0)
                        .setRangeMatch(CONTAINED_IN)
        );


        Neuron.init(pSuppr,
                0.0,
                INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setNeuron(pDA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(pDB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(0.0)
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
