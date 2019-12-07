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


import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.Provider.SuspensionMode;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.TSynapse;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.NegExcitatorySynapse;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.MetaInhibSynapse;
import network.aika.neuron.input.InputNeuron;
import network.aika.neuron.meta.MetaNeuron;
import network.aika.neuron.meta.MetaPatternNeuron;
import network.aika.neuron.meta.MetaSynapse;
import network.aika.neuron.meta.NegMetaSynapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;



/**
 * The model consists of two layers. The first layer is the actual neural network consisting of neurons and synapses.
 * The second layer is a pattern lattice containing a boolean logic representation of all the neurons. Whenever the
 * synapse weights of a neuron are adjusted, then the underlying boolean logic representation of this neuron will be
 * updated too.
 * <p>
 * <p>The model supports parallel processing using a fixed number of threads.
 *
 * @author Lukas Molzberger
 */
public class Model {

    public int charCounter = 0;


    public int numberOfThreads = 1;

    public int[] lastCleanup;

    public Document[] docs;

    public SuspensionHook suspensionHook;

    public AtomicInteger docIdCounter = new AtomicInteger(0);
    public AtomicInteger currentId = new AtomicInteger(0);

    // Important: the id field needs to be referenced by the provider!
    public WeakHashMap<Integer, WeakReference<Provider<? extends AbstractNode>>> providers = new WeakHashMap<>();
    public Map<Integer, Provider<? extends AbstractNode>> activeProviders = new TreeMap<>();

    public Map<Integer, PassiveInputFunction> passiveActivationFunctions = new TreeMap<>();

    public int defaultThreadId = 0;
    public static AtomicLong visitedCounter = new AtomicLong(1);

    /**
     * Creates a model with a single thread.
     */
    public Model() {
        this(null, 1);
    }


    public Model(SuspensionHook sh, int numberOfThreads) {
        assert numberOfThreads >= 1;
        this.numberOfThreads = numberOfThreads;

        lastCleanup = new int[numberOfThreads];
        docs = new Document[numberOfThreads];
        suspensionHook = sh;
    }


    public SuspensionHook getSuspensionHook() {
        return suspensionHook;
    }


    public void setSuspensionHook(SuspensionHook suspensionHook) {
        this.suspensionHook = suspensionHook;
    }


    public INeuron readNeuron(DataInput in, Neuron p) throws IOException {
        INeuron n = null;
        switch(in.readUTF()) {
            case PatternNeuron.TYPE_STR:
                n = new PatternNeuron(p);
                break;
            case MetaPatternNeuron.TYPE_STR:
                n = new MetaPatternNeuron(p);
                break;
            case ExcitatoryNeuron.TYPE_STR:
                n = new ExcitatoryNeuron(p);
                break;
            case InhibitoryNeuron.TYPE_STR:
                n = new InhibitoryNeuron(p);
                break;
            case MetaNeuron.TYPE_STR:
                n = new MetaNeuron(p);
                break;
            case InputNeuron.TYPE_STR:
                n = new InputNeuron(p);
                break;
        }

        n.readFields(in, this);
        return n;
    }


    public Synapse readSynapse(DataInput in) throws IOException {
        Synapse s = null;
        switch(in.readUTF()) {
            case ExcitatorySynapse.TYPE_STR:
                s = new ExcitatorySynapse();
                break;
            case NegExcitatorySynapse.TYPE_STR:
                s = new NegExcitatorySynapse();
                break;
            case InhibitorySynapse.TYPE_STR:
                s = new InhibitorySynapse();
                break;
            case MetaInhibSynapse.TYPE_STR:
                s = new MetaInhibSynapse();
                break;
            case MetaSynapse.TYPE_STR:
                s = new MetaSynapse();
                break;
            case NegMetaSynapse.TYPE_STR:
                s = new NegMetaSynapse();
                break;
        }

        s.readFields(in, this);
        return s;
    }


    public void writeSynapse(Synapse s, DataOutput out) throws IOException {
        s.write(out);
    }


