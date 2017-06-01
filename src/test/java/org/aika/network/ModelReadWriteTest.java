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
import org.aika.Iteration.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Lukas Molzberger
 */
public class ModelReadWriteTest {

    @Test
    public void testPatternMatching() throws IOException {

        Map<Character, Integer> inputNeurons = new HashMap<>();
        Map<Character, Integer> recurrentNeurons = new HashMap<>();

        Integer startSignal;
        Integer pattern;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        {
            Model m = new Model();

            // Now, feed the inputs into the network.
            Iteration t = m.startIteration(null, 0);


            // The space neuron will be used as clock signal for the recurrent neurons.
            InputNeuron inSpace = t.createOrLookupInputSignal("SPACE");
            inputNeurons.put(' ', inSpace.id);

            startSignal = t.createOrLookupInputSignal("START-SIGNAL").id;

            Neuron ctNeuron = t.createCounterNeuron(new Neuron("CTN"),
                    inSpace, false,
                    m.neurons.get(startSignal), true,
                    false
            );

            // Create an input neuron and a recurrent neuron for every letter in this example.
            for (char c : new char[]{'a', 'b', 'c', 'd', 'e'}) {
                InputNeuron in = t.createOrLookupInputSignal(c + "");
                Neuron rn = t.createRelationalNeuron(
                        new Neuron(c + "-RN"),
                        ctNeuron,
                        in, false
                );

                inputNeurons.put(c, in.id);
                recurrentNeurons.put(c, rn.id);
            }

            // Create a pattern neuron with the recurrent neurons as input. The number that are
            // given in the inputs are the recurrent ids (relativeRid) which specify the relative position
            // of the inputs relative to each other. The following flag specifies whether this relativeRid is
            // relative or absolute.
            pattern = t.createAndNeuron(
                    new Neuron("BCD"),
                    0.4,
                    new Input()
                            .setNeuron(m.neurons.get(recurrentNeurons.get('b')))
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setRelativeRid(0)
                            .setMinInput(0.9)
                            .setMatchRange(false),
                    new Input()
                            .setNeuron(m.neurons.get(recurrentNeurons.get('c')))
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setRelativeRid(1)
                            .setMinInput(0.9)
                            .setMatchRange(false),
                    new Input()
                            .setNeuron(m.neurons.get(recurrentNeurons.get('d')))
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setRelativeRid(2)
                            .setMinInput(0.9)
                            .setMatchRange(false)
            ).id;


            m.write(dos);
            dos.close();
        }

        {
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            Model m = Model.read(new DataInputStream(bais));

            Document doc = Document.create("a b c d e ");
            Iteration t = m.startIteration(doc, 0);

            ((InputNeuron) m.neurons.get(startSignal)).addInput(t, 0, 1, 0);

            System.out.println(t.networkStateToString(true, true));

            for (int i = 0; i < doc.length(); i++) {
//            Iteration.APPLY_DEBUG_OUTPUT = true;
                char c = doc.getContent().charAt(i);
                if (c == ' ')
                    ((InputNeuron) m.neurons.get(inputNeurons.get(c))).addInput(t, i, i + 1);

                System.out.println(t.networkStateToString(true, true));
            }

            for (int i = 0; i < doc.length(); i++) {
//            Iteration.APPLY_DEBUG_OUTPUT = true;
                char c = doc.getContent().charAt(i);
                if (c != ' ') {
                    ((InputNeuron) m.neurons.get(inputNeurons.get(c))).addInput(t, i, i + 1);
                }
                System.out.println(t.networkStateToString(true, true));
            }

            // Computes the selected option
            t.process();

            Assert.assertEquals(1, m.neurons.get(pattern).node.getThreadState(t).activations.size());


            System.out.println("Output activation:");
            for (Activation act : m.neurons.get(pattern).node.getActivations(t)) {
                System.out.println("Text Range: " + act.key.r);
                System.out.println("Option: " + act.key.o);
                System.out.println("Node: " + act.key.n);
                System.out.println("Rid: " + act.key.rid);
//            System.out.println("Activation weight: " + act.finalState.value);
                System.out.println();
            }

            System.out.println("All activations:");
            System.out.println(t.networkStateToString(true, true));
            System.out.println();

            t.clearActivations();
        }
    }

}
