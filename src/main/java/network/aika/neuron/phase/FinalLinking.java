package network.aika.neuron.phase;

import network.aika.neuron.activation.Activation;

public class FinalLinking extends Linking {

    @Override
    public void process(Activation act) {
        act.updateForFinalPhase();
    }

    @Override
    public Phase nextPhase() {
        return SOFTMAX;
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public int getRank() {
        return 1;
    }
}
