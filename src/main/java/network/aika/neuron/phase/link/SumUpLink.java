package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.RankedImpl;

public class SumUpLink extends RankedImpl implements LinkPhase {

    private Link oldLink;

    public SumUpLink(int round, Link ol) {
        super(round, SUM_UP_LINK_RANK);

        oldLink = ol;
    }

    @Override
    public void process(Link l) {
        Activation oAct = l.getOutput();

        oAct.sumUpLink(oldLink, l);
        oAct.checkIfFired();
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
