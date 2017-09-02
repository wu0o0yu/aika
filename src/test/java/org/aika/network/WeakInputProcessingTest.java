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


import org.aika.Input;
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.neuron.Activation;
import org.aika.corpus.Document;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class WeakInputProcessingTest {


    @Test
    public void testWeakInputProcessing() {
        Model m = new Model();

        Neuron strongInput = new Neuron(m, "Strong Input");

        Neuron weakInputA = new Neuron(m, "Weak Input A");
        Neuron weakInputB = new Neuron(m, "Weak Input B");
        Neuron weakInputC = new Neuron(m, "Weak Input C");

        Neuron suppr = new Neuron(m, "suppr");

        Neuron patternA = m.initAndNeuron(
                new Neuron(m, "Pattern A"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setMinInput(0.9)
                        .setMaxLowerWeightsSum(0.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(weakInputA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        Neuron patternB = m.initAndNeuron(
                new Neuron(m, "Pattern B"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setMinInput(0.9)
                        .setMaxLowerWeightsSum(0.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(weakInputB)
                        .setWeight(1.5)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        Neuron patternC = m.initAndNeuron(
                new Neuron(m, "Pattern C"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setMinInput(0.9)
                        .setMaxLowerWeightsSum(0.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(weakInputC)
                        .setWeight(0.5)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );


        m.initOrNeuron(suppr,
                new Input()
                        .setNeuron(patternA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(patternB)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(patternC)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("a ");

        strongInput.addInput(doc,0,1);

        weakInputB.addInput(doc, 0, 1);

        Document.APPLY_DEBUG_OUTPUT = true;
        doc.process();

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Activation act = TestHelper.get(doc, patternA.node.get(), null, null);
        Assert.assertTrue(act.finalState.value < 0.5);

        act = TestHelper.get(doc, patternB.node.get(), null, null);
        Assert.assertTrue(act.finalState.value > 0.5);

        act = TestHelper.get(doc, patternC.node.get(), null, null);
        Assert.assertTrue(act.finalState.value < 0.5);
    }

}
