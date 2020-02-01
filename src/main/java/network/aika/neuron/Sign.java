package network.aika.neuron;

import network.aika.neuron.activation.Activation;

import java.util.function.Function;

public enum Sign {
    POS(act -> act != null ? act.value : 0.0, n -> n.getP()),
    NEG(act -> act != null ? 1.0 - act.value : 1.0, n -> 1.0 - n.getP());

    private Function<Activation, Double> actF;
    private Function<TNeuron, Double> neuronF;

    Sign(Function<Activation, Double> actF, Function<TNeuron, Double> neuronF) {
        this.actF = actF;
        this.neuronF = neuronF;
    }

    public double getX(Activation act) {
        return actF.apply(act);
    }

    public double getP(TNeuron n) {
        return neuronF.apply(n);
    }
}
