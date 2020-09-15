package network.aika.neuron;

import network.aika.neuron.activation.Activation;


public enum Sign {
    POS(),
    NEG();

    public static Sign getSign(Activation act) {
        return act.isActive() ? POS : NEG;
    }
}
