package network.aika;


public enum ActivationFunction {

    RECTIFIED_HYPERBOLIC_TANGENT(x -> Math.max(0.0, Math.tanh(x))),
    RECTIFIED_LINEAR_UNIT(x -> Math.max(0.0, x)),
    LIMITED_RECTIFIED_LINEAR_UNIT(x -> Math.max(0.0, Math.min(1.0, x)));

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

    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.pow(Math.E, (-x)));
    }
}
