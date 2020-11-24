package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class PropagateOutputGradient implements LinkPhase {

    @Override
    public void process(Link l) {
        l.getOutput().addInputGradient(l.getOutputGradient());
    }

    @Override
    public int getRank() {
        return 11;
    }

    @Override
    public int compare(Link o1, Link o2) {
        return 0;
    }
}
