package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.phase.link.LinkPhase;


public class FinalLinking extends Linking {

    public boolean isFinal() {
        return true;
    }

    @Override
    public void propagate(Activation act, Visitor v) {
        act.propagateIntern(v);
    }

    @Override
    public int getRank() {
        return 4;
    }
}
