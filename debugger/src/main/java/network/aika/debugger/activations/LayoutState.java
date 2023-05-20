package network.aika.debugger.activations;

import network.aika.debugger.activations.particles.ActivationParticle;
import network.aika.elements.activations.Activation;

import static network.aika.debugger.AbstractGraphManager.STANDARD_DISTANCE_X;

public class LayoutState {

    ActivationGraphManager graphManager;

    private Double lastInputActXPos = null;
    private Activation lastInputAct = null;

    public LayoutState() {
    }

    public Activation getLastInputAct() {
        return lastInputAct;
    }

    public void setLastInputAct(Activation lastInputAct) {
        this.lastInputAct = lastInputAct;
    }

    public synchronized Double getInitialXPos(Activation act) {
        double x = lastInputActXPos != null ?
                lastInputActXPos + STANDARD_DISTANCE_X :
                0.0;

        lastInputActXPos = x;
        setLastInputAct(act);
        return x;
    }

    private Double getActivationXCoordinate(Activation act) {
        if(act == null)
            return null;

        ActivationParticle originParticle = graphManager.getParticle(act.getId());
        if(originParticle == null)
            return null;

        return originParticle.getPosition().x;
    }
}
