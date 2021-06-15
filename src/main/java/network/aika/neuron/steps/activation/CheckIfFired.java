package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.steps.Phase;

public class CheckIfFired implements ActivationStep {

    @Override
    public void process(Activation act) {
        act.updateValue();

        if(!act.checkIfFired())
            return;

        act.propagate();
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    public boolean checkIfQueued() {
        return true;
    }

    public String toString() {
        return "Act-Step: CheckIfFired";
    }
}
