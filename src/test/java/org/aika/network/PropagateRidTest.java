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


import org.aika.Iteration;
import org.aika.Model;
import org.aika.Iteration.Input;
import org.aika.corpus.Document;
import org.aika.Activation;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.lattice.AndNode;
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
        Document doc = Document.create("aaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron inA = t.createOrLookupInputSignal("A");
        Neuron pA = t.createAndNeuron(new Neuron("pA"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(0.5)
                        .setRelativeRid(5)
        );

        inA.addInput(t, 0, 1, 10, t.doc.bottom);

        Assert.assertEquals(5, Activation.get(t, pA.node, null, null, null, null, null).key.rid.intValue());

    }
}
