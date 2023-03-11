package network.aika.debugger.activations;

import network.aika.elements.activations.Activation;

public class LayoutState {

    private Double lastInputActXPos = null;
    private Activation lastInputAct = null;


    public Double getLastInputActXPos() {
        return lastInputActXPos;
    }

    public void setLastInputActXPos(Double lastInputActXPos) {
        this.lastInputActXPos = lastInputActXPos;
    }

    public Activation getLastInputAct() {
        return lastInputAct;
    }

    public void setLastInputAct(Activation lastInputAct) {
        this.lastInputAct = lastInputAct;
    }

}
