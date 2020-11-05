package network.aika.neuron.phase;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class Counting implements Phase {
    @Override
    public void process(Activation act) {
        act.count();
    }

    @Override
    public boolean transition(Activation act) {
        act.setPhase(INDUCTION);
        return true;
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
}
