package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class GradientDependencies implements LinkPhase {
    @Override
    public void process(Link l) {
        l.removeGradientDependencies();
    }

    @Override
    public int getRank() {
        return 10;
    }

    @Override
    public int compare(Link o1, Link o2) {
        return 0;
    }
}
