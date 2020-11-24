package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class PropagateGradient implements LinkPhase {

    private double gradient;

    public PropagateGradient(double gradient) {
        this.gradient = gradient;
    }

    @Override
    public void process(Link l) {
        l.propagateGradient(gradient);
    }

    @Override
    public int getRank() {
        return 13;
    }

    @Override
    public int compare(Link o1, Link o2) {
        return 0;
    }
}
