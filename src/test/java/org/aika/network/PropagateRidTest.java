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


import org.aika.neuron.*;
import org.aika.Model;
import org.aika.corpus.Document;
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
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");
        Neuron pA = Neuron.init(m.createNeuron("pA"),
                0.1,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setBias(-0.5)
                        .setRecurrent(false)
                        .setRelativeRid(5)
                        .setRangeOutput(true)
        );

        inA.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 1)
                        .setRelationalId(10)
        );

        Assert.assertEquals(5, Selector.get(doc, pA.get(), null, null, null, null, null).key.rid.intValue());

    }
}
