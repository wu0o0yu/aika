package network.aika.steps.activation;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import static network.aika.steps.Phase.OUTPUT_LINKING;

public class LinkingOut extends Step<Activation> {

    private Synapse synapse;

    public static void add(Activation act, Synapse s) {
        Step.add(new LinkingOut(act, s));
    }

    public LinkingOut(Activation act, Synapse s) {
        super(act);
        this.synapse = s;
    }

    @Override
    public void process() {
        synapse.linkAndPropagateOut(getElement());
    }

    @Override
    public Phase getPhase() {
        return OUTPUT_LINKING;
    }
}
