package network.aika.neuron.activation;

public class Gradient {
    private double alreadyPropagatedGradient = 0.0;
    private double gradient = 0.0;

    public void set(double g) {
        gradient = g - alreadyPropagatedGradient;
    }

    public void add(double g) {
        gradient += g;
    }

    public void propagate(Gradient to) {
        to.add(gradient);
        alreadyPropagatedGradient += gradient;
        gradient = 0.0;
    }
}
