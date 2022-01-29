package network.aika.neuron.bindingsignal;

public enum Scope {
    SAME,
    INPUT,
    RELATED;

    public Scope next() {
        switch (this) {
            case SAME:
                return INPUT;
            case INPUT:
                return RELATED;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
