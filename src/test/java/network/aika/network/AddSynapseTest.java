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


import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import java.util.TreeMap;
import java.util.stream.Collectors;

import static network.aika.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
import static network.aika.neuron.INeuron.Type.INHIBITORY;
import static network.aika.neuron.INeuron.Type.INPUT;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class AddSynapseTest {

    @Test
    public void testAddSynapse() {
        Model m = new Model();

        Neuron n = m.createNeuron("OR", INHIBITORY, RECTIFIED_HYPERBOLIC_TANGENT);

        TreeMap<String, Neuron> inputNeurons = new TreeMap<>();

        int i = 0;
        for(String l: new String[] {"A", "B", "C", "D"}) {
            Neuron in = m.createNeuron(l, INPUT);
            inputNeurons.put(l, in);
            int synId = i++;
            Neuron.init(n,
                    new Synapse.Builder()
                            .setSynapseId(synId)
                            .setNeuron(in)
                            .setWeight(10.0),
                    new Relation.Builder()
                            .setFrom(synId)
                            .setTo(OUTPUT)
                            .setRelation(EQUALS)
            );
        }

        Document doc = new Document(m, "                   ");

        i = 0;
        for(Neuron in: inputNeurons.values()) {
            in.addInput(doc,
                    new Activation.Builder()
                            .setRange(i * 2, (i * 2) + 1)
            );

            i++;
        }

        System.out.println(doc.activationsToString());

        doc.process();

        System.out.println(doc.activationsToString());

        Assert.assertEquals(4, n.getActivations(doc, true).collect(Collectors.toList()).size());
    }
}
