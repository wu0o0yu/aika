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
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.NeuronProvider.SuspensionMode;
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.excitatory.PatternSynapse;
import network.aika.neuron.inhibitory.*;
import network.aika.neuron.excitatory.PatternNeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class Model {

    private static final Logger log = LoggerFactory.getLogger(Model.class);

    private int N = 0; // needs to be stored

    private static Map<Byte, Class> typeRegistry = new HashMap<>();

    static {
        registerType(PatternNeuron.class);
        registerType(PatternPartNeuron.class);
        registerType(PatternSynapse.class);
        registerType(PatternPartSynapse.class);

        registerType(InhibitoryNeuron.class);
        registerType(InhibitorySynapse.class);
    }

    private SuspensionCallback suspensionCallback;
    private AtomicLong retrievalCounter = new AtomicLong(0);

    // Important: the id field needs to be referenced by the provider!
    private WeakHashMap<Long, WeakReference<NeuronProvider>> providers = new WeakHashMap<>();
    public Map<Long, NeuronProvider> activeProviders = new TreeMap<>();

    private Templates templates = new Templates(this);

    private Config config;

    public Model() {
        this(new InMemorySuspensionCallback());
    }

    public Model(SuspensionCallback sh) {
        suspensionCallback = sh;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public abstract void linkInputRelations(Activation originAct, Direction dir);

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

    public Collection<NeuronProvider> getActiveNeurons() {
        return activeProviders
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    public NeuronProvider lookupNeuronProvider(String tokenLabel, NeuronProducer onNewCallback) {
        Long id = suspensionCallback.getIdByLabel(tokenLabel);
        if (id == null) {
            Neuron<?> n = onNewCallback.createNeuron(tokenLabel);
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
                .getAllIds()
                .map(id -> lookupNeuron(id));
    }

    public void applyMovingAverage(Config trainingConfig) {
        if(trainingConfig.getAlpha() != null) {
            N *= trainingConfig.getAlpha();
        }
    }

    private static void registerType(Class clazz) {
        byte type = (byte) typeRegistry.size();
        typeRegistry.put(type, clazz);
        try {
            clazz.getField("type").setByte(null, type);
        } catch (Exception e) {
            log.error("Initialization error: ", e);
        }
    }

    public SuspensionCallback getSuspensionHook() {
        return suspensionCallback;
    }

    public void setSuspensionHook(SuspensionCallback suspensionCallback) {
        this.suspensionCallback = suspensionCallback;
    }

    public Neuron readNeuron(DataInput in, NeuronProvider p) throws Exception {
        Constructor c = typeRegistry.get(in.readByte()).getDeclaredConstructor(NeuronProvider.class);
        Neuron n = (Neuron) c.newInstance(p);
        n.readFields(in, this);
        return n;
    }

    public Synapse readSynapse(DataInput in) throws Exception {
        Synapse s = (Synapse) typeRegistry.get(in.readByte()).getDeclaredConstructor().newInstance();
        s.readFields(in, this);
        return s;
    }

    public void writeSynapse(Synapse s, DataOutput out) throws IOException {
        s.write(out);
    }

    public void addToN(int l) {
        N += l;
    }

    public int getN() {
        return N;
    }

    public void setN(int n) {
        N = n;
    }

    public NeuronProvider lookupNeuron(Long id) {
        synchronized (providers) {
            WeakReference<NeuronProvider> wr = providers.get(id);
            if(wr != null) {
                NeuronProvider n = wr.get();
                if (n != null) {
                    return n;
                }
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
        suspensionCallback.suspendAll(sm);
    }

    private boolean suspend(long retrievalCount, NeuronProvider p, SuspensionMode sm) {
        Neuron an = p.getIfNotSuspended();
        if (an != null && an.getRetrievalCount() < retrievalCount) {
            p.suspend(sm);
            return true;
        }
        return false;
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

    public String statToString() {
        StringBuilder sb = new StringBuilder();
        providers.values().stream()
                .map(n -> n.get())
                .map(n -> n.getNeuron())
                .forEach(n -> sb.append(n.statToString() + "\n"));

        return sb.toString();
    }

    public String toString() {
        return "N:" + N;
    }

    public interface NeuronProducer {
        Neuron createNeuron(String tokenLabel);
    }
}
