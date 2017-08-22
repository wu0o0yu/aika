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


import org.aika.Input;
import org.aika.Model;
import org.aika.SuspensionHook;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.aika.corpus.Range.Operator.EQUALS;

/**
 *
 * @author Lukas Molzberger
 */
public class SuspensionTest {


    @Test
    public void testSuspendInputNeuron() {
        Model m = new Model();
        m.suspensionHook = new DummySuspensionHook();

        InputNeuron n = m.createOrLookupInputNeuron("A");
        n.suspend(m);

        n = m.createOrLookupInputNeuron("A");

        Document doc = m.createDocument("Bla");
        n.addInput(doc, 0, 1);
    }


    @Test
    public void testSuspendAndNeuron() {
        Model m = new Model();
        m.suspensionHook = new DummySuspensionHook();

        InputNeuron inA = m.createOrLookupInputNeuron("A");
        InputNeuron inB = m.createOrLookupInputNeuron("B");

        Neuron nC = m.createAndNeuron(new Neuron("C"), 0.5,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(0)
                        .setRecurrent(false)
                        .setStartRangeMatch(EQUALS)
                        .setStartRangeOutput(true),
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRelativeRid(null)
                        .setRecurrent(false)
                        .setEndRangeMatch(EQUALS)
                        .setEndRangeOutput(true)
        );


        Neuron outD = m.createAndNeuron(new Neuron("D"), 0.5,
                new Input()
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRecurrent(false)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true)
        );
        outD.noSuspension = true;


        inA.suspend(m);
        inB.suspend(m);


        // Reactivate

        Document doc = m.createDocument("Bla");

        inA = m.createOrLookupInputNeuron("A");
        inA.addInput(doc, 0, 1, 0);

        inB = m.createOrLookupInputNeuron("B");
        inB.addInput(doc, 1, 2, 1);

        doc.process();

        Assert.assertFalse(outD.getFinalActivations(doc).isEmpty());
    }




    public static class DummySuspensionHook implements SuspensionHook {

        Map<Key, byte[]> storage = new TreeMap<>();

        @Override
        public void store(long id, Type t, byte[] data) {
            storage.put(new Key(id, t), data);
        }

        @Override
        public byte[] retrieve(long id, Type t) {
            return storage.get(new Key(id, t));
        }

        private static class Key implements Comparable<Key> {
            public Key(long id, Type t) {
                this.id = id;
                this.t = t;
            }

            long id;
            Type t;

            @Override
            public int compareTo(Key k) {
                int r = Long.compare(id, k.id);
                if(r != 0) return r;
                return t.compareTo(k.t);
            }
        }
    }
}
