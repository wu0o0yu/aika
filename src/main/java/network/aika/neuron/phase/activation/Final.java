package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.phase.link.LinkPhase;

public class Final implements ActivationPhase {
    @Override
    public void process(Activation act) {

    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public void tryToLink(Activation act, Visitor v) {

    }

    @Override
    public void propagate(Activation act, Visitor v) {

    }

    @Override
    public int getRank() {
        return 20;
    }

    @Override
    public int compare(Activation o1, Activation o2) {
        return 0;
    }
}
