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
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range.Operator;
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

        Integer pattern;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        {
            Model m = new Model();


            // Create an input neuron and a recurrent neuron for every letter in this example.
            for (char c : new char[]{'a', 'b', 'c', 'd', 'e'}) {
                InputNeuron in = m.createOrLookupInputNeuron(c + "");
                inputNeurons.put(c, in.id);
            }

            // Create a pattern neuron with the recurrent neurons as input. The number that are
            // given in the inputs are the recurrent ids (relativeRid) which specify the relative position
            // of the inputs relative to each other. The following flag specifies whether this relativeRid is
            // relative or absolute.
            pattern = m.createAndNeuron(
                    new Neuron("BCD"),
                    0.4,
                    new Input()
                            .setNeuron(m.neurons.get(inputNeurons.get('b')))
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setRelativeRid(0)
                            .setMinInput(0.9)
                            .setStartRangeMatch(Operator.EQUALS)
                            .setEndRangeMatch(Operator.LESS_THAN)
                            .setStartRangeOutput(true),
                    new Input()
                            .setNeuron(m.neurons.get(inputNeurons.get('c')))
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setRelativeRid(1)
                            .setMinInput(0.9)
                            .setRangeMatch(RangeRelation.CONTAINS),
                    new Input()
                            .setNeuron(m.neurons.get(inputNeurons.get('d')))
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setRelativeRid(2)
                            .setMinInput(0.9)
                            .setStartRangeMatch(Operator.GREATER_THAN)
                            .setEndRangeMatch(Operator.EQUALS)
                            .setEndRangeOutput(true)
            ).id;


            m.write(dos);
            dos.close();
        }

        {
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            Model m = Model.read(new DataInputStream(bais));

            Document doc = m.createDocument("a b c d e ", 0);

            System.out.println(doc.networkStateToString(true, true));

            int wordPos = 0;
            for (int i = 0; i < doc.length(); i++) {
                char c = doc.getContent().charAt(i);
                if (c != ' ') {
                    ((InputNeuron) m.neurons.get(inputNeurons.get(c))).addInput(doc, i, i + 1, wordPos);
                } else  {
                    wordPos++;
                }
                System.out.println(doc.networkStateToString(true, true));
            }

            // Computes the selected option
            doc.process();

            Assert.assertEquals(1, m.neurons.get(pattern).node.getThreadState(doc, true).activations.size());


            System.out.println("Output activation:");
            for (Activation act : m.neurons.get(pattern).node.getActivations(doc)) {
                System.out.println("Text Range: " + act.key.r);
                System.out.println("Option: " + act.key.o);
                System.out.println("Node: " + act.key.n);
                System.out.println("Rid: " + act.key.rid);
//            System.out.println("Activation weight: " + act.finalState.value);
                System.out.println();
            }

            System.out.println("All activations:");
            System.out.println(doc.networkStateToString(true, true));
            System.out.println();

            doc.clearActivations();
        }
    }

}
