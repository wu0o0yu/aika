package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;

public class UpdateBias implements ActivationStep {

    @Override
    public void process(Activation act) {
        act.getNeuron().updateBias(act);
    }

    public String toString() {
        return "Act-Step: Update Bias";
    }
}
