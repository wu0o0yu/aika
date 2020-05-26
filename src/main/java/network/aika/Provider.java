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

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author Lukas Molzberger
 */
public class Provider<T extends AbstractNode> implements Comparable<Provider<?>> {

    private Model model;
    private Long id;

    private volatile T n;

    public enum SuspensionMode {
        SAVE,
        DISCARD
    }

    public Provider(Model model, long id) {
        this.model = model;
        this.id = id;

        if(model != null) {
            model.registerProvider(this);
        }
    }

    public Provider(Model model, T n) {
        this.model = model;
        this.n = n;

        id = model.createNeuronId();
        model.registerProvider(this);
    }

    public Long getId() {
        return id;
    }

    public Model getModel() {
        return model;
    }

    public boolean isSuspended() {
        return n == null;
    }

    public T getIfNotSuspended() {
        return n;
    }

    public synchronized T get() {
        if (n == null) {
            reactivate();
        }
        n.retrievalCount = model.getCurrentRetrievalCount();
        return n;
    }

    public synchronized void suspend(SuspensionMode sm) {
        if(n == null) return;
        assert model.getSuspensionHook() != null;
        n.suspend();

        if(sm == SuspensionMode.SAVE) {
            save();
        }

        n = null;
    }

    public void save() {
        if (n.modified) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (
                    GZIPOutputStream gzipos = new GZIPOutputStream(baos);
                    DataOutputStream dos = new DataOutputStream(gzipos)) {

                n.write(dos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            model.getSuspensionHook().store(id, baos.toByteArray());
        }
        n.modified = false;
    }

    private void reactivate() {
        assert model.getSuspensionHook() != null;

        byte[] data = model.getSuspensionHook().retrieve(id);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (
                GZIPInputStream gzipis = new GZIPInputStream(bais);
                DataInputStream dis = new DataInputStream(gzipis);) {
            n = (T) AbstractNode.read(dis, this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        n.reactivate();

        model.incrementRetrievalCounter();
    }

    @Override
    public boolean equals(Object o) {
        return id == ((Provider<?>) o).id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        return "p(" + id + ":" + (n != null ? n.toString() : "SUSPENDED") + ")";
    }

    public int compareTo(Provider<?> n) {
        return Long.compare(id, n.id);
    }
}
