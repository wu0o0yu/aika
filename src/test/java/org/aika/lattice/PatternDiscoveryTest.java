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
package org.aika.lattice;


import org.aika.*;
import org.aika.corpus.Document;
import org.aika.corpus.Document.DiscoveryConfig;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.network.TestHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternDiscoveryTest {


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


    public void count(Document doc, Node n) {
        Node.ThreadState<?, NodeActivation<?>> ts = n.getThreadState(doc.threadId, false);
        if (ts == null) return;

        NodeStatistic stat = ((NodeStatistic) n.statistic);
        stat.frequency += ts.activations.size();
    }


    public void resetFrequency(Model m) {
        for (Provider<? extends AbstractNode> p : m.activeProviders.values()) {
            if (p != null && p.get() instanceof Node) {
                ((NodeStatistic) ((Node) p.get()).statistic).frequency = 0;
            }
        }
    }


    private boolean checkRidRange(Node n) {
        if(n instanceof InputNode) return true;
        AndNode an = (AndNode) n;

        int maxRid = 0;
        for(AndNode.Refinement ref: an.parents.keySet()) {
            if(ref.rid != null) {
                maxRid = Math.max(maxRid, ref.rid);
            }
        }
        return maxRid < 1;
    }


    @Test
    public void discoverPatterns() {
        Model m = new Model();
        m.setNodeStatisticFactory(() -> new NodeStatistic());

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        InputNode pANode = TestHelper.addOutputNode(doc, inA, 0, null, false);
        InputNode pBNode = TestHelper.addOutputNode(doc, inB, 0, null, false);
        InputNode pCNode = TestHelper.addOutputNode(doc, inC, 0, null, false);
        InputNode pDNode = TestHelper.addOutputNode(doc, inD, 0, null, false);


        DiscoveryConfig discoveryConfig = new DiscoveryConfig()
                .setCounter((d, n) -> count(d, n))
                .setCheckExpandable(n -> ((NodeStatistic) n.statistic).frequency >= 1)
                .setCheckValidPattern(n -> checkRidRange(n));

        doc.bestInterpretation = Arrays.asList(doc.bottom);
        doc.discoverPatterns(discoveryConfig);

        inA.addInput(doc, 0, 1, 0);

        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(1, ((NodeStatistic) pANode.statistic).frequency, 0.01);
        Assert.assertEquals(null, pANode.andChildren);


        inB.addInput(doc, 0, 1, 0);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(1, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
//        Assert.assertEquals(0, pBNode.andChildren.size());


        inB.addInput(doc, 2, 3, 1);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(2, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(1, pBNode.andChildren.size());


        inA.addInput(doc, 2, 3, 1);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(1, pANode.andChildren.size());
        Assert.assertEquals(1, pBNode.andChildren.size());

        Assert.assertEquals(2, ((NodeStatistic) pBNode.statistic).frequency, 0.01);

        AndNode pAB = pANode.andChildren.get(new Refinement(0, pBNode.provider)).get();
        Assert.assertEquals(pAB.provider, pBNode.andChildren.get(new Refinement(0, pANode.provider)));

        Assert.assertEquals(1, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAB.parents.size());
        Assert.assertEquals(pANode.provider, pAB.parents.get(new Refinement(0, pBNode.provider)));
        Assert.assertEquals(pBNode.provider, pAB.parents.get(new Refinement(0, pANode.provider)));


        inC.addInput(doc, 4, 5, 2);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(1, ((NodeStatistic) pCNode.statistic).frequency, 0.01);
        Assert.assertEquals(null, pCNode.andChildren);

        Assert.assertEquals(2, pAB.parents.size());


        inB.addInput(doc, 4, 5, 2);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(3, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());


        inB.addInput(doc, 6, 7, 3);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(4, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());

        inC.addInput(doc, 6, 7, 3);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(4, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, ((NodeStatistic) pCNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(1, pCNode.andChildren.size());

        AndNode pBC = pBNode.andChildren.get(new Refinement(0,pCNode.provider)).get();
        Assert.assertEquals(pBC.provider, pCNode.andChildren.get(new Refinement(0, pBNode.provider)));

        Assert.assertEquals(1, ((NodeStatistic) pBC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBC.parents.size());
        Assert.assertEquals(pBNode.provider, pBC.parents.get(new Refinement(0, pCNode.provider)));
        Assert.assertEquals(pCNode.provider, pBC.parents.get(new Refinement(0, pBNode.provider)));


        inA.addInput(doc, 4, 5, 2);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(3, ((NodeStatistic) pANode.statistic).frequency, 0.01);
        Assert.assertEquals(2, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());


        inA.addInput(doc, 8, 9, 4);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(4, ((NodeStatistic) pANode.statistic).frequency, 0.01);
        Assert.assertEquals(2, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

        inC.addInput(doc, 8, 9, 4);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(3, ((NodeStatistic) pCNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(1, ((NodeStatistic) pBC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

        AndNode pAC = pCNode.andChildren.get(new Refinement(0, pANode.provider)).get();
        Assert.assertEquals(pAC.provider, pANode.andChildren.get(new Refinement(0, pCNode.provider)));

        Assert.assertEquals(1, ((NodeStatistic) pAC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAC.parents.size());
        Assert.assertEquals(pANode.provider, pAC.parents.get(new Refinement(0, pCNode.provider)));
        Assert.assertEquals(pCNode.provider, pAC.parents.get(new Refinement(0, pANode.provider)));


        Assert.assertEquals(3, ((NodeStatistic) pCNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(1, ((NodeStatistic) pBC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

//        Assert.assertEquals(1, pAB.andChildren.size());
//        Assert.assertEquals(1, pAC.andChildren.size());
//        Assert.assertEquals(1, pBC.andChildren.size());

        Assert.assertEquals(1, ((NodeStatistic) pAC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAC.parents.size());
        Assert.assertEquals(pANode.provider, pAC.parents.get(new Refinement(0, pCNode.provider)));
        Assert.assertEquals(pCNode.provider, pAC.parents.get(new Refinement(0, pANode.provider)));


        inB.addInput(doc, 8, 9, 4);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(5, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());
        Assert.assertEquals(1, pAB.andChildren.size());
        Assert.assertEquals(3, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(1, pBC.andChildren.size());
        Assert.assertEquals(1, pAC.andChildren.size());

        AndNode pABC = pAB.andChildren.get(new Refinement(0, pCNode.provider)).get();
        Assert.assertEquals(pABC.provider, pAC.andChildren.get(new Refinement(0, pBNode.provider)));
        Assert.assertEquals(pABC.provider, pBC.andChildren.get(new Refinement(0, pANode.provider)));

//        Assert.assertEquals(1, pABC.frequency, 0.01);
        Assert.assertEquals(3, pABC.parents.size());
        Assert.assertEquals(pAB.provider, pABC.parents.get(new Refinement(0, pCNode.provider)));
        Assert.assertEquals(pAC.provider, pABC.parents.get(new Refinement(0, pBNode.provider)));
        Assert.assertEquals(pBC.provider, pABC.parents.get(new Refinement(0, pANode.provider)));


        inD.addInput(doc, 0, 1, 0);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        inD.addInput(doc, 4, 5, 2);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        inA.addInput(doc, 10, 11, 5);
        inB.addInput(doc, 10, 11, 5);
        inC.addInput(doc, 10, 11, 5);
        inD.addInput(doc, 10, 11, 5);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        Assert.assertEquals(3, pBNode.andChildren.size());
        Assert.assertEquals(3, pDNode.andChildren.size());

        AndNode pAD = pANode.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pAD.provider, pDNode.andChildren.get(new Refinement(0, pANode.provider)));

        AndNode pBD = pBNode.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pBD.provider, pDNode.andChildren.get(new Refinement(0, pBNode.provider)));

        Assert.assertEquals(2, ((NodeStatistic) pAD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, ((NodeStatistic) pBD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBD.parents.size());

        AndNode pABD = pAB.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pABD.provider, pAD.andChildren.get(new Refinement(0, pBNode.provider)));
        Assert.assertEquals(pABD.provider, pBD.andChildren.get(new Refinement(0, pANode.provider)));

//        Assert.assertEquals(2, ((NodeStatistic) pABD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());
        Assert.assertEquals(pAB.provider, pABD.parents.get(new Refinement(0, pDNode.provider)));
        Assert.assertEquals(pAD.provider, pABD.parents.get(new Refinement(0, pBNode.provider)));
        Assert.assertEquals(pBD.provider, pABD.parents.get(new Refinement(0, pANode.provider)));

        Assert.assertEquals(1, pABC.andChildren.size());
        Assert.assertEquals(1, pABD.andChildren.size());



        Assert.assertEquals(3, pBNode.andChildren.size());
        Assert.assertEquals(3, pDNode.andChildren.size());

        pAD = pANode.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pAD.provider, pDNode.andChildren.get(new Refinement(0, pANode.provider)));

        pBD = pBNode.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pBD.provider, pDNode.andChildren.get(new Refinement(0, pBNode.provider)));

        Assert.assertEquals(2, ((NodeStatistic) pAD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, ((NodeStatistic) pBD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBD.parents.size());

        pABD = pAB.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pABD.provider, pAD.andChildren.get(new Refinement(0, pBNode.provider)));
        Assert.assertEquals(pABD.provider, pBD.andChildren.get(new Refinement(0, pANode.provider)));

        Assert.assertEquals(1, ((NodeStatistic) pABD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());
        Assert.assertEquals(pAB.provider, pABD.parents.get(new Refinement(0, pDNode.provider)));
        Assert.assertEquals(pAD.provider, pABD.parents.get(new Refinement(0, pBNode.provider)));
        Assert.assertEquals(pBD.provider, pABD.parents.get(new Refinement(0, pANode.provider)));

        Assert.assertEquals(1, pABC.andChildren.size());
        Assert.assertEquals(1, pABD.andChildren.size());



        inD.addInput(doc, 8, 9, 4);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);
        resetFrequency(m);
        doc.discoverPatterns(discoveryConfig);

        AndNode pACD = pAC.andChildren.get(new Refinement(0, pDNode.provider)).get();

        Assert.assertEquals(3, ((NodeStatistic) pAD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAD.parents.size());

        Assert.assertEquals(2, ((NodeStatistic) pABD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pABD.parents.size());

        AndNode pCD = pDNode.andChildren.get(new Refinement(0, pCNode.provider)).get();
        Assert.assertEquals(pCD.provider, pCNode.andChildren.get(new Refinement(0, pDNode.provider)));

        Assert.assertEquals(2, ((NodeStatistic) pCD.statistic).frequency, 0.01);

        pACD = pAC.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pACD.provider, pAD.andChildren.get(new Refinement(0, pCNode.provider)));
        Assert.assertEquals(pACD.provider, pCD.andChildren.get(new Refinement(0, pANode.provider)));

        Assert.assertEquals(1, ((NodeStatistic) pACD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pACD.parents.size());

        AndNode pBCD = pBC.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pBCD.provider, pBD.andChildren.get(new Refinement(0, pCNode.provider)));
        Assert.assertEquals(pBCD.provider, pCD.andChildren.get(new Refinement(0, pBNode.provider)));

        Assert.assertEquals(1, ((NodeStatistic) pACD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pBCD.parents.size());

        AndNode pABCD = pABC.andChildren.get(new Refinement(0, pDNode.provider)).get();
        Assert.assertEquals(pABCD.provider, pABD.andChildren.get(new Refinement(0, pCNode.provider)));
        Assert.assertEquals(pABCD.provider, pACD.andChildren.get(new Refinement(0, pBNode.provider)));
        Assert.assertEquals(pABCD.provider, pBCD.andChildren.get(new Refinement(0, pANode.provider)));

// The Pattern ABC has no Activation yet, since it has just been created.
//        Assert.assertEquals(1, pABCD.frequency, 0.01);

        Assert.assertEquals(4, pABCD.parents.size());
        Assert.assertEquals(null, pABCD.andChildren);

        Assert.assertNull(TestHelper.get(doc, pCNode, new Range(0, 1), doc.bottom));

    }
}
