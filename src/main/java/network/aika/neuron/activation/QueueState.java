package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.phase.Phase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public class QueueState<P extends Phase> {

    private TreeSet<P> pendingPhases = new TreeSet<>(Comparator.comparing(p -> p.getRank()));
    private QueueEntry<P> entryToQueue;
    private QueueEntry<P> queuedEntry;
    private boolean marked;

    public QueueState() {
    }

    public QueueState(QueueEntry<P> entry, P... initialPhases) {
        entryToQueue = entry;
        pendingPhases.addAll(Arrays.asList(initialPhases));
    }

    public QueueState(QueueEntry<P> entry, Collection<P> initialPhases) {
        entryToQueue = entry;
        pendingPhases.addAll(initialPhases);
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public void setEntryToQueue(QueueEntry<P>  entryToQueue) {
        this.entryToQueue = entryToQueue;
    }

    public void addPhase(QueueEntry<P>  e, P... p) {
        entryToQueue = e;
        pendingPhases.addAll(Arrays.asList(p));

        updateThoughtQueue();
    }

    public void updateThoughtQueue() {
        if(pendingPhases.isEmpty())
            return;

        P nextPhase = pendingPhases.first();
        Thought t = entryToQueue.getThought();
        if(queuedEntry != null) {
            if(queuedEntry.getPhase() == nextPhase)
                return;

            t.removeActivationFromQueue(queuedEntry);
        }
        entryToQueue.setPhase(nextPhase);
        t.addToQueue(entryToQueue);
        queuedEntry = entryToQueue;
    }

    public void removePendingPhase() {
        queuedEntry = null;
        pendingPhases.pollFirst();
    }

    public QueueState copy(QueueEntry<P> newEntry) {
        QueueState qs = new QueueState();
        qs.pendingPhases.addAll(pendingPhases);
        qs.entryToQueue = newEntry;
        return qs;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        pendingPhases.forEach(p -> sb.append(p.getClass().getSimpleName() + ", "));

        return sb.toString();
    }
}
