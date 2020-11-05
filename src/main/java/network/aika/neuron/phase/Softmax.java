package network.aika.neuron.phase;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class Softmax implements Phase {
    @Override
    public void process(Activation act) {
        act.computeBranchProbability();
    }

    @Override
    public Phase nextPhase() {
        return COUNTING;
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
        return 2;
    }
}
