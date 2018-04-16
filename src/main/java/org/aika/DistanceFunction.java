package org.aika;


import org.aika.neuron.activation.Activation;

public enum DistanceFunction {

    NONE((iAct, oAct) -> 1),
    SIGMOID((iAct, oAct) -> Utils.sigmoid(((double) (oAct.range.begin - iAct.range.begin) + 40) / 10.0));

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
