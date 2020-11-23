package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.phase.Phase;


public abstract class QueueEntry<P extends Phase> {

    protected P phase; // = INITIAL_LINKING;
    protected QueueState<P> queueState;


    public void addToQueue(P... p) {
        if(p.length == 0) {
            return;
        }

        queueState.addPhase(this, p);
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

        phase.process(this);

        if(isActive()) {
            queueState.updateThoughtQueue();
        }
    }
}
