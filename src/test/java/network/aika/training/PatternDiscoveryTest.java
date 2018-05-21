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
package network.aika.training;


import network.aika.*;
import network.aika.lattice.AndNode;
import network.aika.lattice.InputNode;
import network.aika.neuron.Neuron;
import network.aika.Document;
import network.aika.lattice.Node;
import network.aika.lattice.NodeActivation;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.relation.RangeRelation;
import network.aika.training.PatternDiscovery.Config;
import network.aika.neuron.activation.Range;
import org.junit.Assert;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;

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


    public void count(NodeActivation act) {
        NodeStatistic stat = ((NodeStatistic) act.node.statistic);
        stat.frequency += 1; //ts.activations.size();
    }


    public void resetFrequency(Model m) {
        synchronized (m.activeProviders) {
            for (Provider<? extends AbstractNode> p : m.activeProviders.values()) {
                if (p != null && p.get() instanceof Node) {
                    ((NodeStatistic) ((Node) p.get()).statistic).frequency = 0;
                }
            }
        }
    }


    @Test
    public void testLevel2() {
        Model m = new Model();
        m.setNodeStatisticFactory(() -> new NodeStatistic());

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        {
            Document doc = m.createDocument("ab", 0);

            inA.addInput(doc, 0, 1);
            inB.addInput(doc, 1, 2);

            Config config = new Config()
                    .setCounter(act -> count(act))
                    .setCandidateCheck((act, secondAct) -> true)
                    .setRefinementCheck(map -> true);
            PatternDiscovery.discover(doc, config);

            AndNode an = inA.get().outputNode.get().andChildren.firstEntry().getValue().child.get();
            Assert.assertNotNull(an);

            doc.clearActivations();
        }

        {
            Document doc = m.createDocument("ab", 0);

            Activation actA = inA.addInput(doc, 0, 1);
            Activation actB = inB.addInput(doc, 1, 2);

            AndNode.AndActivation andAct = actA.outputToInputNode.output.outputsToAndNode.firstEntry().getValue().output;
            Assert.assertNotNull(andAct);
        }
    }


    @Test
    public void testLevel3Linear() {
        Model m = new Model();
        m.setNodeStatisticFactory(() -> new NodeStatistic());

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");

        {
            Document doc = m.createDocument("abc", 0);

            inA.addInput(doc, 0, 1);
            inB.addInput(doc, 1, 2);
            inC.addInput(doc, 2, 3);

            Config config = new Config()
                    .setCounter(act -> {})
                    .setCandidateCheck((act, secondAct) -> true)
                    .setRefinementCheck(map -> true);
            PatternDiscovery.discover(doc, config);

            doc.clearActivations();
        }
        {
            Document doc = m.createDocument("abc", 0);

            inA.addInput(doc, 0, 1);
            inB.addInput(doc, 1, 2);
            inC.addInput(doc, 2, 3);

            Config config = new Config()
                    .setCounter(act -> {})
                    .setCandidateCheck((act, secondAct) -> true)
                    .setRefinementCheck(map -> true);
            PatternDiscovery.discover(doc, config);

            AndNode an = inA.get().outputNode.get().andChildren.firstEntry().getValue().child.get();
            Assert.assertNotNull(an);

            doc.clearActivations();
        }

        {
            Document doc = m.createDocument("abc", 0);

            Activation actA = inA.addInput(doc, 0, 1);
            Activation actB = inB.addInput(doc, 1, 2);
            Activation actC = inC.addInput(doc, 2, 3);

            AndNode.AndActivation and2Act = actA.outputToInputNode.output.outputsToAndNode.firstEntry().getValue().output;
            AndNode.AndActivation and3Act = and2Act.outputsToAndNode.firstEntry().getValue().output;
            Assert.assertNotNull(and3Act);
        }
    }

    @Test
    public void testLevelNLinear() {

        for(int n = 3; n < 6; n++) {
            Model m = new Model();
            m.setNodeStatisticFactory(() -> new NodeStatistic());

            Neuron[] in = new Neuron[n];
            for (int i = 0; i < n; i++) {
                in[i] = m.createNeuron((char)('A' + i) + "");
            }

            for (int i = 0; i < n; i++) {
                Document doc = m.createDocument("abcdefgh", 0);

                for (int j = 0; j < n; j++) {
                    in[j].addInput(doc, 0 + j, 1 + j);
                }

                Config config = new Config()
                        .setCounter(act -> {})
                        .setCandidateCheck((act, secondAct) -> true)
                        .setRefinementCheck(map -> true);
                PatternDiscovery.discover(doc, config);

                Node<?, ?> node = in[0].get().outputNode.get();
                for (int j = 0; j < i; j++) {
                    node = node.andChildren.firstEntry().getValue().child.get();
                }
                Assert.assertNotNull(node);
                doc.clearActivations();
            }

            {
                Document doc = m.createDocument("abcdefgh", 0);

                for (int j = 0; j < n; j++) {
                    in[j].addInput(doc, 0 + j, 1 + j);
                }

                Activation act = in[0].getActivation(doc, new Range(0, 1), false);
                NodeActivation<?> nodeAct = act.outputToInputNode.output;
                for(int j = 0; j < n - 1; j++) {
                    nodeAct = nodeAct.outputsToAndNode.firstEntry().getValue().output;
                }
                Assert.assertNotNull(nodeAct);
            }
        }
    }

