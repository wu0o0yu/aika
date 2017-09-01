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
import org.aika.Provider;
import org.aika.SuspensionHook;
import org.aika.corpus.Document;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.AbstractNeuron;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

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
        n.node.suspend();
        n.provider.suspend();


        // Reactivate
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

        AbstractNeuron nC = m.initAndNeuron(m.createNeuron("C"), 0.5,
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


        Provider<? extends AbstractNeuron> outD = m.initAndNeuron(m.createNeuron("D"), 0.5,
                new Input()
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setMinInput(0.9)
                        .setRecurrent(false)
                        .setRangeMatch(Input.RangeRelation.EQUALS)
                        .setRangeOutput(true)
        ).provider;

        m.suspendAll();

        Assert.assertTrue(outD.isSuspended());

        // Reactivate

        Document doc = m.createDocument("Bla");

        inA = m.createOrLookupInputNeuron("A");
        inA.addInput(doc, 0, 1, 0);

        inB = m.createOrLookupInputNeuron("B");
        inB.addInput(doc, 1, 2, 1);

        doc.process();

        Assert.assertFalse(outD.get().getFinalActivations(doc).isEmpty());
    }




    public static class DummySuspensionHook implements SuspensionHook {
        public AtomicInteger currentId = new AtomicInteger(0);

        Map<Integer, byte[]> storage = new TreeMap<>();

        @Override
        public int getNewId() {
            return currentId.addAndGet(1);
        }

        @Override
        public void store(int id, byte[] data) {
            storage.put(id, data);
        }

        @Override
        public byte[] retrieve(int id) {
            return storage.get(id);
        }
    }
}
