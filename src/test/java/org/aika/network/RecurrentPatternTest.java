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


import org.aika.Iteration;
import org.aika.Model;
import org.aika.Iteration.Input;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.Activation;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.TreeMap;


/**
 *
 * @author Lukas Molzberger
 */
public class RecurrentPatternTest {

    @Test
    public void testRecurrentPattern() {
        String txt = "A B C D E F G H.";

        Model m = new Model();
        Document doc = new Document(txt);
        Iteration t = m.startIteration(doc, 0);

        InputNeuron startSignal = t.createOrLookupInputSignal("START-SIGNAL");
        InputNeuron spaceN = t.createOrLookupInputSignal("SPACE");


        TreeMap<Character, InputNeuron> chars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            InputNeuron charSN = t.createOrLookupInputSignal("" + c);
            chars.put(c, charSN);
        }

        Neuron ctNeuron = t.createCounterNeuron(new Neuron("CTN"), spaceN, false, startSignal, true, false);

        TreeMap<Character, Neuron> recChars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            InputNeuron charSN = chars.get(c);
            recChars.put(c, t.createRelationalNeuron(new Neuron("RN-" + c), ctNeuron, charSN, false));
        }

        Neuron patternN = t.createAndNeuron(new Neuron("PATTERN"),
                0.001,
                new Input()
                        .setNeuron(recChars.get('C'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(recChars.get('D'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(1)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(recChars.get('E'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(2)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(ctNeuron)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
                        .setMatchRange(false)
        );


        startSignal.addInput(t, 0, 1, 0);
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                spaceN.addInput(t, i, i + 1);
            }
        }
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ' && c != '.') {
                chars.get(c).addInput(t, i, i + 1);
            }
        }

        System.out.println("All activations:");
        System.out.println(t.networkStateToString(true, true));
        System.out.println();

        t.process();

        System.out.println("All activations:");
        System.out.println(t.networkStateToString(true, true));
        System.out.println();

        Activation patAct = patternN.node.getFirstActivation(t);
        Assert.assertEquals(4, patAct.key.r.begin);
        Assert.assertEquals(10, patAct.key.r.end);

        t.clearActivations();

    }



    @Test
    public void testRecurrentPattern1() {
        String txt = "A B C D E F G H.";

        Model m = new Model();
        Document doc = new Document(txt);
        Iteration t = m.startIteration(doc, 0);

        InputNeuron startSignal = t.createOrLookupInputSignal("START-SIGNAL");
        InputNeuron spaceN = t.createOrLookupInputSignal("SPACE");

        TreeMap<Character, InputNeuron> chars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            InputNeuron charSN = t.createOrLookupInputSignal("" + c);
            chars.put(c, charSN);
        }

        Neuron ctNeuron = new Neuron("CTN");

        t.createCounterNeuron(ctNeuron, spaceN, false, startSignal, true, false);

        TreeMap<Character, Neuron> recChars = new TreeMap<>();
        for(char c = 'A'; c <= 'Z'; c++) {
            InputNeuron charSN = chars.get(c);
            recChars.put(c, t.createRelationalNeuron(new Neuron("RN-" + c), ctNeuron, charSN, false));
        }

        Neuron patternN = t.createAndNeuron(new Neuron("PATTERN"),
                0.001,
                new Input()
                        .setNeuron(recChars.get('C'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(recChars.get('D'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(1)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(recChars.get('E'))
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(2)
                        .setMatchRange(false),
                new Input()
                        .setNeuron(ctNeuron)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setRelativeRid(0)
                        .setMatchRange(false)
        );


        startSignal.addInput(t, 0, 1, 0);
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c == ' ') {
                spaceN.addInput(t, i, i + 1);
            }
        }
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().charAt(i);
            if(c != ' ' && c != '.') {
                chars.get(c).addInput(t, i, i + 1);
            }
        }

        t.process();

        System.out.println("All activations:");
        System.out.println(t.networkStateToString(true, true));
        System.out.println();

        Activation patAct = patternN.node.getFirstActivation(t);
        Assert.assertEquals(4, patAct.key.r.begin);
        Assert.assertEquals(10, patAct.key.r.end);

        t.clearActivations();

    }


    @Test
    public void testAndWithRid() {
        Model m = new Model();
        Iteration t = m.startIteration(null, 0);

        InputNeuron start = t.createOrLookupInputSignal("START");
        InputNeuron clock = t.createOrLookupInputSignal("CLOCK");
        InputNeuron input = t.createOrLookupInputSignal("INPUT");

        Neuron ctn = t.createCounterNeuron(new Neuron("CTN"), clock, false, start, true, false);
        Neuron rn = t.createRelationalNeuron(new Neuron("RN"), ctn, input, false);

        InputNeuron aN = t.createOrLookupInputSignal("A");
        InputNeuron bN = t.createOrLookupInputSignal("B");
        InputNeuron cN = t.createOrLookupInputSignal("C");

        Neuron result = t.createAndNeuron(new Neuron("RESULT"),
                0.001,
                new Input()
                        .setNeuron(ctn)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0)
                        .setAbsoluteRid(1),
                new Input()
                        .setNeuron(aN)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setMinInput(1.0),
                new Input()
                        .setNeuron(bN)
                        .setWeight(-1.0)
                        .setRecurrent(true)
                        .setMinInput(1.0)
                        .setAbsoluteRid(0)
        );
    }


    @Test
    public void testCTNeuron() {
        Model m = new Model();
        Document doc = Document.create("                                                  ");
        Iteration t = m.startIteration(doc, 0);

        InputNeuron start = t.createOrLookupInputSignal("START");
        InputNeuron clock = t.createOrLookupInputSignal("CLOCK");

        Neuron ctn = t.createCounterNeuron(new Neuron("CTN"), clock, false, start, true, false);


        for(int i = 5; i < 30; i += 5) {
            clock.addInput(t, i - 1, i);
        }

        System.out.println(t.networkStateToString(true, false));

        start.addInput(t, 0, 1, 0);

        System.out.println(t.networkStateToString(true, false));

        Assert.assertEquals(2, Activation.get(t, ctn.node, 2, new Range(10, 15), Range.Relation.EQUALS, null, null).key.o.primId);
    }
}
