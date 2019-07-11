package network.aika.neuron.activation;


import java.util.ArrayDeque;

import network.aika.neuron.activation.Activation.OscillatingActivationsException;

import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.activation.Decision.UNKNOWN;

public class ValueQueue {
    private final ArrayDeque<Activation> queue = new ArrayDeque<>();


    public void propagateActivationValue(Activation act, SearchNode sn)  {
        act.getOutputLinks()
                .forEach(l -> add(l.getOutput(), sn));
    }


    public void add(Activation act, SearchNode sn) {
        if(act == null || act.currentOption.isQueued() || (act.getDecision() == UNKNOWN && act.getType() == EXCITATORY)) return;

        queue.add(act);
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

