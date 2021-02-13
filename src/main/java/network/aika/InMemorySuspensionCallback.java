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

import network.aika.neuron.NeuronProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class InMemorySuspensionCallback implements SuspensionCallback {

    private AtomicInteger currentId = new AtomicInteger(0);

    private Map<Long, byte[]> storage = new TreeMap<>();
    private final Map<String, Long> labels = new HashMap<>();

    @Override
    public long createId() {
        return currentId.addAndGet(1);
    }

    @Override
    public void store(Long id, String label, Writable customData, byte[] data) {
        storage.put(id, data);
    }

    @Override
    public void delete(Long id) {
        storage.remove(id);
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
    public byte[] retrieve(Long id) {
        return storage.get(id);
    }

    @Override
    public Long getIdByLabel(String label) {
        return labels.get(label);
    }

    @Override
    public void suspendAll(NeuronProvider.SuspensionMode sm) {
    }

    @Override
    public Stream<Long> getAllIds() {
        return storage.keySet().stream();
    }
}
