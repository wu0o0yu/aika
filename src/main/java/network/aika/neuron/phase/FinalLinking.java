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
        act.setPhase(FINAL_LINKING);
        return true;
    }

    @Override
    public boolean isFinal() {
        return true;
    }
}