/*
    @Test
    public void discoverPatterns() {
        Model m = new Model();
        m.setNodeStatisticFactory(() -> new NodeStatistic());

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        InputNode pANode = inA.get().outputNode.get();
        InputNode pBNode = inB.get().outputNode.get();
        InputNode pCNode = inC.get().outputNode.get();
        InputNode pDNode = inD.get().outputNode.get();


        Config config = new Config()
                .setCounter(act -> count(act))
                .setRefinementFactory((act, secondAct) -> null);

        PatternDiscovery.discover(doc, config);

        inA.addInput(doc, 0, 2);

        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(1, ((NodeStatistic) pANode.statistic).frequency, 0.01);
        Assert.assertEquals(null, pANode.andChildren);


        inB.addInput(doc, 0, 2);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(1, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
//        Assert.assertEquals(0, pBNode.andChildren.size());


        inB.addInput(doc, 2, 4);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(2, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(1, pBNode.andChildren.size());


        inA.addInput(doc, 2, 4);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(1, pANode.andChildren.size());
        Assert.assertEquals(1, pBNode.andChildren.size());

        Assert.assertEquals(2, ((NodeStatistic) pBNode.statistic).frequency, 0.01);

        AndNode pAB = pANode.andChildren.get(getRefinement(pBNode)).child.get();
        Assert.assertEquals(pAB.provider, pBNode.andChildren.get(getRefinement(pANode)));

        Assert.assertEquals(2, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAB.andParents.size());
        Assert.assertEquals(pANode.provider, pAB.andParents.get(getRefinement(pBNode)));
        Assert.assertEquals(pBNode.provider, pAB.andParents.get(getRefinement(pANode)));


        inC.addInput(doc, 4, 6);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(1, ((NodeStatistic) pCNode.statistic).frequency, 0.01);
        Assert.assertEquals(null, pCNode.andChildren);

        Assert.assertEquals(2, pAB.andParents.size());


        inB.addInput(doc, 4, 6);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(3, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());


        inB.addInput(doc, 6, 8);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(4, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());

        inC.addInput(doc, 6, 8);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(4, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, ((NodeStatistic) pCNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(1, pCNode.andChildren.size());

        AndNode pBC = pBNode.andChildren.get(getRefinement(pCNode)).child.get();
        Assert.assertEquals(pBC.provider, pCNode.andChildren.get(getRefinement(pBNode)));

        Assert.assertEquals(2, ((NodeStatistic) pBC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBC.andParents.size());
        Assert.assertEquals(pBNode.provider, pBC.andParents.get(getRefinement(pCNode)));
        Assert.assertEquals(pCNode.provider, pBC.andParents.get(getRefinement(pBNode)));


        inA.addInput(doc, 4, 6);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(3, ((NodeStatistic) pANode.statistic).frequency, 0.01);
        Assert.assertEquals(3, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());


        inA.addInput(doc, 8, 10);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(4, ((NodeStatistic) pANode.statistic).frequency, 0.01);
        Assert.assertEquals(3, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

        inC.addInput(doc, 8, 10);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(3, ((NodeStatistic) pCNode.statistic).frequency, 0.01);
        Assert.assertEquals(3, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(2, ((NodeStatistic) pBC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

        AndNode pAC = pCNode.andChildren.get(getRefinement(pANode)).child.get();
        Assert.assertEquals(pAC.provider, pANode.andChildren.get(getRefinement(pCNode)));

        Assert.assertEquals(2, ((NodeStatistic) pAC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAC.andParents.size());
        Assert.assertEquals(pANode.provider, pAC.andParents.get(getRefinement(pCNode)));
        Assert.assertEquals(pCNode.provider, pAC.andParents.get(getRefinement(pANode)));


        Assert.assertEquals(3, ((NodeStatistic) pCNode.statistic).frequency, 0.01);
        Assert.assertEquals(3, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(2, ((NodeStatistic) pBC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());

//        Assert.assertEquals(1, pAB.andChildren.size());
//        Assert.assertEquals(1, pAC.andChildren.size());
//        Assert.assertEquals(1, pBC.andChildren.size());

        Assert.assertEquals(2, ((NodeStatistic) pAC.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAC.andParents.size());
        Assert.assertEquals(pANode.provider, pAC.andParents.get(getRefinement(pCNode)));
        Assert.assertEquals(pCNode.provider, pAC.andParents.get(getRefinement(pANode)));


        inB.addInput(doc, 8, 10);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(5, ((NodeStatistic) pBNode.statistic).frequency, 0.01);
        Assert.assertEquals(2, pANode.andChildren.size());
        Assert.assertEquals(2, pBNode.andChildren.size());
        Assert.assertEquals(2, pCNode.andChildren.size());
        Assert.assertEquals(1, pAB.andChildren.size());
        Assert.assertEquals(4, ((NodeStatistic) pAB.statistic).frequency, 0.01);
        Assert.assertEquals(1, pBC.andChildren.size());
        Assert.assertEquals(1, pAC.andChildren.size());

        AndNode pABC = pAB.andChildren.get(getRefinement(pCNode)).child.get();
        Assert.assertEquals(pABC.provider, pAC.andChildren.get(getRefinement(pBNode)));
        Assert.assertEquals(pABC.provider, pBC.andChildren.get(getRefinement(pANode)));

//        Assert.assertEquals(1, pABC.frequency, 0.01);
        Assert.assertEquals(3, pABC.andParents.size());
        Assert.assertEquals(pAB.provider, pABC.andParents.get(getRefinement(pCNode)));
        Assert.assertEquals(pAC.provider, pABC.andParents.get(getRefinement(pBNode)));
        Assert.assertEquals(pBC.provider, pABC.andParents.get(getRefinement(pANode)));


        inD.addInput(doc, 0, 2);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        inD.addInput(doc, 4, 6);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        inA.addInput(doc, 10, 12);
        inB.addInput(doc, 10, 12);
        inC.addInput(doc, 10, 12);
        inD.addInput(doc, 10, 12);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        Assert.assertEquals(3, pBNode.andChildren.size());
        Assert.assertEquals(3, pDNode.andChildren.size());

        AndNode pAD = pANode.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pAD.provider, pDNode.andChildren.get(getRefinement(pANode)));

        AndNode pBD = pBNode.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pBD.provider, pDNode.andChildren.get(getRefinement(pBNode)));

        Assert.assertEquals(3, ((NodeStatistic) pAD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAD.andParents.size());

        Assert.assertEquals(3, ((NodeStatistic) pBD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBD.andParents.size());

        AndNode pABD = pAB.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pABD.provider, pAD.andChildren.get(getRefinement(pBNode)));
        Assert.assertEquals(pABD.provider, pBD.andChildren.get(getRefinement(pANode)));

//        Assert.assertEquals(2, ((NodeStatistic) pABD.meta).frequency, 0.01);
        Assert.assertEquals(3, pABD.andParents.size());
        Assert.assertEquals(pAB.provider, pABD.andParents.get(getRefinement(pDNode)));
        Assert.assertEquals(pAD.provider, pABD.andParents.get(getRefinement(pBNode)));
        Assert.assertEquals(pBD.provider, pABD.andParents.get(getRefinement(pANode)));

        Assert.assertEquals(1, pABC.andChildren.size());
        Assert.assertEquals(1, pABD.andChildren.size());



        Assert.assertEquals(3, pBNode.andChildren.size());
        Assert.assertEquals(3, pDNode.andChildren.size());

        pAD = pANode.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pAD.provider, pDNode.andChildren.get(getRefinement(pANode)));

        pBD = pBNode.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pBD.provider, pDNode.andChildren.get(getRefinement(pBNode)));

        Assert.assertEquals(3, ((NodeStatistic) pAD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAD.andParents.size());

        Assert.assertEquals(3, ((NodeStatistic) pBD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pBD.andParents.size());

        pABD = pAB.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pABD.provider, pAD.andChildren.get(getRefinement(pBNode)));
        Assert.assertEquals(pABD.provider, pBD.andChildren.get(getRefinement(pANode)));

        Assert.assertEquals(3, ((NodeStatistic) pABD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pABD.andParents.size());
        Assert.assertEquals(pAB.provider, pABD.andParents.get(getRefinement(pDNode)));
        Assert.assertEquals(pAD.provider, pABD.andParents.get(getRefinement(pBNode)));
        Assert.assertEquals(pBD.provider, pABD.andParents.get(getRefinement(pANode)));

        Assert.assertEquals(1, pABC.andChildren.size());
        Assert.assertEquals(1, pABD.andChildren.size());



        inD.addInput(doc, 8, 10);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);
        resetFrequency(m);
        PatternDiscovery.discover(doc, config);

        AndNode pACD = pAC.andChildren.get(getRefinement(pDNode)).child.get();

        Assert.assertEquals(4, ((NodeStatistic) pAD.statistic).frequency, 0.01);
        Assert.assertEquals(2, pAD.andParents.size());

        Assert.assertEquals(4, ((NodeStatistic) pABD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pABD.andParents.size());

        AndNode pCD = pDNode.andChildren.get(getRefinement(pCNode)).child.get();
        Assert.assertEquals(pCD.provider, pCNode.andChildren.get(getRefinement(pDNode)));

        Assert.assertEquals(3, ((NodeStatistic) pCD.statistic).frequency, 0.01);

        pACD = pAC.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pACD.provider, pAD.andChildren.get(getRefinement(pCNode)));
        Assert.assertEquals(pACD.provider, pCD.andChildren.get(getRefinement(pANode)));

        Assert.assertEquals(3, ((NodeStatistic) pACD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pACD.andParents.size());

        AndNode pBCD = pBC.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pBCD.provider, pBD.andChildren.get(getRefinement(pCNode)));
        Assert.assertEquals(pBCD.provider, pCD.andChildren.get(getRefinement(pBNode)));

        Assert.assertEquals(3, ((NodeStatistic) pACD.statistic).frequency, 0.01);
        Assert.assertEquals(3, pBCD.andParents.size());

        AndNode pABCD = pABC.andChildren.get(getRefinement(pDNode)).child.get();
        Assert.assertEquals(pABCD.provider, pABD.andChildren.get(getRefinement(pCNode)));
        Assert.assertEquals(pABCD.provider, pACD.andChildren.get(getRefinement(pBNode)));
        Assert.assertEquals(pABCD.provider, pBCD.andChildren.get(getRefinement(pANode)));

// The Pattern ABC has no Activation yet, since it has just been created.
//        Assert.assertEquals(1, pABCD.frequency, 0.01);

        Assert.assertEquals(4, pABCD.andParents.size());
        Assert.assertEquals(null, pABCD.andChildren);

//        Assert.assertNull(TestHelper.get(doc, pCNode, new Range(0, 1), doc.bottom));

    }

    private Refinement getRefinement(InputNode pANode) {
        return new Refinement(new AndNode.RelationsMap(), pANode.provider);
    }


    @Test
    public void testSimplePattern() {
        Model m = new Model();
        m.setNodeStatisticFactory(() -> new NodeStatistic());

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");


        InputNode.add(m, inA.get());

        InputNode.add(m, inB.get());

        Config config = new Config()
                .setCounter(act -> count(act))
                .setRefinementFactory((act, act2) -> ((NodeStatistic) act.node.statistic).frequency >= 1);
        {
            Document doc = m.createDocument("ab", 0);

            inA.addInput(doc, 0, 1);
            inB.addInput(doc, 1, 2);

            doc.process();

            PatternDiscovery.discover(doc, config);

            doc.clearActivations();
        }

        {
            Document doc = m.createDocument("ab", 0);

            inA.addInput(doc, 0, 1);
            inB.addInput(doc, 1, 2);

            doc.process();

            System.out.println(doc.activationsToString(true, true, true));

//            Assert.assertNotNull(inA.get().outputNodes.firstEntry().getValue().get().andChildren.firstEntry().getValue().get().getFirstActivation(doc));

            doc.clearActivations();
        }

    }
*/
}
