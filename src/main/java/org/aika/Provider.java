package org.aika;


import java.io.*;
import java.lang.ref.WeakReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class Provider<T extends AbstractNode> implements Comparable<Provider<?>> {

    public Model m;
    public int id;

    public boolean deleted;

    private T n;

    public Provider(Model m, int id) {
        this.m = m;
        this.id = id;

        synchronized (m.providers) {
            WeakReference wr = m.providers.put(id, new WeakReference(this));
            if(wr != null && wr.get() != null) {
                System.out.println();
            }
            assert wr == null;
        }
    }


    public Provider(Model m, T n) {
        this.m = m;
        this.n = n;

        id = m.suspensionHook != null ? m.suspensionHook.getNewId() : m.currentId.addAndGet(1);
        synchronized (m.providers) {
            WeakReference wr = m.providers.put(id, new WeakReference<>(this));
            assert wr == null;

            if(n != null) {
                m.register(this);
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
        if(n == null) return;

        assert m.suspensionHook != null;

        n.suspend();

        m.unregister(this);

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

        m.register(this);
    }


    @Override
    public void finalize() {
        deleted = true;
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
