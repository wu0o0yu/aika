package network.aika;


import network.aika.neuron.activation.Activation;

public enum DistanceFunction {

    NONE((iAct, oAct) -> 1.0),
    DEGRADING((iAct, oAct) -> 1.0 / ((double) (1 + Math.abs(iAct.getSlot(Activation.BEGIN).getDistance(oAct.getSlot(Activation.BEGIN))))));

    Function f;

    DistanceFunction(Function f) {
        this.f = f;
    }

    public double f(Activation iAct, Activation oAct) {
        return f.f(iAct, oAct);
    }


    interface Function {
        double f(Activation iAct, Activation oAct);
    }

}
