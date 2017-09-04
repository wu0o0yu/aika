package org.aika;


import java.io.*;


public class Provider<T extends AbstractNode> implements Comparable<Provider<?>> {

    public Model m;
    public final int id;

    private T n;


    public Provider(Model m, int id, T n) {
        this.m = m;
        this.id = id;
        this.n = n;
    }


    public synchronized boolean isSuspended() {
        return n == null;
    }


    public synchronized T get() {
        if(n == null) {
            reactivate();
        }
        return n;
    }


    public synchronized void suspend() {
        assert m.suspensionHook != null;

        if(n.modified) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            try {
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
        DataInputStream dis = new DataInputStream(bais);
        try {
            n = (T) AbstractNode.read(dis, this);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        synchronized (m.providersInMemory) {
            m.providersInMemory.put(id, this);
        }
    }


    public String toString() {
        return "p(" + id + ":" + (n != null ? n.toString() : "SUSPENDED") + ")";
    }


    public int compareTo(Provider<?> n) {
        if(id < n.id) return -1;
        else if(id > n.id) return 1;
        else return 0;
    }
}
