package network.aika.neuron.phase;

import network.aika.neuron.activation.Activation;

public class FinalLinking extends Linking {

    @Override
    public void process(Activation act) {
        act.updateForFinalPhase();
        act.process();
    }

    @Override
    public boolean transition(Activation act) {
        act.setPhase(SOFTMAX);
        return true;
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
