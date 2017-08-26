package org.aika;


public class Provider<T> implements Comparable<Provider<?>> {

    public Model m;
    public final int id;
    public T obj;


    public Provider(Model m, int id, T obj) {
        this.m = m;
        this.id = id;
        this.obj = obj;
    }


    public T get() {
        if(obj != null) {
            return obj;
        }
        return null;
    }


    public String toString() {
        return "p(" + obj.toString() + ")";
    }


    public int compareTo(Provider<?> n) {
        if(id < n.id) return -1;
        else if(id > n.id) return 1;
        else return 0;
    }
}
