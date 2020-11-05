package network.aika.neuron.phase;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class Gradients implements Phase {
    @Override
    public void process(Activation act) {
        act.processGradient();
    }

    @Override
    public boolean transition(Activation act) {
        act.setPhase(UPDATE_WEIGHTS);
        return false;
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
}
