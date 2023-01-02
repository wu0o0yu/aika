package network.aika.steps;

public enum Phase {
    INPUT_LINKING("IL"),
    OUTPUT_LINKING("OL"),
    INFERENCE("I"),
    INSTANTIATION_NODES("IA"),
    INSTANTIATION_EDGES("IB"),
    TRAINING("T"),
    COUNTING("C"),
    SAVE("S");

    String label;

    Phase(String l) {
        label = l;
    }

    public String getLabel() {
        return label;
    }
}
