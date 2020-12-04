package network.aika.neuron.phase.link;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;

public class Induction implements LinkPhase {

    @Override
    public void process(Link link) {
        assert link.getSynapse().isTemplate();

        Synapse inducedSynapse = link.getSynapse().instantiateTemplate(link.getInput().getNeuron(), link.getOutput().getNeuron());

        link.setSynapse(inducedSynapse);
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
