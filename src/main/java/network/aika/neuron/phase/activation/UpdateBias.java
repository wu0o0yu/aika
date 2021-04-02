package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;

public class UpdateBias implements ActivationPhase {

    @Override
    public void process(Activation act) {
        act.getNeuron().updateBias(act);
    }

    public String toString() {
        return "Act-Phase: Update Bias";
    }
}
