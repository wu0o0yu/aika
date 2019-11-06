package network.aika.neuron;

public enum Sign {
    POS,
    NEG;

    public double getX(double x) {
        switch(this) {
            case POS:
                return x;
            case NEG:
                return 1.0 - x;
            default:
                return 0.0;
        }
    }
}
