package network.aika.neuron;

import java.util.function.Function;

public enum Sign {
    POS(
            n -> n.getP(),
            1.0
    ),
    NEG(
            n -> 1.0 - n.getP(),
            -1.0
    );

    private Function<Neuron, Double> neuronF;
    private double sign;

    Sign(Function<Neuron, Double> neuronF, double sign) {
        this.neuronF = neuronF;
        this.sign = sign;
    }

    public double getP(Neuron n) {
        return neuronF.apply(n);
    }

    public double getSign() {
        return sign;
    }
}
