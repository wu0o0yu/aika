package network.aika.lattice.activation;

import network.aika.lattice.InputNode;
import network.aika.lattice.NodeActivation;
import network.aika.neuron.activation.Activation;


public class InputActivation extends NodeActivation<InputNode> {

    public Activation input;

    public InputActivation(Activation iAct, InputNode node) {
        super(iAct.getDocument(), node);
        input = iAct;
        iAct.setOutputNodeActivation(this);
    }

    public Activation getInputActivation(int i) {
        assert i == 0;
        return input;
    }


    public String toString() {
        return "I-ACT(" + input.getLabel() + " " + input.slotsToString() + ")";
    }
}