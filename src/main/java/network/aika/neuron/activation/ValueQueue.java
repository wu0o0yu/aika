package network.aika.neuron.activation;

import network.aika.Document;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation.OscillatingActivationsException;

import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.activation.Decision.UNKNOWN;

public class ValueQueue {
    private final ArrayDeque<Activation> queue = new ArrayDeque<>();


    public void propagateActivationValue(Activation act)  {
        act.getOutputLinks()
                .forEach(l -> add(l.getOutput()));
    }


    public void add(Activation act) {
        if(act.currentOption.isQueued() || (act.getDecision() == UNKNOWN && act.getType() == EXCITATORY)) return;

        queue.add(act);
        act.currentOption.setQueued(true);
    }


    public double process(Document doc, SearchNode sn) throws OscillatingActivationsException {
        add(sn.getActivation());

        double delta = 0.0;
        while (!queue.isEmpty()) {
            Activation act = queue.pollFirst();
            act.currentOption.setQueued(false);

            delta += act.process(sn);
        }
        return delta;
    }
}

