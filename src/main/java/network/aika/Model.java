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


import network.aika.callbacks.InMemorySuspensionCallback;
import network.aika.callbacks.SuspensionCallback;
import network.aika.neuron.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.utils.Writable;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class Model implements Writable {

    private long N = 0;

    private SuspensionCallback suspensionCallback;
    private final AtomicLong retrievalCounter = new AtomicLong(0);

    // Important: the id field needs to be referenced by the provider!
    private final WeakHashMap<Long, WeakReference<NeuronProvider>> providers = new WeakHashMap<>();
    public final Map<Long, NeuronProvider> activeProviders = new TreeMap<>();

    private final Templates templates = new Templates(this);

    private Thought currentThought;

    private Supplier<Writable> customDataInstanceSupplier;

    public Model() {
        this(new InMemorySuspensionCallback());
    }

    public Model(SuspensionCallback sc) {
        suspensionCallback = sc;
    }

    public abstract void init();

    public Long getIdByLabel(String label) {
        return suspensionCallback.getIdByLabel(label);
    }

    public void putLabel(String label, Long id) {
        suspensionCallback.putLabel(label, id);
    }

    public Supplier<Writable> getCustomDataInstanceSupplier() {
        return customDataInstanceSupplier;
    }

    public void setCustomDataInstanceSupplier(Supplier<Writable> customDataInstanceSupplier) {
        this.customDataInstanceSupplier = customDataInstanceSupplier;
    }

    public long getCurrentRetrievalCount() {
        return retrievalCounter.longValue();
    }

    public void incrementRetrievalCounter() {
        retrievalCounter.addAndGet(1);
    }

    public long createNeuronId() {
        return suspensionCallback.createId();
    }

    public Templates getTemplates() {
        return templates;
    }

    public Thought<?> getCurrentThought() {
        return currentThought;
    }

    public void setCurrentThought(Thought<?> currentThought) {
        this.currentThought = currentThought;
    }

    public Collection<NeuronProvider> getActiveNeurons() {
        return new ArrayList<>(activeProviders.values());
    }

    public NeuronProvider lookupNeuronProvider(String tokenLabel, NeuronProducer onNewCallback) {
        Long id = suspensionCallback.getIdByLabel(tokenLabel);
        if (id == null) {
            Neuron<?, ?> n = onNewCallback.createNeuron(tokenLabel);
            NeuronProvider p = n.getProvider();

            suspensionCallback.putLabel(tokenLabel, p.getId());
            return p;
        }
        return lookupNeuron(id);
    }

    public NeuronProvider getNeuronProvider(String tokenLabel) {
        Long id = suspensionCallback.getIdByLabel(tokenLabel);
        if(id == null) return null;
        return lookupNeuron(id);
    }

    public Neuron getNeuron(String tokenLabel) {
        NeuronProvider np = getNeuronProvider(tokenLabel);
        return np != null ? np.getNeuron() : null;
    }

    public Stream<NeuronProvider> getAllNeurons() {
        return suspensionCallback
                .getAllIds().stream()
                .map(this::lookupNeuron);
    }

    public void applyMovingAverage(Config trainingConfig) {
        if(trainingConfig.getAlpha() != null) {
            N *= trainingConfig.getAlpha();
        }
    }

    public SuspensionCallback getSuspensionHook() {
        return suspensionCallback;
    }

    public void setSuspensionHook(SuspensionCallback suspensionCallback) {
        this.suspensionCallback = suspensionCallback;
    }

    public void addToN(int l) {
        N += l;
    }

    public long getN() {
        return N;
    }

    public void setN(long n) {
        N = n;
    }

    public NeuronProvider lookupNeuron(Long id) {
        synchronized (providers) {
            WeakReference<NeuronProvider> wr = providers.get(id);
            if(wr != null) {
                NeuronProvider n = wr.get();
                if (n != null)
                    return n;
            }

            return new NeuronProvider(this, id);
        }
    }

    public void suspendUnusedNeurons(long retrievalCount, SuspensionMode sm) {
        synchronized (activeProviders) {
            activeProviders
                    .values()
                    .stream()
                    .filter(n -> !n.isSuspended())
                    .collect(Collectors.toList())
                    .forEach(n -> suspend(retrievalCount, n, sm));
        }
    }

    public void suspendAll(SuspensionMode sm) {
        suspendUnusedNeurons(Integer.MAX_VALUE, sm);
    }

    private void suspend(long retrievalCount, NeuronProvider p, SuspensionMode sm) {
        Neuron an = p.getIfNotSuspended();
        if (an != null && an.getRetrievalCount() < retrievalCount) {
            p.suspend(sm);
        }
    }

    public void registerWeakReference(NeuronProvider p) {
        synchronized (providers) {
            providers.put(p.getId(), new WeakReference<>(p));
        }
    }

    public void register(NeuronProvider p) {
        synchronized (activeProviders) {
            activeProviders.put(p.getId(), p);
        }
    }

    public void unregister(NeuronProvider p) {
        synchronized (activeProviders) {
            activeProviders.remove(p.getId());
        }
    }

    public void open(boolean create) throws IOException {
        if(create)
            suspensionCallback.prepareNewModel();
        else
            suspensionCallback.loadIndex(this);

        suspensionCallback.open();
    }

    public void close() throws IOException {
        suspensionCallback.saveIndex(this);

        suspensionCallback.close();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(N);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        N = in.readLong();
    }

    public String toString() {
        return "N:" + N;
    }

    public interface NeuronProducer {
        Neuron createNeuron(String tokenLabel);
    }
}
