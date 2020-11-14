package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.phase.Phase;

import java.util.Comparator;
import java.util.TreeSet;

public class QueueState {

    private TreeSet<Phase> pendingPhases = new TreeSet<>(Comparator.comparing(p -> p.getRank()));;
    private Activation actToQueue;
    private Activation queuedAct;

    private QueueState() {
    }

    public QueueState(Activation act) {
        actToQueue = act;
        pendingPhases.addAll(act.getThought().getConfig().getPhases());
    }

    public void setActToQueue(Activation actToQueue) {
        this.actToQueue = actToQueue;
    }

    public void addPhase(Activation act, Phase p) {
        actToQueue = act;
        pendingPhases.add(p);

        updateThoughtQueue();
    }

    public void updateThoughtQueue() {
        if(pendingPhases.isEmpty())
            return;

        Thought t = actToQueue.getThought();
        if(queuedAct != null) {
            t.removeActivationFromQueue(queuedAct);
        }
        actToQueue.setPhase(pendingPhases.first());
        t.addActivationToQueue(actToQueue);
        queuedAct = actToQueue;
    }

    public void removePendingPhase() {
        queuedAct = null;
        pendingPhases.pollFirst();
    }

    public QueueState copy(Activation newAct) {
        QueueState qs = new QueueState();
        qs.pendingPhases.addAll(pendingPhases);
        qs.actToQueue = newAct;
        return qs;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        pendingPhases.forEach(p -> sb.append(p.getClass().getSimpleName() + ", "));

        return sb.toString();
    }
}
