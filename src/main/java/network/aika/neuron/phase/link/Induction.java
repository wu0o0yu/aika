package network.aika.neuron.phase.link;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;

public class Induction implements LinkPhase {

    @Override
    public void process(Link l) {
        assert l.getSynapse().isTemplate();

        Synapse inducedSynapse = l.getSynapse()
                .instantiateTemplate(
                        l.getInput().getNeuron(),
                        l.getOutput().getNeuron()
                );

        l.setSynapse(inducedSynapse);
    }

    @Override
    public int getRank() {
        return 16;
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
