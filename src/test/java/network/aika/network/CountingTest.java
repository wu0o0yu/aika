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
import network.aika.Writable;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.training.PatternDiscovery;
import network.aika.training.PatternDiscovery.Config;
import network.aika.lattice.NodeActivation;
import network.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public class CountingTest {


    public static class NodeStatistic implements Writable {
        int frequency = 0;

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(frequency);
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            frequency = in.readInt();
        }
    }


    @Test
    public void testActivationCounting() {
        Model m = new Model();
        m.setNodeStatisticFactory(() -> new NodeStatistic());

        Neuron inA = m.createNeuron("inA");
        Neuron outA = Neuron.init(m.createNeuron("nA"), 50.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(100.0)
                        .setBias(-47.5)
                        .setRangeOutput(true)
        );



        Document doc = m.createDocument("aaaaaaaaaa", 0);


        inA.addInput(doc, 0, 1);
        inA.addInput(doc, 2, 3);
        inA.addInput(doc, 3, 4);
        inA.addInput(doc, 5, 6);
        inA.addInput(doc, 6, 7);
        inA.addInput(doc, 7, 8);

        doc.process();
        PatternDiscovery.discover(doc,
                new Config()
                        .setCandidateCheck((act, secondAct) -> true)
                        .setPatternCheck(map -> true)
                        .setCounter(act -> count(act))
        );
        Assert.assertEquals(6.0, ((NodeStatistic) inA.get().outputNode.get().statistic).frequency, 0.001);
    }


    public void count(NodeActivation act) {
        NodeStatistic stat = ((NodeStatistic) act.node.statistic);

        stat.frequency++;
    }
}
