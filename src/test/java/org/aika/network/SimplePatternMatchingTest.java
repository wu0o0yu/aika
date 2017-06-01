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
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.Activation;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.Iteration.Input;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Lukas Molzberger
 */
public class SimplePatternMatchingTest {

    @Test
    public void testPatternMatching() {
        Model m = new Model();

        // Create a simple text document.
        Document doc = Document.create("a b c d e ");

        Iteration t = m.startIteration(doc, 0);

        Map<Character, InputNeuron> inputNeurons = new HashMap<>();
        Map<Character, Neuron> relNeurons = new HashMap<>();

        // The space neuron will be used as clock signal for the recurrent neurons.
        InputNeuron inSpace = t.createOrLookupInputSignal("SPACE");

        InputNeuron startSignal = t.createOrLookupInputSignal("START-SIGNAL");

        Neuron ctNeuron = t.createCounterNeuron(new Neuron("CTN"),
                inSpace, false,
                startSignal, true,
                false
        );

        // Create an input neuron and a recurrent neuron for every letter in this example.
        for(char c: new char[] {'a', 'b', 'c', 'd', 'e'}) {
            InputNeuron in = t.createOrLookupInputSignal(c + "");
            Neuron rn = t.createRelationalNeuron(
                    new Neuron(c + "-RN"),
                    ctNeuron,
                    in, false
            );

            inputNeurons.put(c, in);
            relNeurons.put(c, rn);
        }

        // Create a pattern neuron with the relational neurons as input. The numbers that are
        // given in the inputs are the recurrent ids (relativeRid) which specify the relative position
        // of the inputs relative to each other. The following flag specifies whether this relativeRid
        // is relative or absolute.
        Neuron pattern = t.createAndNeuron(
                new Neuron("BCD"),
                0.4,
                new Input()
                        .setNeuron(relNeurons.get('b'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(0.9)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(relNeurons.get('c'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(1)
                        .setMinInput(0.9)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(relNeurons.get('d'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(2)
                        .setMinInput(0.9)
                        .setMatchRange(false)
        );


        startSignal.addInput(t, 0, 1, 0);  // iteration, begin, end, relational id

        // First add the space seperators
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                inSpace.addInput(t, i, i + 1);
            }
        }

        // Then add the characters
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ') {
                inputNeurons.get(c).addInput(t, i, i + 1);
            }
        }

        // Computes the selected option
        t.process();

        Assert.assertEquals(1, pattern.node.getThreadState(t).activations.size());


        System.out.println("Output activation:");
        for(Activation act: pattern.node.getActivations(t)) {
            System.out.println("Text Range: " + act.key.r);
            System.out.println("Option: " + act.key.o);
            System.out.println("Node: " + act.key.n);
            System.out.println("Rid: " + act.key.rid);
            System.out.println();
        }

        System.out.println("All activations:");
        System.out.println(t.networkStateToString(true, true));
        System.out.println();


        t.train();

        t.clearActivations();
    }
}
