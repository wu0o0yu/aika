package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class Induction implements LinkPhase {

    @Override
    public void process(Link link) {

    }

    @Override
    public int getRank() {
        return 17;
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
