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


import org.aika.neuron.Synapse;
import org.aika.Model;
import org.aika.neuron.Neuron;
import org.aika.corpus.Document;
import org.aika.neuron.INeuron;
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

        Neuron inA = m.createNeuron("A");

        Neuron bN = m.createNeuron("B");
        Neuron cN = Neuron.init(
                m.createNeuron("C"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(bN)
                        .setWeight(5.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        Neuron.init(
                bN,
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(5.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(cN)
                        .setWeight(5.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );


        Document t = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(t, 0, 1);

        System.out.println(t.activationsToString(false, true));

        Assert.assertEquals(1, bN.get().getFirstActivation(t).key.interpretation.orInterpretationNodes.size());
    }
}
