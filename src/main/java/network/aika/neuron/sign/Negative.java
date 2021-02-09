package network.aika.neuron.sign;

public class Negative implements Sign {
    @Override
    public Sign invert() {
        return POS;
    }

    @Override
    public Double getValue(double x) {
        return 1.0 - x;
    }
}
