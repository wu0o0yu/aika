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


import org.aika.neuron.Neuron;
import org.aika.neuron.activation.Range;
import org.aika.lattice.NodeActivation;
import org.aika.neuron.Synapse;
import org.aika.neuron.activation.Range.Relation;
import org.aika.Model;
import org.aika.Document;
import org.aika.neuron.activation.Range.Operator;
import org.aika.neuron.INeuron;
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


        // Create an input neuron and a recurrent neuron for every letter in this example.
        for(char c: new char[] {'a', 'b', 'c', 'd', 'e'}) {
            Neuron in = m.createNeuron(c + "");

            inputNeurons.put(c, in);
        }

        // Create a pattern neuron with the recurrent neurons as input. The number that are
        // given in the inputs are the recurrent ids (relativeRid) which specify the relative position
        // of the inputs relative to each other. The following flag specifies whether this relativeRid is
        // relative or absolute.
        Neuron pattern = Neuron.init(
                m.createNeuron("BCD"),
                2.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inputNeurons.get('b'))
                        .setWeight(4.0)
                        .setRecurrent(false)
                        .setRelativeRid(0)
                        .setBias(-4.0)
                        .setRangeMatch(Operator.EQUALS, Operator.GREATER_THAN_EQUAL)
                        .setRangeOutput(Range.Output.BEGIN),
                new Synapse.Builder()
                        .setNeuron(inputNeurons.get('c'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setRelativeRid(1)
                        .setBias(0.0)
                        .setRangeMatch(Relation.CONTAINS),
                new Synapse.Builder()
                        .setNeuron(inputNeurons.get('d'))
                        .setWeight(4.0)
                        .setRecurrent(false)
                        .setRelativeRid(2)
                        .setBias(-4.0)
                        .setRangeMatch(Operator.LESS_THAN_EQUAL, Operator.EQUALS)
                        .setRangeOutput(Range.Output.END)
        );

        Document doc = m.createDocument("a b c d e ", 0);

        System.out.println(doc.activationsToString(false, true));

        int wordPos = 0;
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ') {
                inputNeurons.get(c).addInput(doc, i, i + 1, wordPos++);
            }
            System.out.println(doc.activationsToString(false, true));
        }

        // Computes the selected option
        doc.process();

        Assert.assertEquals(1, pattern.get().getThreadState(doc.threadId, true).activations.size());

        System.out.println("Output activation:");
        INeuron n = pattern.get();
        for(NodeActivation act: n.getActivations(doc)) {
            System.out.println("Text Range: " + act.key.range);
            System.out.println("Node: " + act.key.node);
            System.out.println("Rid: " + act.key.rid);
//            System.out.println("Activation weight: " + act.finalState.value);
            System.out.println();
        }

        System.out.println("All activations:");
        System.out.println(doc.activationsToString(false, true));
        System.out.println();

        doc.clearActivations();
    }

}
