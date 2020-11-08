package network.aika.neuron.phase;

import network.aika.Config;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class UpdateWeights implements Phase {

    @Override
    public void process(Activation act) {
        act.updateSynapseWeights();
        act.getNeuronProvider().save();
    }

    @Override
    public Phase nextPhase(Config c) {
        return FINAL;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public void tryToLink(Activation iAct, Activation oAct, Visitor c) {

    }

    @Override
    public void propagate(Activation act) {

    }

    @Override
    public int getRank() {
        return 7;
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return 0;
    }
}
