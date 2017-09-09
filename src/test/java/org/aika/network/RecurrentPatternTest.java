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
import org.aika.Provider;
import org.aika.lattice.NodeActivation;
import org.aika.Input;
import org.aika.Input.RangeRelation;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.TreeMap;

import static org.aika.corpus.Range.Operator.*;


/**
 *
 * @author Lukas Molzberger
 */
public class RecurrentPatternTest {

    @Test
    public void testRecurrentPattern() {
        String txt = "A B C D E F G H.";

        Model m = new Model();

        Neuron startSignal = m.createNeuron("START-SIGNAL");
        Neuron spaceN = m.createNeuron("SPACE");


        TreeMap<Character, Neuron> chars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            Neuron charSN = m.createNeuron("" + c);
            chars.put(c, charSN);
        }

        Neuron ctNeuron = m.initCounterNeuron(m.createNeuron("CTN"), spaceN, false, startSignal, true, false);

        TreeMap<Character, Neuron> recChars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            Neuron charSN = chars.get(c);
            recChars.put(c, m.initRelationalNeuron(m.createNeuron("RN-" + c), ctNeuron, charSN, false));
        }

        Neuron patternN = m.initAndNeuron(m.createNeuron("PATTERN"),
                0.001,
                new Input()
                        .setNeuron(recChars.get('C'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRelativeRid(0)
                        .setStartRangeMatch(EQUALS)
                        .setEndRangeMatch(GREATER_THAN)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(recChars.get('D'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRelativeRid(1)
                        .setRangeMatch(RangeRelation.CONTAINS),
                new Input()
                        .setNeuron(recChars.get('E'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRelativeRid(2)
                        .setStartRangeMatch(LESS_THAN)
                        .setEndRangeMatch(EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(ctNeuron)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRelativeRid(0)
                        .setRangeMatch(RangeRelation.NONE)
        );

        Document doc = m.createDocument(txt, 0);

        startSignal.get().addInput(doc, 0, 1, 0);
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                spaceN.get().addInput(doc, i, i + 1);
            }
        }
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ' && c != '.') {
                chars.get(c).get().addInput(doc, i, i + 1);
            }
        }

        System.out.println("All activations:");
        System.out.println(doc.neuronActivationsToString(true, false, true));
        System.out.println();

        doc.process();

        System.out.println("All activations:");
        System.out.println(doc.neuronActivationsToString(true, false, true));
        System.out.println();

        NodeActivation patAct = patternN.get().node.get().getFirstActivation(doc);
        Assert.assertEquals(4, patAct.key.r.begin.intValue());
        Assert.assertEquals(10, patAct.key.r.end.intValue());

        doc.clearActivations();

    }



    @Test
    public void testRecurrentPattern1() {
        String txt = "A B C D E F G H.";

        Model m = new Model();

        Neuron startSignal = m.createNeuron("START-SIGNAL");
        Neuron spaceN = m.createNeuron("SPACE");

        TreeMap<Character, Neuron> chars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            Neuron charSN = m.createNeuron("" + c);
            chars.put(c, charSN);
        }

        Neuron ctNeuron = m.createNeuron("CTN");

        m.initCounterNeuron(ctNeuron, spaceN, false, startSignal, true, false);

        TreeMap<Character, Neuron> recChars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            Neuron charSN = chars.get(c);
            recChars.put(c, m.initRelationalNeuron(m.createNeuron("RN-" + c), ctNeuron, charSN, false));
        }

        Neuron patternN = m.initAndNeuron(m.createNeuron("PATTERN"),
                0.001,
                new Input()
                        .setNeuron(recChars.get('C'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRelativeRid(0)
                        .setStartRangeMatch(EQUALS)
                        .setEndRangeMatch(GREATER_THAN)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(recChars.get('D'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRelativeRid(1)
                        .setRangeMatch(RangeRelation.CONTAINS),
                new Input()
                        .setNeuron(recChars.get('E'))
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRelativeRid(2)
                        .setStartRangeMatch(LESS_THAN)
                        .setEndRangeMatch(EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(ctNeuron)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setRelativeRid(0)
                        .setRangeMatch(RangeRelation.NONE)
        );


        Document doc = m.createDocument(txt, 0);

        startSignal.get().addInput(doc, 0, 1, 0);
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                spaceN.get().addInput(doc, i, i + 1);
            }
        }
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ' && c != '.') {
                chars.get(c).get().addInput(doc, i, i + 1);
            }
        }

        doc.process();

        System.out.println("All activations:");
        System.out.println(doc.neuronActivationsToString(true, false, true));
        System.out.println();

        NodeActivation patAct = patternN.get().node.get().getFirstActivation(doc);
        Assert.assertEquals(4, patAct.key.r.begin.intValue());
        Assert.assertEquals(10, patAct.key.r.end.intValue());

        doc.clearActivations();

    }


    @Test
    public void testAndWithRid() {
        Model m = new Model();

        Neuron start = m.createNeuron("START");
        Neuron clock = m.createNeuron("CLOCK");
        Neuron input = m.createNeuron("INPUT");

        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), clock, false, start, true, false);
        Neuron rn = m.initRelationalNeuron(m.createNeuron("RN"), ctn, input, false);

        Neuron aN = m.createNeuron("A");
        Neuron bN = m.createNeuron("B");
        Neuron cN = m.createNeuron("C");

        Neuron result = m.initAndNeuron(m.createNeuron("RESULT"),
                0.001,
                new Input()
                        .setNeuron(ctn)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f)
                        .setAbsoluteRid(1),
                new Input()
                        .setNeuron(aN)
                        .setWeight(1.0f)
                        .setRecurrent(false)
                        .setMinInput(1.0f),
                new Input()
                        .setNeuron(bN)
                        .setWeight(-1.0f)
                        .setRecurrent(true)
                        .setMinInput(1.0f)
                        .setAbsoluteRid(0)
        );
    }


    @Test
    public void testCTNeuron() {
        Model m = new Model();

        Neuron start = m.createNeuron("START");
        Neuron clock = m.createNeuron("CLOCK");

        Neuron ctn = m.initCounterNeuron(m.createNeuron("CTN"), clock, false, start, true, false);


        Document doc = m.createDocument("                                                  ", 0);

        for(int i = 5; i < 30; i += 5) {
            clock.get().addInput(doc, i - 1, i);
        }

        System.out.println(doc.neuronActivationsToString(false, false, true));

        start.get().addInput(doc, 0, 1, 0);

        System.out.println(doc.neuronActivationsToString(false, false, true));

        Assert.assertEquals(2, NodeActivation.get(doc, ctn.get().node.get(), 2, new Range(10, 15), EQUALS, EQUALS, null, null).key.o.primId);
    }
}
