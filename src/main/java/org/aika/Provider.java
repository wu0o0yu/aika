package org.aika;


import org.aika.corpus.Document;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class Provider<T extends AbstractNode> implements Comparable<Provider<?>> {

    public Model model;
    public Integer id;

    private volatile T n;

    public Provider(Model model, int id) {
        this.model = model;
        this.id = id;

        synchronized (model.providers) {
            model.providers.put(this.id, new WeakReference<>(this));
        }
    }


    public Provider(Model model, T n) {
        this.model = model;
        this.n = n;

        id = model.suspensionHook != null ? model.suspensionHook.getNewId() : model.currentId.addAndGet(1);
        synchronized (model.providers) {
            model.providers.put(id, new WeakReference<>(this));

            if(n != null) {
                model.register(this);
            }
        }
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
        return n;
    }


    public synchronized T get(int lastUsedDocumentId) {
        T n = get();
        n.lastUsedDocumentId = Math.max(n.lastUsedDocumentId, lastUsedDocumentId);
        return n;
    }


    /**
     *
     * @param doc The document is used to remember when this node has been used last.
     * @return
     */
    public T get(Document doc) {
        return get(doc.id);
    }


    public synchronized void suspend() {
        if(n == null) return;

        assert model.suspensionHook != null;

        n.suspend();

        model.unregister(this);

        save();

        discard();
    }


    public synchronized void discard() {
        n = null;
    }


    public void save() {
        if (n.modified) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (
                    GZIPOutputStream gzipos = new GZIPOutputStream(baos);
                    DataOutputStream dos = new DataOutputStream(gzipos);) {

                n.write(dos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            model.suspensionHook.store(id, baos.toByteArray());
        }
        n.modified = false;
    }


    private void reactivate() {
        assert model.suspensionHook != null;

        byte[] data = model.suspensionHook.retrieve(id);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (
                GZIPInputStream gzipis = new GZIPInputStream(bais);
                DataInputStream dis = new DataInputStream(gzipis);) {
            n = (T) AbstractNode.read(dis, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        n.reactivate();

        model.register(this);
    }


    @Override
    public boolean equals(Object o) {
        return id == ((Provider<?>) o).id;
    }


    @Override
    public int hashCode() {
        return id;
    }


    public String toString() {
        return "p(" + id + ":" + (n != null ? n.toString() : "SUSPENDED") + ")";
    }


    public int compareTo(Provider<?> n) {
        if (id < n.id) return -1;
        else if (id > n.id) return 1;
        else return 0;
    }
}
