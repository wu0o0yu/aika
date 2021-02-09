package network.aika.neuron.sign;

public class Positive implements Sign {
    @Override
    public Sign invert() {
        return NEG;
    }

    @Override
    public Double getValue(double x) {
        return x;
    }
}
