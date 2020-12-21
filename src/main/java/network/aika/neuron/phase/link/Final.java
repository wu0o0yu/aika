package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.RankedImpl;

public class Final extends RankedImpl implements LinkPhase {


    public Final(int rank) {
        super(rank);
    }

    @Override
    public void process(Link l) {

    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
