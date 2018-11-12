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
import network.aika.neuron.relation.Relation;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.CONTAINED_IN;
import static network.aika.neuron.relation.Relation.EQUALS;

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
                        .setSynapseId(0)
                        .setNeuron(inAA)
                        .setWeight(3.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inBA)
                        .setWeight(4.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        pDA = m.createNeuron("DA");

        Neuron.init(pDA,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(pOrA)
                        .setWeight(1.0)
                        .setBias(-0.6)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inCA)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(pSuppr)
                        .setWeight(-2.0)
                        .setBias(0.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(CONTAINED_IN),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        inAB = m.createNeuron("AB");
        inBB = m.createNeuron("BB");
        inCB = m.createNeuron("CB");

        Neuron pOrB = m.createNeuron("pOrB");
        Neuron.init(pOrB,
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inAB)
                        .setWeight(2.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inBB)
                        .setWeight(5.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        pDB = m.createNeuron("DB");
        Neuron.init(pDB,
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(pOrB)
                        .setWeight(1.0)
                        .setBias(-0.6)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inCB)
                        .setWeight(1.0)
                        .setBias(-1.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(pSuppr)
                        .setWeight(-2.0)
                        .setBias(0.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(CONTAINED_IN),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        Neuron.init(pSuppr,
                0.0,
                INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(pDA)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(pDB)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
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
