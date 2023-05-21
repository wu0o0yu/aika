package network.aika.debugger.activations;

public enum ActivationPanelMode {
    SELECTED("Selected"),
    CURRENT("Current");

    String txt;

    ActivationPanelMode(String txt) {
        this.txt = txt;
    }

    public String getTxt() {
        return txt;
    }
}
