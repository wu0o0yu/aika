package network.aika.neuron.activation;

public enum Direction {
    INPUT,
    OUTPUT;

    static {
        INPUT.setInverted(OUTPUT);
        OUTPUT.setInverted(INPUT);
    }

    private Direction inverted;

    public void setInverted(Direction inverted) {
        this.inverted = inverted;
    }

    public Direction invert() {
        return inverted;
    }
}
