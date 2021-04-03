package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.steps.Phase;

public class UpdateBias implements ActivationStep {

    @Override
    public Phase getPhase() {
        return Phase.MAIN;
    }

    @Override
    public void process(Activation act) {
        act.getNeuron().updateBias(act);
    }

    public String toString() {
        return "Act-Step: Update Bias";
    }
}
