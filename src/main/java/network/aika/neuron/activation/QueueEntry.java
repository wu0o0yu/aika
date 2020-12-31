package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.phase.Phase;
import org.graphstream.graph.Graph;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Supplier;


public abstract class QueueEntry<P extends Phase> implements Comparable<QueueEntry<P>> {

    protected P phase;

    private TreeSet<P> pendingPhases = new TreeSet<>(Comparator.comparing(p -> p.getRank()));
    private boolean isQueued;
    private boolean marked;

    public abstract void updateGraphStreamElement();

    public void initPhases(P... initialPhases) {
        pendingPhases.addAll(Arrays.asList(initialPhases));
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public void addPhase(QueueEntry<P> e, P... p) {
        pendingPhases.addAll(Arrays.asList(p));

        queueNextPhase(e);
    }

    public void queueNextPhase(QueueEntry<P> e) {
        if(pendingPhases.isEmpty()) {
            e.setPhase(null);
            return;
        }

        updateQueueEntry(() -> {
            e.setPhase(pendingPhases.first());
            return e;
        });
    }

    public void updateQueueEntry(Supplier<QueueEntry<P>> newEntrySupplier) {
        removeFromQueue();
        QueueEntry<P> newEntry = newEntrySupplier.get();
        addToQueue(newEntry);
    }

    private void addToQueue(QueueEntry<P> newEntry) {
        newEntry.getThought().addToQueue(newEntry);
        isQueued = true;
    }

    private void removeFromQueue() {
        if(isQueued()) {
            getThought().removeActivationFromQueue(this);
            isQueued = true;
        }
    }

    public boolean isQueued() {
        return isQueued;
    }

    public void removePendingPhase() {
        isQueued = false;
        pendingPhases.pollFirst();
    }

    public void clearPendingPhases() {
        pendingPhases.clear();
    }

    public void copyPhases(QueueEntry<P> oldEntry) {
        pendingPhases.addAll(oldEntry.pendingPhases);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        pendingPhases.forEach(p -> sb.append(p.getClass().getSimpleName() + ", "));

        return sb.toString();
    }

    public void addToQueue(P... p) {
        if(p.length == 0) {
            return;
        }

        addPhase(this, p);
    }

    public P getPhase() {
        return phase;
    }

    public void setPhase(P p) {
        phase = p;
    }

    public abstract boolean isActive();

    public abstract Thought getThought();

    public void process() {
        removePendingPhase();

        getPhase().process(this);

        queueNextPhase(this);
    }

    @Override
    public int compareTo(QueueEntry<P> qe) {
        int r = Integer.compare(getPhase().getRank(), qe.getPhase().getRank());
        if(r != 0) return r;
        r = getPhase().compare(this, qe);
        if(r != 0) return r;
        return innerCompareTo(qe);
    }

    protected abstract int innerCompareTo(QueueEntry<P> qe);
}
