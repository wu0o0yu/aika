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


import org.aika.*;
import org.aika.corpus.Document;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.training.PatternDiscovery;
import org.aika.training.PatternDiscovery.Config;
import org.aika.lattice.NodeActivation;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static org.aika.corpus.Range.Relation.EQUALS;

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
                        .setNeuron(inA)
                        .setWeight(100.0)
                        .setBias(-47.5)
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
        PatternDiscovery.discover(doc,
                new Config()
                        .setCheckExpandable(n -> false)
                        .setCounter(act -> count(act))
        );
        Assert.assertEquals(6.0, ((NodeStatistic) outA.get().node.get().parents.get(Integer.MIN_VALUE).first().get().statistic).frequency, 0.001);
    }


    public void count(NodeActivation act) {
        NodeStatistic stat = ((NodeStatistic) act.key.node.statistic);

        stat.frequency++;
    }
}
