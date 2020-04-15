package network.aika.neuron;

import network.aika.neuron.activation.Activation;

import java.util.function.Function;

public enum Sign {
    POS(
            act -> act != null ? act.value : 0.0,
            n -> n.getP(),
            1.0
    ),
    NEG(
            act -> act != null ? 1.0 - act.value : 1.0,
            n -> 1.0 - n.getP(),
            -1.0
    );

    private Function<Activation, Double> actF;
    private Function<TNeuron, Double> neuronF;
    private double sign;

    Sign(Function<Activation, Double> actF, Function<TNeuron, Double> neuronF, double sign) {
        this.actF = actF;
        this.neuronF = neuronF;
        this.sign = sign;
    }

    public double getX(Activation act) {
        return actF.apply(act);
    }

    public double getP(TNeuron n) {
        return neuronF.apply(n);
    }

    public double getSign() {
        return sign;
    }
}
