package network.aika.steps;

public enum Phase {
    INPUT_LINKING("IL"),
    OUTPUT_LINKING("OL"),
    PROCESSING("P"),
    INSTANTIATION_A("IA"),
    INSTANTIATION_B("IB"),
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
