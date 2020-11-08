package network.aika.neuron.phase;

import network.aika.Config;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class Final implements Phase {
    @Override
    public void process(Activation act) {

    }

    @Override
    public Phase nextPhase(Config c) {
        return null;
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
        return 9;
    }

    @Override
    public int compare(Activation o1, Activation o2) {
        return 0;
    }
}
