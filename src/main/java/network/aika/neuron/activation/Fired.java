package network.aika.neuron.activation;

import network.aika.neuron.INeuron;


public class Fired implements Comparable<Fired> {


    public final static Fired MIN = new Fired(0, 0);
    public final static Fired MAX = new Fired(Integer.MAX_VALUE, Integer.MAX_VALUE);


    private final int inputTimestamp;
    private final int fired;

    public Fired(int inputTimestamp, int fired) {
        this.inputTimestamp = inputTimestamp;
        this.fired = fired;
    }


    public int getInputTimestamp() {
        return inputTimestamp;
    }

    public int getFired() {
        return fired;
    }

    public static Fired max(Fired a, Fired b) {
        if(a == null) return b;
        if(b == null) return a;
        return a.compareTo(b) > 0 ? a : b;
    }


    public static Fired min(Fired a, Fired b) {
        if(a == null) return b;
        if(b == null) return a;
        return a.compareTo(b) < 0 ? a : b;
    }


    @Override
    public int compareTo(Fired f) {
        int r = Integer.compare(inputTimestamp, f.inputTimestamp);
        if(r != 0) return r;

        return Integer.compare(fired, f.fired);
    }


    public String toString() {
        return "[" + inputTimestamp + "," + fired + "]";
    }
}
