package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;

public class UpdateWeight implements LinkPhase {
    @Override
    public void process(Link l) {
        l.updateSynapse();
    }

    @Override
    public int getRank() {
        return 18;
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
