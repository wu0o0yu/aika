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
import org.junit.Ignore;
import org.junit.Test;

import static org.aika.corpus.Range.Mapping.NONE;

/**
 *
 * @author Lukas Molzberger
 */
public class GenerateTextTest {

    @Ignore
    @Test
    public void testTwoWords() {
        Model m = new Model();

        Neuron suppr = m.createNeuron("SUPPR");

        Neuron inA = m.createNeuron("IN-A");
        Neuron inB = m.createNeuron("IN-B");


        Neuron outA = m.createNeuron("OUT-A", "aaaaaaa ");
        Neuron outB = m.createNeuron("OUT-B", "bbb ");
        Neuron outC = m.createNeuron("OUT-C", "ccccccccc ");
        Neuron outD = m.createNeuron("OUT-D", "ddddd ");

        m.initNeuron(outA, 4.0,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0f)
                        .setBiasDelta(1.0)
                        .setStartRangeMapping(NONE)
                        .setEndRangeMapping(NONE),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-20.0f)
                        .setBiasDelta(1.0)
        );

        m.initNeuron(outB, 5.0,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0f)
                        .setBiasDelta(1.0)
                        .setStartRangeMapping(NONE)
                        .setEndRangeMapping(NONE),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-20.0f)
                        .setBiasDelta(1.0)
        );

        m.initNeuron(outC, 5.0,
                new Input()
                        .setNeuron(outA)
                        .setWeight(10.0f)
                        .setBiasDelta(1.0)
                        .setStartRangeMapping(Range.Mapping.END)
                        .setEndRangeMapping(NONE),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-20.0f)
                        .setBiasDelta(1.0)
        );

        m.initNeuron(outD, 5.0,
                new Input()
                        .setNeuron(outB)
                        .setWeight(10.0f)
                        .setBiasDelta(1.0)
                        .setStartRangeMapping(Range.Mapping.END)
                        .setEndRangeMapping(NONE),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-20.0f)
                        .setBiasDelta(1.0)
        );


        m.initNeuron(suppr, 0.0,
                new Input()
                        .setNeuron(outA)
                        .setWeight(10.0f)
                        .setBiasDelta(0.0)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(outB)
                        .setWeight(10.0f)
                        .setBiasDelta(0.0)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(outC)
                        .setWeight(10.0f)
                        .setBiasDelta(0.0)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(outD)
                        .setWeight(10.0f)
                        .setBiasDelta(0.0)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );


        Document doc = m.createDocument("Bla");

        inA.addInput(doc, 0, 3);
        inB.addInput(doc, 0, 3);

        System.out.println(doc.neuronActivationsToString(true, true, true));

        doc.process();

        System.out.println(doc.neuronActivationsToString(true, true, true));

        System.out.println();

        System.out.println(doc.generateOutputText());
    }
}
