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
import org.aika.Provider;
import org.aika.neuron.Activation;
import org.aika.corpus.Document;
import org.aika.neuron.INeuron;
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

        Provider<INeuron> strongInput = m.createNeuron("Strong Input");

        Provider<INeuron> weakInputA = m.createNeuron("Weak Input A");
        Provider<INeuron> weakInputB = m.createNeuron("Weak Input B");
        Provider<INeuron> weakInputC = m.createNeuron("Weak Input C");

        Provider<INeuron> suppr = m.createNeuron("suppr");

        Provider<INeuron> patternA = m.initAndNeuron(
                m.createNeuron("Pattern A"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0f)
                        .setRecurrent(false)
                        .setMinInput(0.9f)
                        .setMaxLowerWeightsSum(0.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(weakInputA)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0f)
                        .setRecurrent(true)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        Provider<INeuron> patternB = m.initAndNeuron(
                m.createNeuron("Pattern B"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0f)
                        .setRecurrent(false)
                        .setMinInput(0.9f)
                        .setMaxLowerWeightsSum(0.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(weakInputB)
                        .setWeight(1.5f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0f)
                        .setRecurrent(true)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        Provider<INeuron> patternC = m.initAndNeuron(
                m.createNeuron("Pattern C"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0f)
                        .setRecurrent(false)
                        .setMinInput(0.9f)
                        .setMaxLowerWeightsSum(0.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(weakInputC)
                        .setWeight(0.5f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0f)
                        .setRecurrent(true)
                        .setMinInput(1.0f)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );


        m.initOrNeuron(suppr,
                new Input()
                        .setNeuron(patternA)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(patternB)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(patternC)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setRangeMatch(RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("a ");

        strongInput.get().addInput(doc,0,1);

        weakInputB.get().addInput(doc, 0, 1);

        Document.APPLY_DEBUG_OUTPUT = true;
        doc.process();

        System.out.println(doc.neuronActivationsToString(true, false, true));

        Activation act = TestHelper.get(doc, patternA.get().node.get(), null, null);
        Assert.assertTrue(act.finalState.value < 0.5);

        act = TestHelper.get(doc, patternB.get().node.get(), null, null);
        Assert.assertTrue(act.finalState.value > 0.5);

        act = TestHelper.get(doc, patternC.get().node.get(), null, null);
        Assert.assertTrue(act.finalState.value < 0.5);
    }

}
