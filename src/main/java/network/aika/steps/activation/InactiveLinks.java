package network.aika.steps.activation;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.steps.Phase;
import network.aika.steps.Step;

public class InactiveLinks extends Step<Activation> {

    public static void add(Activation act) {
        Step.add(new InactiveLinks(act));
    }

    public InactiveLinks(Activation act) {
        super(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.POST_PROCESSING;
    }

    @Override
    public void process() {
        Activation act = getElement();
        Neuron n = act.getNeuron();

        n.addInactiveLinks(act);
    }
}
