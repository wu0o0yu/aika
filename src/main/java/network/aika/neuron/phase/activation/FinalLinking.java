package network.aika.neuron.phase.activation;


import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;


public class FinalLinking extends Linking {

    public boolean isFinal() {
        return true;
    }

    @Override
    public void propagate(Activation act, Visitor v) {
        act.updateOutgoingLinks();
    }

    @Override
    public int getRank() {
        return 4;
    }
}
