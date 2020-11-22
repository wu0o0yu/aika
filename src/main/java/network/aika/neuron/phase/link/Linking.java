package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class Linking implements LinkPhase {

    @Override
    public void process(Link l) {
        l.propagate();
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
