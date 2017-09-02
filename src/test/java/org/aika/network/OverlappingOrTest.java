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


import org.aika.lattice.NodeActivation;
import org.aika.Input;
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range.Operator;
import org.aika.lattice.OrNode;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Lukas Molzberger
 */
public class OverlappingOrTest {


    @Test
    public void testOverlappingOr() {
        Model m = new Model();

        Map<Character, Neuron> inputNeurons = new HashMap<>();
        Map<Character, Neuron> relNeurons = new HashMap<>();

        // The space neuron will be used as clock signal for the recurrent neurons.
        Neuron inSpace = new Neuron(m, "SPACE");
        inputNeurons.put(' ', inSpace);

        Neuron startSignal = new Neuron(m, "START-SIGNAL");

        Neuron ctNeuron = m.initCounterNeuron(new Neuron(m, "CTN"),
                inSpace, false,
                startSignal, true,
                false
        );

        // Create an input neuron and a recurrent neuron for every letter in this example.
        for(char c: new char[] {'a', 'b', 'c', 'd', 'e'}) {
            Neuron in = new Neuron(m, c + "");
            Neuron rn = m.initRelationalNeuron(
                    new Neuron(m, c + "-RN"),
                    ctNeuron,
                    in, false
            );

            inputNeurons.put(c, in);
            relNeurons.put(c, rn);
        }

        // Create a pattern neuron with the recurrent neurons as input. The number that are
        // given in the inputs are the recurrent ids (relativeRid) which specify the relative position
        // of the inputs relative to each other. The following flag specifies whether this relativeRid is
        // relative or absolute.
        Neuron pattern = m.initAndNeuron(
                new Neuron(m, "BCD"),
                0.4,
                new Input()
                        .setNeuron(relNeurons.get('b'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setMinInput(0.5)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setEndRangeMatch(Operator.LESS_THAN)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(relNeurons.get('c'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(1)
                        .setMinInput(0.5)
                        .setRangeMatch(RangeRelation.CONTAINS),
                new Input()
                        .setNeuron(relNeurons.get('d'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(2)
                        .setMinInput(0.5)
                        .setStartRangeMatch(Operator.GREATER_THAN)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );

        Document doc = m.createDocument("a b c d e ", 0);

        startSignal.addInput(doc, 0, 1, 0);

        System.out.println(doc.neuronActivationsToString(true, false, true));

        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                inputNeurons.get(c).addInput(doc, i, i + 1);
            }
            System.out.println(doc.neuronActivationsToString(true, false, true));
        }

        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ') {
                inputNeurons.get(c).addInput(doc, i, i + 1);
            }

            System.out.println(doc.neuronActivationsToString(true, false, true));
        }

        // Computes the selected option
        doc.process();

        Assert.assertEquals(1, pattern.node.get().getThreadState(doc.threadId, true).activations.size());

        System.out.println("Output activation:");
        OrNode n = pattern.node.get();
        for(NodeActivation act: n.getActivations(doc)) {
            System.out.println("Text Range: " + act.key.r);
            System.out.println("Option: " + act.key.o);
            System.out.println("Node: " + act.key.n);
            System.out.println("Rid: " + act.key.rid);
//            System.out.println("Activation weight: " + act.finalState.value);
            System.out.println();
        }

        System.out.println("All activations:");
        System.out.println(doc.neuronActivationsToString(true, false, true));
        System.out.println();


        doc.train();

        doc.clearActivations();
    }

}
