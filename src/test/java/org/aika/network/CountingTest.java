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
import org.aika.TrainConfig;
import org.aika.corpus.Document;
import org.aika.lattice.AndNode;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.Input.RangeRelation.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class CountingTest {


    @Test
    public void testActivationCounting() {
        Model m = new Model();

        Neuron inA = m.createNeuron("inA");
        Neuron outA = m.initNeuron(m.createNeuron("nA"), 50.0,
                new Input()
                        .setBiasDelta(0.95)
                        .setWeight(100.0f)
                        .setNeuron(inA)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true)
        );



        Document doc = m.createDocument("aaaaaaaaaa", 0);


        inA.addInput(doc, 0, 1);
        inA.addInput(doc, 0, 1);
        inA.addInput(doc, 2, 3);
        inA.addInput(doc, 3, 4);
        inA.addInput(doc, 3, 4);
        inA.addInput(doc, 5, 6);
        inA.addInput(doc, 6, 7);
        inA.addInput(doc, 7, 8);

        doc.process();
        doc.train(new TrainConfig().setCheckExpandable(n -> false));
        Assert.assertEquals(6.0, outA.get().node.get().parents.get(Integer.MIN_VALUE).first().get().frequency, 0.001);
    }
}
