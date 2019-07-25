package network.aika.neuron.activation;


public enum Direction {
    INPUT,
    OUTPUT;

    public Direction getInverted() {
        switch (this) {
            case INPUT:
                return OUTPUT;
            case OUTPUT:
                return INPUT;
        }
        return null;
    }
}