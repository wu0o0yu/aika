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
import org.aika.Iteration;
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;


/**
 *
 * @author Lukas Molzberger
 */
public class RecurrentIdTest {

    @Test
    public void testABPattern() {
        Model m = new Model();

        InputNeuron cl = m.createOrLookupInputSignal("Clock");
        InputNeuron start = m.createOrLookupInputSignal("Start");
        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");

        Neuron ctNeuron = m.createCounterNeuron(new Neuron("CTN"), cl, false, start, true, false);

        Neuron recA = m.createRelationalNeuron(new Neuron("SA"), ctNeuron, inA, false);
        Neuron recB = m.createRelationalNeuron(new Neuron("SB"), ctNeuron, inB, false);

        Node outCNode = m.createAndNeuron(new Neuron("C", true, true),
                0.001,
                new Input()
                        .setNeuron(recA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0),
                new Input()
                        .setNeuron(recB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(1)
        ).node;


        Document doc = Document.create("aaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNode outANode = TestHelper.addOutputNode(t, recA, 0, null);
        InputNode outBNode = TestHelper.addOutputNode(t, recB, 0, null);


        TestHelper.addActivation(outANode, t, new Activation(outANode, new Range(0, 1), 20, doc.bottom));
        TestHelper.addActivation(outBNode, t, new Activation(outBNode, new Range(0, 1), 21, doc.bottom));

        Activation outC1 = Activation.get(t, outCNode, 20, new Range(0, 1), Range.Relation.OVERLAPS, null, null);

        System.out.println(t.networkStateToString(true, true));

        Assert.assertNotNull(outC1);
    }


    @Test
    public void testABCPattern() {
        Model m = new Model();

        InputNeuron cl = m.createOrLookupInputSignal("Clock");
        InputNeuron start = m.createOrLookupInputSignal("Start");
        InputNeuron inA = m.createOrLookupInputSignal("A");
        InputNeuron inB = m.createOrLookupInputSignal("B");
        InputNeuron inC = m.createOrLookupInputSignal("C");

        Neuron ctNeuron = m.createCounterNeuron(new Neuron("CTN"), cl, false, start, true, false);

        Neuron recA = m.createRelationalNeuron(new Neuron("A"), ctNeuron, inA, false);
        Neuron recB = m.createRelationalNeuron(new Neuron("B"), ctNeuron, inB, false);
        Neuron recC = m.createRelationalNeuron(new Neuron("C"), ctNeuron, inC, false);

        Node outDNode = m.createAndNeuron(new Neuron("D", true, true),
                0.001,
                new Input()
                        .setNeuron(recA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(3),
                new Input()
                        .setNeuron(recB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0),
                new Input()
                        .setNeuron(recC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(6)
        ).node;

        Document doc = Document.create("aaaaaaaaaa");
        Iteration t = m.startIteration(doc, 0);

        InputNode outANode = TestHelper.addOutputNode(t, recA, 0, null);
        InputNode outBNode = TestHelper.addOutputNode(t, recB, 0, null);
        InputNode outCNode = TestHelper.addOutputNode(t, recC, 0, null);

        outANode.inputNeuron = new Neuron();
        outBNode.inputNeuron = new Neuron();
        outCNode.inputNeuron = new Neuron();

        TestHelper.addActivation(outANode, t, new Activation(outANode, new Range(0, 1), 13, doc.bottom));
        TestHelper.addActivation(outBNode, t, new Activation(outBNode, new Range(0, 1), 10, doc.bottom));
        TestHelper.addActivation(outCNode, t, new Activation(outCNode, new Range(0, 1), 16, doc.bottom));

        Activation outD1 = Activation.get(t, outDNode, 10, new Range(0, 1), Range.Relation.OVERLAPS, null, null);

        Assert.assertNotNull(outD1);
    }


    @Test
    public void testHuettenheim() {
        Model m = new Model();

        HashMap<Character, InputNeuron> chars = new HashMap<>();
        for (char c = 'a'; c <= 'z'; c++) {
            InputNeuron rec = m.createOrLookupInputSignal("IN-" + c);
            chars.put(c, rec);
        }

        String[] testWords = {"Hüttenheim"};

        for (String word : testWords) {
            Set<Input> inputs = new TreeSet<>();
            for (int i = 0; i < word.length(); i++) {
                char c = word.toLowerCase().charAt(i);

                InputNeuron rec = chars.get(c);
                if (rec != null) {
                    inputs.add(
                            new Input()
                                .setNeuron(rec)
                                .setWeight(1.0)
                                .setRecurrent(false)
                                .setMinInput(1.0)
                                .setRelativeRid(i)
                    );
                }
            }

            Neuron n = m.createAndNeuron(new Neuron("PATTERN"), 0.5, inputs);

            System.out.println(n.node.logicToString());

            Document doc = Document.create("abc Hüttenheim cba");
            Iteration t = m.startIteration(doc, 0);

            for (int i = 0; i < doc.length(); i++) {
                char c = doc.getContent().toLowerCase().charAt(i);

                InputNeuron rec = chars.get(c);
                if (rec != null) {
                    Range r = new Range(i, doc.length());

                    rec.addInput(t, r.begin, r.end, i, doc.bottom);
                }
            }

            assert n.node.getActivations(t).size() >= 1;
        }
    }
}
