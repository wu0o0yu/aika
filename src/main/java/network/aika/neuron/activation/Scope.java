package network.aika.neuron.activation;

public enum Scope {
    SAME,
    INPUT,
    RELATED;

    private Scope in;
    private Scope out;

    public void init(Scope in, Scope out) {
        this.in = in;
        this.out = out;
    }

    static {
        SAME.init(INPUT, RELATED);
        INPUT.init(RELATED, SAME);
        RELATED.init(SAME, INPUT);
    }

    public Scope getNext(Direction dir) {
        return dir == Direction.INPUT ? in : out;
    }
}
