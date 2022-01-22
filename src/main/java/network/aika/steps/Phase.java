package network.aika.steps;

public enum Phase {
    INIT,
    PROCESSING,
    LATE,
    POST_PROCESSING  // No changes to the activations are allowed.
}
