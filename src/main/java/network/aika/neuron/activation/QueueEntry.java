package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.phase.Phase;


public abstract class QueueEntry<P extends Phase> implements Comparable<QueueEntry<P>> {

    protected P phase;
    protected QueueState<P> queueState;


    public void addToQueue(P... p) {
        if(p.length == 0) {
            return;
        }

        queueState.addPhase(this, p);
    }

    public void updateThoughtQueue() {
        queueState.updateThoughtQueue();
    }

    public P getPhase() {
        return phase;
    }

    public void setPhase(P p) {
        phase = p;
    }

    public boolean isMarked() {
        return queueState.isMarked();
    }

    public void setMarked(boolean marked) {
        queueState.setMarked(marked);
    }

    public abstract boolean isActive();

    public abstract Thought getThought();

    public void process() {
        queueState.removePendingPhase();

        getPhase().process(this);

        queueState.updateThoughtQueue();
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
