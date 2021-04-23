package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.steps.Phase;
import network.aika.utils.Utils;

public class UpdateBias implements ActivationStep {

    public UpdateBias(double biasDelta) {
        this.biasDelta = biasDelta;
    }

    private double biasDelta;

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    public boolean checkIfQueued() {
        return false;
    }

    @Override
    public void process(Activation act) {
        act.getNeuron().updateBias(biasDelta);
    }

    public String toString() {
        return "Act-Step: Update Bias (" + Utils.round(biasDelta) + ")";
    }
}
