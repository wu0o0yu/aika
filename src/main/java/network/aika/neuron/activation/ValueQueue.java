package network.aika.neuron.activation;

import network.aika.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

public class ValueQueue {
    private final ArrayList<TreeSet<Activation>> queue = new ArrayList<>();

    private static Comparator<Activation> VALUE_QUEUE_COMP = (a, b) -> {
        int r = Integer.compare(a.getSequence(), b.getSequence());
        if(r != 0) return r;
        return Integer.compare(a.getId(), b.getId());
    };


    public void propagateActivationValue(int round, Activation act)  {
        act.getOutputLinks()
                .forEach(l -> add(l.synapse.isRecurrent ? round + 1 : round, l.output));
    }


    private void add(Activation act) {
        if(act == null) return;

        add(0, act);
        act.getOutputLinks()
                .filter(l -> l.synapse.isRecurrent)
                .forEach(l -> add(0, l.output));
    }


    public void add(int round, Activation act) {
        if(act.rounds.isQueued(round) || act.decision == SearchNode.Decision.UNKNOWN) return;

        TreeSet<Activation> q;
        if(round < queue.size()) {
            q = queue.get(round);
        } else {
            assert round == queue.size();
            q = new TreeSet<>(VALUE_QUEUE_COMP);
            queue.add(q);
        }

        act.rounds.setQueued(round, true);
        q.add(act);
    }


    public double process(Document doc, SearchNode sn) {
        long v = doc.getNewVisitedId();

        add(sn.getActivation());

        double delta = 0.0;
        for(int round = 0; round < queue.size(); round++) {
            TreeSet<Activation> q = queue.get(round);
            while (!q.isEmpty()) {
                Activation act = q.pollFirst();
                act.rounds.setQueued(round, false);

                delta += act.process(sn, round, v);
            }
        }
        return delta;
    }
}

