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


import org.aika.Activation;
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.lattice.AndNode;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.AbstractNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author Lukas Molzberger
 */
public class PropagateRidTest {


    @Test
    public void simpleTest() {
        Model m = new Model();
        AndNode.minFrequency = 1;
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        InputNeuron inA = m.createOrLookupInputNeuron("A");
        Neuron pA = m.initAndNeuron(m.createNeuron("pA"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(0.5)
                        .setRelativeRid(5)
        );

        inA.addInput(doc, 0, 1, 10, doc.bottom);

        Assert.assertEquals(5, Activation.get(doc, pA.node.get(), null, null, null, null, null, null).key.rid.intValue());

    }
}
