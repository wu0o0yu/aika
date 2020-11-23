package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class Softmax implements ActivationPhase {
    @Override
    public void process(Activation act) {
        act.computeBranchProbability();
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
        return 5;
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return 0;
    }
}
