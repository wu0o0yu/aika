package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class UpdateSynapseInputLinks implements ActivationPhase {

    @Override
    public void process(Activation act) {
        act.getNeuron().updateSynapseInputLinks();
        act.getNeuronProvider().save();
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public void tryToLink(Activation act, Visitor v) {

    }

    @Override
    public void propagate(Activation act, Visitor v) {

    }

    @Override
    public int getRank() {
        return 19;
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return 0;
    }
}
