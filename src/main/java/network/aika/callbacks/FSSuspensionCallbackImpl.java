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
package network.aika.callbacks;

import network.aika.utils.Writable;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Lukas Molzberger
 */
public class FSSuspensionCallbackImpl implements SuspensionCallback {

    private AtomicLong currentId = new AtomicLong(0);

    private Map<String, Long> labels = Collections.synchronizedMap(new TreeMap<>());
    private Map<Long, long[]> index = Collections.synchronizedMap(new TreeMap<>());

    private File path;
    private String modelLabel;

    private RandomAccessFile dataStore;


    public void open(File path, String modelLabel, boolean create) throws FileNotFoundException {
        this.path = path;
        this.modelLabel = modelLabel;
        if(create) {
            getFile("model").deleteOnExit();
            getFile("index").deleteOnExit();
        } else {
            loadIndex();
        }
        dataStore = new RandomAccessFile(getFile("model"), "rw");
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
        if (label == null)
            return;

        labels.remove(label);
    }

    @Override
    public long createId() {
        return currentId.addAndGet(1);
    }

    @Override
    public synchronized void store(Long id, String label, Writable customData, byte[] data) throws IOException {
        dataStore.seek(dataStore.length());

        index.put(id, new long[]{(int) dataStore.getFilePointer(), data.length});
        dataStore.write(data);
    }

    @Override
    public synchronized byte[] retrieve(Long id) throws IOException {
        long[] pos = index.get(id);
        if(pos == null)
            throw new MissingNeuronException(String.format("Neuron with id %d is missing in model label %d", id, modelLabel));

        byte[] data = new byte[(int)pos[1]];

        dataStore.seek(pos[0]);
        dataStore.read(data);

        return data;
    }

    @Override
    public synchronized void remove(Long id) {
        index.remove(id);
    }

    @Override
    public Collection<Long> getAllIds() {
        return index.keySet();
    }


    @Override
    public void loadIndex() {
        try (FileInputStream fis = new FileInputStream(getFile("index"));
             ByteArrayInputStream bais = new ByteArrayInputStream(fis.readAllBytes());
             DataInputStream dis = new DataInputStream(bais)) {
            readIndex(dis);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void storeIndex() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (DataOutputStream dos = new DataOutputStream(baos);
             FileOutputStream fos = new FileOutputStream(getFile("index"))) {
            writeIndex(dos);
            fos.write(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getFile(String prefix) {
        return new File(path, prefix + "-" + modelLabel + ".dat");
    }

    private void readIndex(DataInput in) throws IOException {
        currentId = new AtomicLong(in.readLong());

        labels.clear();
        while(in.readBoolean()) {
            String l = in.readUTF();
            Long id = in.readLong();
            labels.put(l, id);
        }

        index.clear();
        while(in.readBoolean()) {
            Long id = in.readLong();
            long[] pos = new long[2];
            pos[0] = in.readLong();
            pos[1] = in.readInt();

            index.put(id, pos);
        }
    }

    private void writeIndex(DataOutput out) throws IOException {
        out.writeLong(currentId.get());

        for(Map.Entry<String, Long> me: labels.entrySet()) {
            out.writeBoolean(true);
            out.writeUTF(me.getKey());
            out.writeLong(me.getValue());
        }
        out.writeBoolean(false);

        for(Map.Entry<Long, long[]> me: index.entrySet()) {
            out.writeBoolean(true);
            out.writeLong(me.getKey());
            out.writeLong(me.getValue()[0]);
            out.writeInt((int)me.getValue()[1]);
        }
        out.writeBoolean(false);
    }
}
