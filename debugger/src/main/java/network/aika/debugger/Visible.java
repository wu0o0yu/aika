package network.aika.debugger;

public enum Visible {
    SHOW("Show"),
    HIDE("Hide");

    String txt;

    Visible inverted;

    static {
        SHOW.inverted = HIDE;
        HIDE.inverted = SHOW;
    }

    public Visible getInverted() {
        return inverted;
    }


    Visible(String txt) {
        this.txt = txt;
    }

    public String getTxt() {
        return txt;
    }
}
