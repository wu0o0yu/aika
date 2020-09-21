package network.aika.neuron.inhibitory;

import network.aika.neuron.activation.Visitor;

public class PrimaryInhibitorySynapse extends InhibitorySynapse {

    public Visitor transition(Visitor v) {
        return new Visitor(v, true);
    }
}
