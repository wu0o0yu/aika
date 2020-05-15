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


import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.Provider.SuspensionMode;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.excitatory.patternpart.*;
import network.aika.neuron.inhibitory.*;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *
 * @author Lukas Molzberger
 */
public class Model {

    private static final Logger log = LoggerFactory.getLogger(Model.class);

    private int N = 0; // needs to be stored

    private static Map<Byte, Class> typeRegistry = new HashMap<>();

    static {
        register(PatternNeuron.class);
        register(PatternSynapse.class);

        register(PatternPartNeuron.class);
        register(PatternPartSynapse.class);

        register(PatternInhibitoryNeuron.class);
        register(PatternPartInhibitoryNeuron.class);
        register(InhibitorySynapse.class);
        register(PrimaryInhibitorySynapse.class);
    }

    private SuspensionHook suspensionHook;
    private AtomicInteger currentNeuronId = new AtomicInteger(0);

    // Important: the id field needs to be referenced by the provider!
    public WeakHashMap<Integer, WeakReference<Provider<? extends AbstractNode>>> providers = new WeakHashMap<>();
    public Map<Integer, Provider<? extends AbstractNode>> activeProviders = new TreeMap<>();

    private Config trainingConfig = new Config();

    public Model() {
        this(null);
    }

    public Model(SuspensionHook sh) {
        suspensionHook = sh;
    }

    public int createNeuronId() {
        return suspensionHook != null ? suspensionHook.getNewId() : currentNeuronId.addAndGet(1);
    }

    public void applyMovingAverage() {
        if(trainingConfig.getAlpha() != null) {
            N *= trainingConfig.getAlpha();
        }
    }

    private static void register(Class clazz) {
        byte type = (byte) typeRegistry.size();
        typeRegistry.put(type, clazz);
        try {
            clazz.getField("type").setByte(null, type);
        } catch (Exception e) {
            log.error("Initialization error: ", e);
        }
    }

    public Config getTrainingConfig() {
        return trainingConfig;
    }

    public void setTrainingConfig(Config trainingConfig) {
        this.trainingConfig = trainingConfig;
    }

    public static Class getClassForType(byte type) {
        return typeRegistry.get(type);
    }

    public SuspensionHook getSuspensionHook() {
        return suspensionHook;
    }

    public void setSuspensionHook(SuspensionHook suspensionHook) {
        this.suspensionHook = suspensionHook;
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

    public Collection<NeuronProvider> getActiveNeurons() {
        List<NeuronProvider> tmp = new ArrayList<>();
        for(Provider<?> p: activeProviders.values()) {
            if(p instanceof NeuronProvider) {
                tmp.add((NeuronProvider) p);
            }
        }

        return tmp;
    }

    public NeuronProvider lookupNeuron(int id) {
        synchronized (providers) {
            WeakReference<Provider<? extends AbstractNode>> wr = providers.get(id);
            if(wr != null) {
                NeuronProvider n = (NeuronProvider) wr.get();
                if (n != null) {
                    return n;
                }
            }

            return new NeuronProvider(this, id);
        }
    }

    public void register(Provider p) {
        synchronized (activeProviders) {
            activeProviders.put(p.id, p);
        }
    }

    public void unregister(Provider p) {
        synchronized (activeProviders) {
            activeProviders.remove(p.id);
        }
    }

    private boolean suspend(int docId, Provider<? extends AbstractNode> p, SuspensionMode sm) {
        AbstractNode an = p.getIfNotSuspended();
        if (an != null && an.lastUsedDocumentId < docId) {
            p.suspend(sm);
            return true;
        }
        return false;
    }

    public void removeProvider(Provider p) {
        synchronized (activeProviders) {
            activeProviders.remove(p.id);
        }
        synchronized (providers) {
            providers.remove(p.id);
        }
    }

    public void dumpStat() {
        for(NeuronProvider n: getActiveNeurons()) {
            Neuron tn = n.get();
            tn.dumpStat();
        }
        System.out.println();
    }
}
