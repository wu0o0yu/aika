package network.aika.neuron.activation;


import java.util.ArrayDeque;

import network.aika.neuron.activation.Activation.OscillatingActivationsException;
import network.aika.neuron.activation.search.SearchNode;

import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.INeuron.Type.INHIBITORY;
import static network.aika.neuron.activation.search.Decision.UNKNOWN;

public class ValueQueue {
    private final ArrayDeque<Activation> queue = new ArrayDeque<>();


    public void propagateActivationValue(Activation act, SearchNode sn, boolean lowerBoundChange, boolean upperBoundChange)  {
        if(!lowerBoundChange && !upperBoundChange)
            return;

        act.getOutputLinks()
                .filter(l -> l.getOutput().needsPropagation(sn, lowerBoundChange, upperBoundChange))
                .forEach(l -> add(l.getOutput(), sn));
    }


    public void add(Activation act, SearchNode sn) {
        if(act == null || act.currentOption.isQueued()) return;

        if(act.getNextDecision(act.currentOption, sn) == UNKNOWN && act.getType() == EXCITATORY) return;

        if(act.getType() == INHIBITORY) {
            queue.addFirst(act);
        } else {
            queue.addLast(act);
        }

        act.currentOption.setQueued(true);
    }


    public double process(SearchNode sn) throws OscillatingActivationsException {
        add(sn.getActivation(), sn);

        double delta = 0.0;
        while (!queue.isEmpty()) {
            Activation act = queue.pollFirst();
            act.currentOption.setQueued(false);

            delta += act.process(sn);
        }
        return delta;
    }
}

