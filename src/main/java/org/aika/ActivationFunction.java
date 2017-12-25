package org.aika;


public interface ActivationFunction {

    double f(double x);


    String RECTIFIED_SCALED_LOGISTIC_SIGMOID_KEY = "ReSLS";
    ActivationFunction RECTIFIED_SCALED_LOGISTIC_SIGMOID =
            x -> Math.max(0.0, (2.0 * (1.0 / (1.0 + Math.pow(Math.E, (-x))))) - 1.0);

    String RECTIFIED_HYPERBOLIC_TANGENT_KEY = "ReTANH";
    ActivationFunction RECTIFIED_HYPERBOLIC_TANGENT =
            x -> Math.max(0.0, Math.tanh(x));

    String RECTIFIED_LINEAR_UNIT_KEY = "ReLU";
    ActivationFunction RECTIFIED_LINEAR_UNIT =
            x -> Math.max(0.0, x);

}
