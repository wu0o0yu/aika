package org.aika;


import java.io.*;


public class Provider<T extends AbstractNode> implements Comparable<Provider<?>> {

    public Model m;
    public final int id;

    private T obj;


    public Provider(Model m, int id, T obj) {
        this.m = m;
        this.id = id;
        this.obj = obj;
    }


    public synchronized boolean isSuspended() {
        return obj == null;
    }


    public synchronized T get() {
        if(obj == null) {
            reactivate();
        }
        return obj;
    }


    public synchronized void suspend() {
        assert m.suspensionHook != null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            obj.write(dos);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        m.suspensionHook.store(id, baos.toByteArray());

        obj = null;
    }


    private void reactivate() {
        assert m.suspensionHook != null;

        byte[] data = m.suspensionHook.retrieve(id);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        try {
            obj = (T) AbstractNode.read(dis, this);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        synchronized (m.providersInMemory) {
            m.providersInMemory.put(id, this);
        }
    }


    public String toString() {
        return "p(" + id + ":" + (obj != null ? obj.toString() : "SUSPENDED") + ")";
    }


    public int compareTo(Provider<?> n) {
        if(id < n.id) return -1;
        else if(id > n.id) return 1;
        else return 0;
    }
}
