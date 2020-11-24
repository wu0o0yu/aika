package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class OutputGradient implements LinkPhase {
    @Override
    public void process(Link l) {
        l.computeOutputGradient();
    }

    @Override
    public int getRank() {
        return 9;
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
