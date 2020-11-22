package network.aika.neuron.activation;

import network.aika.neuron.phase.Phase;


public abstract class QueueEntry<P extends Phase> {

    protected P phase; // = INITIAL_LINKING;
    protected QueueState queueState;


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


    public void process() {
        queueState.removePendingPhase();

        phase.process(this);

        if(isActive()) {
            queueState.updateThoughtQueue();
        }
    }
}
