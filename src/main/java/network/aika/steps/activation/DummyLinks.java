package network.aika.steps.activation;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.steps.Phase;
import network.aika.steps.Step;

public class DummyLinks extends Step<Activation> {

    public static void add(Activation act) {
        Step.add(new DummyLinks(act));
    }

    public DummyLinks(Activation act) {
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

        n.addDummyLinks(act);
    }

    public String toString() {
        return "Act-Step: Add dummy links " + getElement().toShortString();
    }
}
