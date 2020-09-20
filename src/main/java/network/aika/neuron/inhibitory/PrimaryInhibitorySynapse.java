package network.aika.neuron.inhibitory;

import network.aika.neuron.activation.Context;

public class PrimaryInhibitorySynapse extends InhibitorySynapse {

    public Context transition(Context c) {
        return new Context(c, true);
    }
}
