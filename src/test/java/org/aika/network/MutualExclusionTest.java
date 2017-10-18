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
import org.aika.Model;
import org.aika.Neuron;
import org.aika.neuron.Activation;
import org.aika.corpus.Conflicts.Conflict;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.lattice.OrNode;
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
        Neuron inA = m.createNeuron("IN-A");
        Neuron inB = m.createNeuron("IN-B");
        Neuron inC = m.createNeuron("IN-C");

        // Instantiate the suppressing neuron. Its inputs will be added later on.
        Neuron pSuppr = m.createNeuron("SUPPRESS");

        // Create three neurons that might be suppressed by the suppressing neuron.
        Neuron pA = m.initNeuron(
                m.createNeuron("A"),
                3,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0f)
                        .setMaxLowerWeightsSum(0.0f)
                        .setRecurrent(false)
                        .setBiasDelta(1.0),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-10.0f)
                        .setRecurrent(true)
                        .setBiasDelta(1.0)     // This input is negated
        );

        Neuron pB = m.initNeuron(
                m.createNeuron("B"),
                5,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0f)
                        .setMaxLowerWeightsSum(0.0f)
                        .setRecurrent(false)
                        .setBiasDelta(1.0),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-10.0f)
                        .setRecurrent(true)
                        .setBiasDelta(1.0)
        );

        Neuron pC = m.initNeuron(
                m.createNeuron("C"),
                2,
                new Input()
                        .setNeuron(inC)
                        .setWeight(10.0f)
                        .setMaxLowerWeightsSum(0.0f)
                        .setRecurrent(false)
                        .setBiasDelta(1.0),
                new Input()
                        .setNeuron(pSuppr)
                        .setWeight(-10.0f)
                        .setRecurrent(true)
                        .setBiasDelta(1.0)
        );

        // Finally addInput all the inputs to the suppressing neuron.
        m.initNeuron(
                pSuppr,
                -0.001,
                new Input()
                        .setNeuron(pA)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0),
                new Input()
                        .setNeuron(pB)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0),
                new Input()
                        .setNeuron(pC)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
        );

        Neuron outN = m.initNeuron(m.createNeuron("OUT"),
                -0.001,
                new Input()
                        .setNeuron(pB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
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

        System.out.println("Selected Option: " + doc.bestInterpretation);
        System.out.println();

        System.out.println("Show all conflicts with the selected option:");
        for(InterprNode so: doc.bestInterpretation) {
            for(Conflict c: so.conflicts.primary.values()) {
                System.out.println(c.conflict);
            }
        }
        System.out.println();

        System.out.println(doc.neuronActivationsToString(true, false, true));
        doc.clearActivations();
    }
}
