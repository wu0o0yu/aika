package network.aika.steps;

public enum Phase {
    INPUT_LINKING,
    OUTPUT_LINKING,
    PROCESSING,
    POST_PROCESSING  // No changes to the activations are allowed.
}
