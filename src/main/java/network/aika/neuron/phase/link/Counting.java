package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class Counting implements LinkPhase {

    @Override
    public void process(Link l) {
        l.count();
    }

    @Override
    public int getRank() {
        return 6;
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
