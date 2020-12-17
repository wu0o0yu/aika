package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.RankedImpl;

import network.aika.neuron.phase.activation.ActivationPhase;

public class Template extends RankedImpl implements LinkPhase {

    public Template(int rank) {
        super(rank);
    }

    @Override
    public void process(Link l) {
        l.propagate(ActivationPhase.TEMPLATE);
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