    public int getNewDocumentId() {
        return docIdCounter.addAndGet(1);
    }


    public void acquireThread(int threadId, Document doc) {
        if (docs[threadId] != null) {
            throw new StaleDocumentException();
        }
        docs[threadId] = doc;
    }


    public Collection<Neuron> getActiveNeurons() {
        List<Neuron> tmp = new ArrayList<>();
        for(Provider<?> p: activeProviders.values()) {
            if(p instanceof Neuron) {
                tmp.add((Neuron) p);
            }
        }

        return tmp;
    }


    public Neuron lookupNeuron(int id) {
        synchronized (providers) {
            WeakReference<Provider<? extends AbstractNode>> wr = providers.get(id);
            if(wr != null) {
                Neuron n = (Neuron) wr.get();
                if (n != null) {
                    return n;
                }
            }

            return new Neuron(this, id);
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



    /**
     * Suspend all neurons and logic nodes whose last used document id is lower/older than docId.
     *
     * @param docId
     */
    public void suspendUnusedNodes(int docId, SuspensionMode sm) {
        docId = Math.min(docId, getOldestDocIdInProcessing());
        List<Provider> tmp;
        synchronized (activeProviders) {
            tmp = new ArrayList<>(activeProviders.values());
        }
        for (Provider p: tmp) {
            suspend(docId, p, sm);
        }
    }


    public int getOldestDocIdInProcessing() {
        int oldestDocId = Integer.MAX_VALUE;
        for(Document doc: docs) {
            if(doc != null) oldestDocId = Math.min(oldestDocId, doc.getId());
        }
        return oldestDocId;
    }


    /**
     * Suspend all neurons and logic nodes in memory.
     *
     */
    public void suspendAll(SuspensionMode sm) {
        suspendUnusedNodes(Integer.MAX_VALUE, sm);
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


    public static class StaleDocumentException extends RuntimeException {

        public StaleDocumentException() {
            super("Two documents are using the same thread. Call clearActivations() first, before processing the next document.");
        }
    }



    public MetaNeuron createMetaNeuron(String label) {
        MetaNeuron metaNeuron = new MetaNeuron(this, "M-" + label);
        InhibitoryNeuron inhibNeuron = new InhibitoryNeuron(this, "I-" + label);
        metaNeuron.setInhibitoryNeuron(inhibNeuron);

        return metaNeuron;
    }


    public void initMetaNeuron(MetaNeuron metaNeuron, double bias, double trainingBias, Synapse.Builder... inputs) {
        InhibitoryNeuron inhibNeuron = metaNeuron.getInhibitoryNeuron();

        List<Synapse.Builder> inputsList = new ArrayList<>(Arrays.asList(inputs));

        inputsList.forEach(b -> b.registerSynapseIds(metaNeuron.getProvider()));

        Integer inhibSynId = metaNeuron.getNewSynapseId();
        inputsList.add(
                new MetaSynapse.Builder()
                        .setIsMetaVariable(false)
                        .setSynapseId(inhibSynId)
                        .setNeuron(inhibNeuron.getProvider())
                        .setWeight(-100.0)
        );


        metaNeuron.trainingBias = trainingBias;

        Neuron.init(metaNeuron.getProvider(), bias + trainingBias, inputsList);

        inhibSynId = inhibNeuron.getNewSynapseId();
        Neuron.init(inhibNeuron.getProvider(),
                new MetaInhibSynapse.Builder()
                        .setSynapseId(inhibSynId)
                        .setNeuron(metaNeuron.getProvider())
                        .setWeight(1.0)
        );
    }


    public void dumpStat() {
        for(Neuron n: getActiveNeurons()) {
            TNeuron tn = (TNeuron) n.get();
            tn.dumpStat();
        }
        System.out.println();
    }


    public void dumpModel() {
        System.out.println();
        System.out.println("Dump Model:");
        for(Neuron n: getActiveNeurons()) {
            TNeuron tn = (TNeuron) n.get();
            System.out.println(tn.toStringWithSynapses());
            System.out.println();
        }
    }
}
