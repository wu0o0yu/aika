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
import org.aika.Iteration.Input;
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

        Document doc = Document.create("a ");
        Iteration t = m.startIteration(doc, 0);


        InputNeuron strongInput = t.createOrLookupInputSignal("Strong Input");

        InputNeuron weakInputA = t.createOrLookupInputSignal("Weak Input A");
        InputNeuron weakInputB = t.createOrLookupInputSignal("Weak Input B");
        InputNeuron weakInputC = t.createOrLookupInputSignal("Weak Input C");

        Neuron suppr = new Neuron("suppr");

        Neuron patternA = t.createAndNeuron(
                new Neuron("Pattern A"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setMinInput(0.9)
                        .setMatchRange(false)
                        .setMaxLowerWeightsSum(0.0),
                new Input()
                        .setNeuron(weakInputA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );

        Neuron patternB = t.createAndNeuron(
                new Neuron("Pattern B"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setMinInput(0.9)
                        .setMatchRange(false)
                        .setMaxLowerWeightsSum(0.0),
                new Input()
                        .setNeuron(weakInputB)
                        .setWeight(1.5)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );

        Neuron patternC = t.createAndNeuron(
                new Neuron("Pattern C"),
                0.4,
                new Input()
                        .setNeuron(strongInput)
                        .setWeight(50.0)
                        .setRecurrent(false)
                        .setMinInput(0.9)
                        .setMatchRange(false)
                        .setMaxLowerWeightsSum(0.0),
                new Input()
                        .setNeuron(weakInputC)
                        .setWeight(0.5)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
                        .setMatchRange(false)
        );


        t.createOrNeuron(suppr,
                new Input()
                        .setNeuron(patternA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(patternB)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(patternC)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setMatchRange(false)
        );

        strongInput.addInput(t,0,1);

        weakInputB.addInput(t, 0, 1);

        Iteration.APPLY_DEBUG_OUTPUT = true;
        t.process();

        System.out.println(t.networkStateToString(true,true));

        Assert.assertTrue(TestHelper.get(t, patternA.node, null, null).finalState.value < 0.5);
        Assert.assertTrue(TestHelper.get(t, patternB.node, null, null).finalState.value > 0.5);
        Assert.assertTrue(TestHelper.get(t, patternC.node, null, null).finalState.value < 0.5);
    }

}
