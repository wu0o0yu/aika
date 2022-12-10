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
package network.aika.neuron;

import network.aika.Model;
import network.aika.utils.ReadWriteLock;

import java.io.*;
import java.util.HashMap;

import static network.aika.neuron.SuspensionMode.SAVE;

/**
 * The {@code NeuronProvider} class is a proxy implementation for the real neuron implementation in the class {@code Neuron}.
 * Aika uses the provider pattern to store and reload rarely used neurons or logic nodes.
 *
 * @author Lukas Molzberger
 */
public class NeuronProvider implements Comparable<NeuronProvider> {

    public static final NeuronProvider MIN_NEURON = new NeuronProvider(Long.MIN_VALUE);
    public static final NeuronProvider MAX_NEURON = new NeuronProvider(Long.MAX_VALUE);

    private Model model;
    private final Long id;

    private volatile Neuron neuron;

    HashMap<Long, Synapse> activeInputSynapses = new HashMap<>();
    HashMap<Long, Synapse> activeOutputSynapses = new HashMap<>();

    protected final ReadWriteLock lock = new ReadWriteLock();

    private boolean permanent;
    private boolean isRegistered;

    public NeuronProvider(long id) {
        this.id = id;
    }

    public NeuronProvider(Model model, long id) {
        this(id);
        assert model != null;
        this.model = model;
    }

    public NeuronProvider(Model model, Neuron n) {
        this(model, model.createNeuronId());
        assert model != null && n != null;

        this.neuron = n;
    }

    public Neuron getNeuron() {
        if (neuron == null)
            reactivate();

        return neuron;
    }

    public void setNeuron(Neuron<?, ?> n) {
        this.neuron = n;
    }

    public String getLabel() {
        return getNeuron().getLabel();
    }

    public Long getId() {
        return id;
    }

    public Model getModel() {
        return model;
    }

    public boolean isSuspended() {
        return neuron == null;
    }

    public Neuron getIfNotSuspended() {
        return neuron;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;

        if(permanent)
            checkRegister();
        else
            checkUnregister();
    }

    public synchronized void suspend(SuspensionMode sm) {
        if(neuron == null) return;
        assert model.getSuspensionCallback() != null;

        if(permanent) {
            if(sm == SAVE)
                save();
            return;
        }

        neuron.suspend();

        checkUnregister();

        if(sm == SAVE)
            save();

        neuron = null;
    }

    public void save() {
        if(!neuron.isModified())
            return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            neuron.write(dos);

            model.getSuspensionCallback().store(
                    id,
                    neuron.getLabel(),
                    neuron.getCustomData(),
                    baos.toByteArray()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        neuron.resetModified();
    }

    private void reactivate() {
        assert model.getSuspensionCallback() != null;

        Neuron n;
        try (DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(
                        model.getSuspensionCallback().retrieve(id)
                )
        )) {
            n = Neuron.read(dis, model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        n.setProvider(this);
        n.reactivate(model);
        neuron = n;
        checkRegister();
    }

    public void linkInput(Synapse s, boolean force) {
        lock.acquireWriteLock();
        s.isInputLinked = true;
        if(force || !isSuspended())
            neuron.addOutputSynapse(s);

        addOutputSynapse(s);
        lock.releaseWriteLock();
    }

    public void unlinkInput(Synapse s, boolean force) {
        lock.acquireWriteLock();
        s.isInputLinked = false;
        if(force || !isSuspended())
            neuron.removeOutputSynapse(s);
        removeOutputSynapse(s);
        lock.releaseWriteLock();
    }

    public void linkOutput(Synapse s, boolean force) {
        lock.acquireWriteLock();
        s.isOutputLinked = true;
        if(force || !isSuspended())
            neuron.addInputSynapse(s);

        addInputSynapse(s);
        lock.releaseWriteLock();
    }

    public void unlinkOutput(Synapse s, boolean force) {
        lock.acquireWriteLock();
        s.isOutputLinked = false;
        if(force || !isSuspended())
            neuron.removeInputSynapse(s);

        removeOutputSynapse(s);
        lock.releaseWriteLock();
    }

    public void addInputSynapse(Synapse s) {
        lock.acquireWriteLock();
        activeInputSynapses.put(s.input.getId(), s);
        checkRegister();
        lock.releaseWriteLock();
    }

    public void removeInputSynapse(Synapse s) {
        lock.acquireWriteLock();
        activeInputSynapses.remove(s.input.getId());
        checkUnregister();
        lock.releaseWriteLock();
    }

    public void addOutputSynapse(Synapse s) {
        lock.acquireWriteLock();
        activeOutputSynapses.put(s.output.getId(), s);
        checkRegister();
        lock.releaseWriteLock();
    }

    public void removeOutputSynapse(Synapse s) {
        lock.acquireWriteLock();
        activeOutputSynapses.remove(s.output.getId());
        checkUnregister();
        lock.releaseWriteLock();
    }

    private void checkRegister() {
        if(!isRegistered && isReferenced()) {
            model.register(this);
            isRegistered = true;
        }
    }

    private void checkUnregister() {
        if(isRegistered && !isReferenced()) {
            model.unregister(this);
            isRegistered = false;
        }
    }

    private boolean isReferenced() {
        return permanent ||
                neuron != null ||
                !activeInputSynapses.isEmpty() ||
                !activeOutputSynapses.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return id.intValue() == ((NeuronProvider) o).id.intValue();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public int compareTo(NeuronProvider np) {
        return Long.compare(id, np.id);
    }

    public String toString() {
        if(this == MIN_NEURON) return "MIN_NEURON";
        if(this == MAX_NEURON) return "MAX_NEURON";

        return "p(" + (neuron != null ? neuron : id + ":" + "SUSPENDED") + ")";
    }

    public String toKeyString() {
        if(this == MIN_NEURON) return "MIN_NEURON";
        if(this == MAX_NEURON) return "MAX_NEURON";

        return "p(" + (neuron != null ? neuron.toKeyString() : id + ":" + "SUSPENDED") + ")";
    }
}