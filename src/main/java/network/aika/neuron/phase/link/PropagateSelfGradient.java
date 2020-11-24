package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class PropagateSelfGradient implements LinkPhase {

    @Override
    public void process(Link l) {
        l.updateAndPropagateSelfGradient();
    }

    @Override
    public int getRank() {
        return 12;
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
