package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.phase.Phase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public class QueueState<P extends Phase> {

    private TreeSet<P> pendingPhases = new TreeSet<>(Comparator.comparing(p -> p.getRank()));;
    private QueueEntry<P> entryToQueue;
    private QueueEntry<P> queuedAct;
    private boolean marked;

    private QueueState() {
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

    public void addPhase(QueueEntry<P>  act, P... p) {
        entryToQueue = act;
        pendingPhases.addAll(Arrays.asList(p));

        updateThoughtQueue();
    }

    public void updateThoughtQueue() {
        if(pendingPhases.isEmpty())
            return;

        Thought t = entryToQueue.getThought();
        if(queuedAct != null) {
            t.removeActivationFromQueue(queuedAct);
        }
        entryToQueue.setPhase(pendingPhases.first());
        t.addToQueue(entryToQueue);
        queuedAct = entryToQueue;
    }

    public void removePendingPhase() {
        queuedAct = null;
        pendingPhases.pollFirst();
    }

    public QueueState copy(Activation newAct) {
        QueueState qs = new QueueState();
        qs.pendingPhases.addAll(pendingPhases);
        qs.entryToQueue = newAct;
        return qs;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        pendingPhases.forEach(p -> sb.append(p.getClass().getSimpleName() + ", "));

        return sb.toString();
    }
}
