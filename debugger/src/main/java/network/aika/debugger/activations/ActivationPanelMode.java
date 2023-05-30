package network.aika.debugger.activations;

public enum ActivationPanelMode {
    SELECTED("Selected"),
    CURRENT("Current");

    String txt;

    ActivationPanelMode inverted;

    static {
        SELECTED.inverted = CURRENT;
        CURRENT.inverted = SELECTED;
    }

    public ActivationPanelMode getInverted() {
        return inverted;
    }

    ActivationPanelMode(String txt) {
        this.txt = txt;
    }

    public String getTxt() {
        return txt;
    }
}
