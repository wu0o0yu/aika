package network.aika.neuron.phase;

import network.aika.neuron.activation.Activation;

import static network.aika.neuron.activation.Direction.OUTPUT;

public class FinalLinking extends Linking {

    @Override
    public Phase nextPhase() {
        return SOFTMAX;
    }

    public boolean isFinal() {
        return true;
    }

    @Override
    public void propagate(Activation act) {
        act.propagateIntern();
    }

    @Override
    public int getRank() {
        return 2;
    }
}
