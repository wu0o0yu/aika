package network.aika.steps.link;

import network.aika.neuron.activation.ConjunctiveLink;
import network.aika.neuron.conjunctive.ConjunctiveNeuron;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import static network.aika.steps.Phase.LINKING;

public class Linking extends Step<ConjunctiveLink> {


    public static void add(ConjunctiveLink l) {
        Step.add(new Linking(l));
    }

    public Linking(ConjunctiveLink l) {
        super(l);
    }

    @Override
    public void process() {
        ConjunctiveLink l = getElement();
        ConjunctiveNeuron n = (ConjunctiveNeuron) l.getOutput().getNeuron();
        n.linkAndPropagateIn(l);
    }

    @Override
    public Phase getPhase() {
        return LINKING;
    }
}
