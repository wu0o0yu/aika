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


import org.aika.Neuron;
import org.aika.lattice.NodeActivation;
import org.aika.Input;
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range.Operator;
import org.aika.lattice.OrNode;
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
    public void testPatternMatching3() {
        Model m = new Model();

        Map<Character, Neuron> inputNeurons = new HashMap<>();

        // Create an input neuron and a recurrent neuron for every letter in this example.
        for(char c: new char[] {'a', 'b', 'c', 'd', 'e'}) {
            Neuron in = m.createNeuron(c + "");

            inputNeurons.put(c, in);
        }

        // Create a pattern neuron with the relational neurons as input. The numbers that are
        // given in the inputs are the recurrent ids (relativeRid) which specify the relative position
        // of the inputs relative to each other. The following flag specifies whether this relativeRid
        // is relative or absolute.
        Neuron pattern = m.initNeuron(
                m.createNeuron("BCD"),
                0.4,
                new Input()
                        .setNeuron(inputNeurons.get('b'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-0.9)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setEndRangeMatch(Operator.GREATER_THAN_EQUAL)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inputNeurons.get('c'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(1)
                        .setBias(-0.9)
                        .setRangeMatch(RangeRelation.CONTAINS),
                new Input()
                        .setNeuron(inputNeurons.get('d'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(2)
                        .setBias(-0.9)
                        .setStartRangeMatch(Operator.LESS_THAN_EQUAL)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );


        // Create a simple text document.
        Document doc = m.createDocument("a b c d e ", 0);

        // Then add the characters
        int wordPos = 0;
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ') {
                inputNeurons.get(c).addInput(doc, i, i + 1, wordPos);
            } else {
                wordPos++;
            }
        }

        // Computes the selected option
        doc.process();

        Assert.assertEquals(1, pattern.get().node.get().getThreadState(doc.threadId, true).activations.size());


        System.out.println("Output activation:");
        OrNode n = pattern.get().node.get();
        for(NodeActivation act: n.getActivations(doc)) {
            System.out.println("Text Range: " + act.key.range);
            System.out.println("Option: " + act.key.interpretation);
            System.out.println("Node: " + act.key.node);
            System.out.println("Rid: " + act.key.rid);
            System.out.println();
        }

        System.out.println("All activations:");
        System.out.println(doc.neuronActivationsToString(true, false, true));
        System.out.println();

        doc.clearActivations();
    }


    @Test
    public void testPatternMatching4() {
        Model m = new Model();

        Map<Character, Neuron> inputNeurons = new HashMap<>();

        // Create an input neuron and a recurrent neuron for every letter in this example.
        for(char c: new char[] {'a', 'b', 'c', 'd', 'e', 'f'}) {
            Neuron in = m.createNeuron(c + "");

            inputNeurons.put(c, in);
        }

        // Create a pattern neuron with the relational neurons as input. The numbers that are
        // given in the inputs are the recurrent ids (relativeRid) which specify the relative position
        // of the inputs relative to each other. The following flag specifies whether this relativeRid
        // is relative or absolute.
        Neuron pattern = m.initNeuron(
                m.createNeuron("BCDE"),
                0.4,
                new Input()
                        .setNeuron(inputNeurons.get('b'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-0.9)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setEndRangeMatch(Operator.GREATER_THAN_EQUAL)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inputNeurons.get('c'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(1)
                        .setBias(-0.9)
                        .setRangeMatch(RangeRelation.CONTAINS),
                new Input()
                        .setNeuron(inputNeurons.get('d'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(2)
                        .setBias(-0.9)
                        .setRangeMatch(RangeRelation.CONTAINS),
                new Input()
                        .setNeuron(inputNeurons.get('e'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setRelativeRid(3)
                        .setBias(-0.9)
                        .setStartRangeMatch(Operator.LESS_THAN_EQUAL)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );


        // Create a simple text document.
        Document doc = m.createDocument("a b c d e ", 0);

        // Then add the characters
        int wordPos = 0;
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ') {
                inputNeurons.get(c).addInput(doc, i, i + 1, wordPos);
            } else {
                wordPos++;
            }
        }

        // Computes the selected option
        doc.process();

        Assert.assertEquals(1, pattern.get().node.get().getThreadState(doc.threadId, true).activations.size());


        System.out.println("Output activation:");
        OrNode n = pattern.get().node.get();
        for(NodeActivation act: n.getActivations(doc)) {
            System.out.println("Text Range: " + act.key.range);
            System.out.println("Interpretation: " + act.key.interpretation);
            System.out.println("Node: " + act.key.node);
            System.out.println("Rid: " + act.key.rid);
            System.out.println();
        }

        System.out.println("All activations:");
        System.out.println(doc.neuronActivationsToString(true, false, true));
        System.out.println();

        doc.clearActivations();
    }
    
}
