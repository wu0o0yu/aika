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
import org.aika.corpus.Document;
import org.aika.neuron.InputNeuron;
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

        InputNeuron strongInput = m.createOrLookupInputNeuron("Strong Input");

        InputNeuron weakInputA = m.createOrLookupInputNeuron("Weak Input A");
        InputNeuron weakInputB = m.createOrLookupInputNeuron("Weak Input B");
        InputNeuron weakInputC = m.createOrLookupInputNeuron("Weak Input C");

        Neuron suppr = m.createNeuron("suppr");

        Neuron patternA = m.initAndNeuron(
                m.createNeuron("Pattern A"),
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
                m.createNeuron("Pattern B"),
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
                m.createNeuron("Pattern C"),
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

        System.out.println(doc.networkStateToString(true,true, false, true));

        Assert.assertTrue(TestHelper.get(doc, patternA.node.get(), null, null).finalState.value < 0.5);
        Assert.assertTrue(TestHelper.get(doc, patternB.node.get(), null, null).finalState.value > 0.5);
        Assert.assertTrue(TestHelper.get(doc, patternC.node.get(), null, null).finalState.value < 0.5);
    }

}
