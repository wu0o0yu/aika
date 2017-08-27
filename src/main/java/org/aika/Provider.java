package org.aika;


import java.io.*;
import org.aika.lattice.Node;
import org.aika.neuron.Neuron;


public class Provider<T extends Writable> implements Comparable<Provider<?>> {

    public Model m;
    public final int id;
    public final Type type;

    private T obj;


    public enum Type {
        NEURON,
        NODE
    }


    public Provider(Model m, int id, Type t, T obj) {
        this.m = m;
        this.id = id;
        this.type = t;
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
            switch(type) {
                case NEURON:
                    obj = (T) Neuron.read(dis, this);
                    break;
                case NODE:
                    obj = (T) Node.read(dis, this);
                    break;
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
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
