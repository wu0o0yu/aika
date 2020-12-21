package network.aika.neuron;

import network.aika.neuron.activation.Activation;


public enum Sign {
    POS,
    NEG;

    static {
        POS.init(NEG);
        NEG.init(POS);
    }

    private Sign inverted;

    private void init(Sign inverted) {
        this.inverted = inverted;
    }

    public Sign invert() {
        return inverted;
    }

    public static Sign getSign(Activation act) {
        return act.isActive() ? POS : NEG;
    }
}
