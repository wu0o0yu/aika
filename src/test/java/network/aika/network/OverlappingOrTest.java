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
package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.Range.Relation;
import network.aika.neuron.activation.Range.Operator;
import network.aika.neuron.INeuron;
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
                        .setSynapseId(0)
                        .setNeuron(inputNeurons.get('b'))
                        .setWeight(4.0)
                        .setRecurrent(false)
                        .addRangeRelation(Relation.END_TO_BEGIN_EQUALS, 1)
                        .addRangeRelation(Relation.create(Operator.NONE, Operator.NONE, Operator.NONE, Operator.LESS_THAN), 2)
                        .setBias(-4.0)
                        .setRangeOutput(Range.Output.BEGIN),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inputNeurons.get('c'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .addRangeRelation(Relation.END_TO_BEGIN_EQUALS, 2)
                        .setBias(0.0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inputNeurons.get('d'))
                        .setWeight(4.0)
                        .setRecurrent(false)
                        .setBias(-4.0)
                        .setRangeOutput(Range.Output.END)
        );

        Document doc = m.createDocument("a b c d e ", 0);

        System.out.println(doc.activationsToString(false, false, true));

        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ') {
                inputNeurons.get(c).addInput(doc, i, i + 2);
            }
            System.out.println(doc.activationsToString(false, false, true));
        }

        // Computes the selected option
        doc.process();

        Assert.assertEquals(1, pattern.get().getThreadState(doc.threadId, true).activations.size());

        System.out.println("All activations:");
        System.out.println(doc.activationsToString(true, true, true));
        System.out.println();

        doc.clearActivations();
    }

}
