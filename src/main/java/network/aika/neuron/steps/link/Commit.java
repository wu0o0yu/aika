package network.aika.neuron.steps.link;

import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.steps.Phase;

public class Commit implements LinkStep {

    @Override
    public void process(Link l) {
        if(l.getOutput().getFired() != Fired.NOT_FIRED)
            return;

        l.unlinkInput();
        l.unlinkOutput();
    }

    @Override
    public Phase getPhase() {
        return Phase.COUNTING;
    }
}
