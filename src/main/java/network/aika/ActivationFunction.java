package network.aika;


public enum ActivationFunction {

    RECTIFIED_SCALED_LOGISTIC_SIGMOID(x -> Math.max(0.0, (2.0 * Utils.sigmoid(x)) - 1.0)),
    RECTIFIED_HYPERBOLIC_TANGENT(x -> Math.max(0.0, Math.tanh(x))),
    RECTIFIED_LINEAR_UNIT(x -> Math.max(0.0, x));

    Function f;

    ActivationFunction(Function f) {
        this.f = f;
    }

    public double f(double x) {
        return f.f(x);
    }


    interface Function {
        double f(double x);
    }

}
