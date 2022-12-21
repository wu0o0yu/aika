package network.aika.steps;

public enum Phase {
    INPUT_LINKING("IL"),
    OUTPUT_LINKING("OL"),
    PROCESSING("P"),
    POST_PROCESSING("PP");

    String label;

    Phase(String l) {
        label = l;
    }

    public String getLabel() {
        return label;
    }
}
