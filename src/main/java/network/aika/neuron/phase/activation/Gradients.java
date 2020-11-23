package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class Gradients implements ActivationPhase {
    @Override
    public void process(Activation act) {
        act.processGradient();
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
        return 6;
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return act2.getFired().compareTo(act1.getFired());
    }
}
