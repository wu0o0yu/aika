package network.aika.neuron.activation;

public enum Direction {
    INPUT,
    OUTPUT,
    SAME;

    static {
        INPUT.init(OUTPUT, SAME, null);
        OUTPUT.init(INPUT, null, SAME);
        SAME.init(SAME, OUTPUT, INPUT);
    }

    private Direction inverted;
    private Direction nextUp;
    private Direction nextDown;

    public void init(Direction inverted, Direction nextUp, Direction nextDown) {
        this.inverted = inverted;
        this.nextUp = nextUp;
        this.nextDown = nextDown;
    }

    public Direction invert() {
        return inverted;
    }

    public Direction getNext(Direction dir) {
        return dir == INPUT ? nextDown : nextUp;
    }
}
