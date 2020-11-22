package network.aika.neuron.phase;


import network.aika.neuron.activation.QueueEntry;

import java.util.Comparator;

public interface Phase<E extends QueueEntry> extends Comparator<E> {

    void process(E e);

    int getRank();

    static String toString(Phase p) {
        return " (" + (p != null ? p.getClass().getSimpleName() : "X") + ")";
    }
}
