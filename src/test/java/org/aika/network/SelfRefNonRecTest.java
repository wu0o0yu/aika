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
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class SelfRefNonRecTest {


    @Test
    public void testSelfReferencingNonRecurrentSynapses() {

        Model m = new Model();

        InputNeuron inA = m.createOrLookupInputSignal("A");

        Neuron bN = new Neuron("B");
        Neuron cN = m.createOrNeuron(new Neuron("C"),
                new Input()
                        .setNeuron(bN)
                        .setWeight(5.0)
                        .setMaxLowerWeightsSum(0.0)
        );

        m.createOrNeuron(bN,
                new Input()
                        .setNeuron(inA)
                        .setWeight(5.0)
                        .setMaxLowerWeightsSum(0.0),
                new Input()
                        .setNeuron(cN)
                        .setWeight(5.0)
                        .setMaxLowerWeightsSum(0.0)
        );


        Document doc = new Document("aaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);


        Iteration.APPLY_DEBUG_OUTPUT = true;

        inA.addInput(t, 0, 1);

        System.out.println(t.networkStateToString(true, false));

        Assert.assertEquals(1, bN.node.getFirstActivation(t).key.o.orOptions.size());
    }
}
