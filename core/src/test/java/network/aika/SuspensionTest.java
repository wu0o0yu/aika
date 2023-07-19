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
package network.aika;



import network.aika.suspension.SuspensionCallback;
import network.aika.elements.neurons.NeuronProvider;
import network.aika.elements.synapses.InputPatternSynapse;
import network.aika.elements.synapses.Synapse;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.neurons.TokenNeuron;
import network.aika.text.Document;
import network.aika.utils.Writable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static network.aika.TestUtils.getConfig;
import static network.aika.TestUtils.setBias;
import static network.aika.enums.direction.Direction.INPUT;
import static network.aika.enums.direction.Direction.OUTPUT;
import static network.aika.suspension.SuspensionMode.SAVE;

/**
 *
 * @author Lukas Molzberger
 */
public class SuspensionTest {

    @Test
    public void testSuspendInputNeuron() {
        Model m = new Model(new DummySuspensionCallback());

        NeuronProvider inStrong = new TokenNeuron().init(m, "IN Strong").getProvider(true);
        NeuronProvider inWeak = new TokenNeuron().init(m, "IN Weak").getProvider(true);
        NeuronProvider out = new BindingNeuron().init(m, "OUT").getProvider(true);
        setBias(out.getNeuron(), 1.0);

        Synapse sStrong = new InputPatternSynapse()
                .setWeight(10.0)
                .init(inStrong.getNeuron(), out.getNeuron())
                .adjustBias();

        Synapse sWeak = new InputPatternSynapse()
                .setWeight(0.5)
                .init(inWeak.getNeuron(), out.getNeuron())
                .adjustBias();

        Assertions.assertEquals(INPUT, sStrong.getStoredAt());
        Assertions.assertEquals(OUTPUT, sWeak.getStoredAt());

        inStrong.suspend(SAVE);
        inWeak.suspend(SAVE);
        out.suspend(SAVE);

        // Reactivate
        inStrong = m.lookupNeuronProvider(inStrong.getId());

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(0.1)
                .setTrainingEnabled(false);

        Document doc = new Document(m, "test");
        doc.setConfig(c);
        doc.addToken(inStrong.getNeuron(), 0, 0, 4);
    }


    public static class DummySuspensionCallback implements SuspensionCallback {
        public AtomicInteger currentId = new AtomicInteger(0);

        Map<Long, byte[]> storage = new TreeMap<>();
        private Map<String, Long> labels = new TreeMap<>();


        @Override
        public void prepareNewModel() {
        }

        @Override
        public void open() {
        }

        @Override
        public void close() {
        }

        @Override
        public long createId() {
            return currentId.addAndGet(1);
        }

        @Override
        public void store(Long id, String label, Writable customData, byte[] data) {
            storage.put(id, data);
        }

        @Override
        public void remove(Long id) {
            storage.remove(id);
        }

        @Override
        public byte[] retrieve(Long id) {
            return storage.get(id);
        }

        @Override
        public Collection<Long> getAllIds() {
            return storage.keySet();
        }

        @Override
        public Long getIdByLabel(String label) {
            return labels.get(label);
        }

        @Override
        public void putLabel(String label, Long id) {
            labels.put(label, id);
        }

        @Override
        public void removeLabel(String label) {
            labels.remove(label);
        }

        @Override
        public void loadIndex(Model m) {
        }

        @Override
        public void saveIndex(Model m) {
        }
    }
}
