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

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The {@code NeuronProvider} class is a proxy implementation for the real neuron implementation in the class {@code Neuron}.
 * Aika uses the provider pattern to store and reload rarely used neurons or logic nodes.
 *
 * @author Lukas Molzberger
 */
public class NeuronProvider implements Comparable<NeuronProvider> {

    public static boolean ENABLE_COMPRESSION = false;

    public static final NeuronProvider MIN_NEURON = new NeuronProvider(Long.MIN_VALUE);
    public static final NeuronProvider MAX_NEURON = new NeuronProvider(Long.MAX_VALUE);

    private Model model;
    private Long id;

    private volatile Neuron neuron;

    public NeuronProvider(long id) {
        this.id = id;
    }

    public NeuronProvider(Model model, long id) {
        assert model != null;

        this.model = model;
        this.id = id;

        model.registerWeakReference(this);
    }

    public NeuronProvider(Model model, Neuron n) {
        assert model != null && n != null;

        this.model = model;
        this.neuron = n;

        id = model.createNeuronId();
        model.registerWeakReference(this);
        model.register(this);
    }

    public Neuron getNeuron() {
        if (neuron == null) {
            reactivate();
        }
        if(model != null) {
            neuron.retrievalCount = model.getCurrentRetrievalCount();
        }
        return neuron;
    }

    public void setNeuron(Neuron<?> n) {
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

    public synchronized void suspend(SuspensionMode sm) {
        if(neuron == null) return;
        assert model.getSuspensionHook() != null;
        neuron.suspend();

        model.unregister(this);

        if(sm == SuspensionMode.SAVE) {
            save();
        }

        neuron = null;
    }

    public void save() {
        if (neuron.isModified()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream dos = getDataOutputStream(
                    baos,
                    ENABLE_COMPRESSION
            )) {
                neuron.write(dos);

                model.getSuspensionHook().store(
                        id,
                        neuron.getLabel(),
                        neuron.getCustomData(),
                        baos.toByteArray()
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        neuron.setModified(false);
    }

    private void reactivate() {
        assert model.getSuspensionHook() != null;

        try (DataInputStream dis = getDataInputStream(
                model.getSuspensionHook().retrieve(id),
                ENABLE_COMPRESSION
        )) {
            neuron = Neuron.read(dis, model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        neuron.reactivate();
        model.register(this);

        model.incrementRetrievalCounter();
    }

    private DataOutputStream getDataOutputStream(OutputStream os, boolean compressed) throws IOException {
        if(compressed)
            os = new GZIPOutputStream(os);

        return new DataOutputStream(os);
    }

    private DataInputStream getDataInputStream(byte[] data, boolean compressed) throws IOException {
        InputStream is = new ByteArrayInputStream(data);
        if(compressed)
            is = new GZIPInputStream(is);

        return new DataInputStream(is);
    }

    @Override
    public boolean equals(Object o) {
        return id == ((NeuronProvider) o).id;
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
}