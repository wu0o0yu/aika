package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.steps.Phase;
import network.aika.utils.Utils;

public class SumUpBias implements ActivationStep {

    private double delta;

    public SumUpBias(double delta) {
        this.delta = delta;
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    public boolean checkIfQueued() {
        return false;
    }


    @Override
    public void process(Activation act) {
        act.changeNet(delta);

//        double delta = act.updateValue();

        QueueEntry.add(act, PROPAGATE_GRADIENTS_NET);
        QueueEntry.add(act, CHECK_IF_FIRED);
    }

    public String toString() {
        return "Act-Step: Sum up Bias (" + Utils.round(delta) + ")";
    }
}
