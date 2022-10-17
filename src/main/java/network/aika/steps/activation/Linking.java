package network.aika.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import static network.aika.steps.Phase.LINKING;

public class Linking extends Step<Activation> {

    public static void add(Activation act) {
        Step.add(new Linking(act));
    }

    public Linking(Activation act) {
        super(act);
    }

    @Override
    public void process() {
        Activation act = getElement();
        act.getNeuron().linkAndPropagateOut(act, true);
    }

    @Override
    public Phase getPhase() {
        return LINKING;
    }
}
