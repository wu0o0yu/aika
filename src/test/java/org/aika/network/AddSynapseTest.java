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
import org.aika.corpus.Range.Relation;
import org.aika.corpus.Document;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.TreeMap;

/**
 *
 * @author Lukas Molzberger
 */
public class AddSynapseTest {

    @Test
    public void testAddSynapse() {
        Model m = new Model();

        Neuron n = m.createNeuron("OR");

        TreeMap<String, Neuron> inputNeurons = new TreeMap<>();

        for(String l: new String[] {"A", "B", "C", "D"}) {
            Neuron in = m.createNeuron(l);
            inputNeurons.put(l, in);
            m.addSynapse(n,
                    new Input()
                            .setNeuron(in)
                            .setWeight(10.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true)
                            .setRelativeRid(0)
            );
        }

        Document doc = m.createDocument("                   ");

        int i = 0;
        for(Neuron in: inputNeurons.values()) {
            in.addInput(doc, i * 2, (i * 2) + 1, i);

            i++;
        }

        doc.process();

        Assert.assertEquals(4, n.getFinalActivations(doc).size());
    }
}
