package network.aika.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import static network.aika.steps.Phase.OUTPUT_LINKING;

public class LinkingOut extends Step<Activation> {

    public static void add(Activation act) {
        Step.add(new LinkingOut(act));
    }

    public LinkingOut(Activation act) {
        super(act);
    }

    @Override
    public void process() {
        Activation act = getElement();
        act.getNeuron().linkAndPropagateOut(act);
    }

    @Override
    public Phase getPhase() {
        return OUTPUT_LINKING;
    }
}
