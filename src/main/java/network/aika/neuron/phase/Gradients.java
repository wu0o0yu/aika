package network.aika.neuron.phase;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class Gradients implements Phase {
    @Override
    public void process(Activation act) {
        act.processGradient();
    }

    @Override
    public Phase nextPhase() {
        return UPDATE_WEIGHTS;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public void tryToLink(Activation iAct, Activation oAct, Visitor c) {

    }

    @Override
    public void propagate(Activation act) {

    }

    @Override
    public int getRank() {
        return 5;
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return act2.getFired().compareTo(act1.getFired());
    }
}
