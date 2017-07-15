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
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Conflicts.Conflict;
import org.aika.corpus.Document;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class MutualExclusionTest {


    /**
     *     |----------------------------|
     *     | ------                     |
     *     -*| &  |                     |
     *  A ---| PA |------\   -------    |
     *       ------       \  |     |    |
     *                     \-| OR  |    |
     *       ------          |     |    |
     *  B ---| &  |----------| All |----|
     *     -*| PB |         -|     |    |
     *     | ------        / -------    |
     *     |----------------------------|
     *                   /              |
     *       ------     /               |
     *  C ---| &  |----/                |
     *     -*| PC |                     |
     *     | ------                     |
     *     |----------------------------|
     */


    @Test
    public void testMutualExclusion() {
        Model m = new Model();

        // Create the input neurons for the network.
        InputNeuron inA = m.createOrLookupInputSignal("IN-A");
        InputNeuron inB = m.createOrLookupInputSignal("IN-B");
        InputNeuron inC = m.createOrLookupInputSignal("IN-C");

        // Instantiate the suppressing neuron. Its inputs will be added later on.
        Neuron pSuppr = new Neuron("SUPPRESS");

        // Create three neurons that might be suppressed by the suppressing neuron.
        Neuron pA = m.createAndNeuron(
                new Neuron("A"),
                0.001,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.5)
                        .setMaxLowerWeightsSum(0.0)
                        .setRecurrent(false)
                        .setMinInput(0.9),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-10.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)     // This input is negated
        );

        Neuron pB = m.createAndNeuron(
                new Neuron("B"),
                0.001,
                new Input()
                        .setNeuron(inB)
                        .setWeight(11.0)
                        .setMaxLowerWeightsSum(0.0)
                        .setRecurrent(false)
                        .setMinInput(0.9),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-10.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
        );

        Neuron pC = m.createAndNeuron(
                new Neuron("C"),
                0.001,
                new Input()
                        .setNeuron(inC)
                        .setWeight(10.0)
                        .setMaxLowerWeightsSum(0.0)
                        .setRecurrent(false)
                        .setMinInput(0.9),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-10.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
        );

        // Finally addInput all the inputs to the suppressing neuron.
        m.createOrNeuron(
                pSuppr,
                new Input()
                        .setNeuron(pA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(pB)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(pC)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        Neuron outN = m.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(pB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
        );

        // Now that the model is complete, apply it to a document.

        Document doc = m.createDocument("foobar", 0);

        // Add input activations starting at char 0 and ending at char 1
        // These inputs will be immediately propagated through the network.
        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);
        inC.addInput(doc, 0, 1);

        // Computes the selected option
        doc.process();

        System.out.println("Selected Option: " + doc.selectedOption);
        System.out.println();

        System.out.println("Show all conflicts with the selected option:");
        for(Conflict c: doc.selectedOption.conflicts.primary.values()) {
            System.out.println(c.conflict);
        }
        System.out.println();

        System.out.println("Output activation:");
        for(Activation act: outN.node.getActivations(doc)) {
            System.out.println("Text Range: " + act.key.r);
            System.out.println("Option: " + act.key.o);
            System.out.println("Node: " + act.key.n);
            System.out.println("Rid: " + act.key.rid);
            System.out.println("Activation weight: " + act.finalState.value);
            System.out.println();
        }

        System.out.println("All activations:");
        System.out.println(doc.networkStateToString(true, true));
        System.out.println();

        System.out.println("Selected activations:");
        System.out.println(doc.networkStateToString(false, true));

        doc.clearActivations();
    }
}
