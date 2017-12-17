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
package org.aika.corpus;

import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.corpus.Range.Mapping.END;
import static org.aika.corpus.Range.Mapping.NONE;
import static org.aika.corpus.Range.Mapping.START;

/**
 *
 * @author Lukas Molzberger
 */
public class GenerateTextTest {

    @Test
    public void testTwoWords() {
        Model m = new Model();

        // The suppressing neuron should prevent that two neurons output overlapping text.
        Neuron suppr = m.createNeuron("SUPPR");

        // Some input neurons
        Neuron inA = m.createNeuron("IN-A");
        Neuron inB = m.createNeuron("IN-B");

        // Four test words that should be used to generate a text.
        Neuron outA = m.createNeuron("OUT-A", "aaaaaaa ");
        Neuron outB = m.createNeuron("OUT-B", "bbb ");
        Neuron outC = m.createNeuron("OUT-C", "ccccccccc ");
        Neuron outD = m.createNeuron("OUT-D", "ddddd ");

        // Word aaaaaaa is only added to the resulting text if input a is active and this neuron
        // is not suppressed by another neuron. Output aaaaaaa may start a text.
        m.initNeuron(outA, 4.0,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setStartRangeMapping(NONE)
                        .setEndRangeMapping(NONE),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setStartRangeMapping(END)   // Check if the suppressing activation and the outA activation are overlapping each other.
                        .setEndRangeMapping(START)
                        .setStartRangeMatch(Range.Operator.LESS_THAN)
                        .setEndRangeMatch(Range.Operator.GREATER_THAN)
        );

        // Word bbb is only added to the resulting text if input b is active and this neuron
        // is not suppressed by another neuron. Output bbb may start a text.
        // Neuron outB has a slightly higher weight than outA.
        m.initNeuron(outB, 5.0,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setStartRangeMapping(NONE)
                        .setEndRangeMapping(NONE),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setStartRangeMapping(END)
                        .setEndRangeMapping(START)
                        .setStartRangeMatch(Range.Operator.LESS_THAN)
                        .setEndRangeMatch(Range.Operator.GREATER_THAN)
        );


        // OutC is only activated if the previous word was outA.
        m.initNeuron(outC, 5.0,
                new Input()
                        .setNeuron(outA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setStartRangeMapping(Range.Mapping.END)
                        .setEndRangeMapping(NONE),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setStartRangeMapping(END)
                        .setEndRangeMapping(START)
                        .setStartRangeMatch(Range.Operator.LESS_THAN)
                        .setEndRangeMatch(Range.Operator.GREATER_THAN)
        );

        // OutD is only activated if the previous word was outB.
        m.initNeuron(outD, 5.0,
                new Input()
                        .setNeuron(outB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setStartRangeMapping(Range.Mapping.END)
                        .setEndRangeMapping(NONE),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-20.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setStartRangeMapping(END)
                        .setEndRangeMapping(START)
                        .setStartRangeMatch(Range.Operator.LESS_THAN)
                        .setEndRangeMatch(Range.Operator.GREATER_THAN)
        );


        // All outputs suppress each other.
        m.initNeuron(suppr, 0.0,
                new Input()
                        .setNeuron(outA)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(outB)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(outC)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(outD)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );


        Document doc = m.createDocument("Bla");

        // Add both input a and input b.
        inA.addInput(doc, 0, 3);
        inB.addInput(doc, 0, 3);

        System.out.println(doc.neuronActivationsToString(true, true, true));

        // Search for the best interpretation.
        doc.process();

        System.out.println(doc.neuronActivationsToString(true, true, true));

        System.out.println();

        // Generate the output text.
        System.out.println(doc.generateOutputText());

        Assert.assertEquals("bbb ddddd ", doc.generateOutputText());
    }
}
