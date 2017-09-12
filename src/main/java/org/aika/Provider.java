package org.aika;


import java.io.*;
import java.lang.ref.WeakReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class Provider<T extends AbstractNode> implements Comparable<Provider<?>> {

    public Model m;
    public int id;

    private T n;

    public Provider(Model m, int id) {
        this.m = m;
        this.id = id;
    }


    public Provider(Model m, T n) {
        this.m = m;
        this.n = n;

        id = m.suspensionHook != null ? m.suspensionHook.getNewId() : m.currentId.addAndGet(1);
        synchronized (m.providers) {
            m.providers.put(id, new WeakReference<>(this));

            if(m.suspensionManager != null) {
                m.suspensionManager.register(this);
            }
        }
    }


    public void setModified() {
        n.modified = true;
    }


    public synchronized boolean isSuspended() {
        return n == null;
    }


    public synchronized T get() {
        if (n == null) {
            reactivate();
        }
        return n;
    }


    public synchronized void suspend() {
        assert m.suspensionHook != null;

        n.suspend();

        if (n.modified) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (
                    GZIPOutputStream gzipos = new GZIPOutputStream(baos);
                    DataOutputStream dos = new DataOutputStream(gzipos);) {

                n.write(dos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            m.suspensionHook.store(id, baos.toByteArray());
        }
        n = null;
    }


    private void reactivate() {
        assert m.suspensionHook != null;

        byte[] data = m.suspensionHook.retrieve(id);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (
                GZIPInputStream gzipis = new GZIPInputStream(bais);
                DataInputStream dis = new DataInputStream(gzipis);) {
            n = (T) AbstractNode.read(dis, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        n.reactivate();

        synchronized (m.providers) {
            m.providers.put(id, new WeakReference<>(this));
            m.suspensionManager.register(this);
        }
    }


    @Override
    public void finalize() {
        synchronized(m.providers) {
            m.providers.remove(id);
        }
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
