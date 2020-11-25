package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.phase.link.LinkPhase;

public class PrepareFinalLinking implements ActivationPhase {

    @Override
    public void process(Activation act) {
        act.updateForFinalPhase();
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public void tryToLink(Activation act, Visitor v) {

    }

    @Override
    public void propagate(Activation act, Visitor v) {

    }

    @Override
    public int getRank() {
        return 2;
    }

    @Override
    public int compare(Activation o1, Activation o2) {
        return 0;
    }
}
