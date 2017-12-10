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

import org.aika.Converter;
import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Range;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class ConverterTest {


    @Test
    public void testConverter() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");

        Neuron out = m.initNeuron(m.createNeuron("ABCD"),
                -9.5,
                new Input()
                        .setNeuron(inA)
                        .setWeight(4.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(3.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inC)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inD)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true)
        );

        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, out.get().node.get().parents.firstEntry().getValue().size());

        out.get().bias = -8.5;
        Converter.convert(m, 0, out.get(), out.get().inputSynapses.values());

        System.out.println(out.get().node.get().logicToString());

        Assert.assertEquals(1, out.get().node.get().parents.firstEntry().getValue().size());
    }


    @Test
    public void testConverter1() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");

        Neuron out = m.initNeuron(m.createNeuron("ABCD"),
                -5.0,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inC)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inD)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true)
        );

        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, out.get().node.get().parents.firstEntry().getValue().size());
    }


    @Test
    public void testConverter2() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");
        Neuron inD = m.createNeuron("D");
        Neuron inE = m.createNeuron("E");

        Neuron out = m.initNeuron(m.createNeuron("ABCD"),
                -11.0,
                new Input()
                        .setNeuron(inA)
                        .setWeight(5.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(5.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inC)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inD)
                        .setWeight(2.0f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inE)
                        .setWeight(0.5f)
                        .setRecurrent(false)
                        .setBiasDelta(0.0)
                        .setStartRangeMatch(Range.Operator.EQUALS)
                        .setStartRangeOutput(true)
                        .setEndRangeMatch(Range.Operator.EQUALS)
                        .setEndRangeOutput(true)
        );

        System.out.println(out.get().node.get().logicToString());

        Assert.assertEquals(2, out.get().node.get().parents.firstEntry().getValue().size());


        inD.inMemoryOutputSynapses.firstEntry().getValue().newWeight = 0.5f;

        Converter.convert(m, 0, out.get(), out.get().inputSynapses.values());
        System.out.println(out.get().node.get().logicToString());
        Assert.assertEquals(1, out.get().node.get().parents.firstEntry().getValue().size());

    }

}
