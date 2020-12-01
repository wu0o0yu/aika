package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class Induction implements ActivationPhase {
    @Override
    public void process(Activation act) {
        System.out.println();
    }

    @Override
    public int getRank() {
        return 16;
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
        System.out.println();
    }

    @Override
    public int compare(Activation o1, Activation o2) {
        return 0;
    }
}
