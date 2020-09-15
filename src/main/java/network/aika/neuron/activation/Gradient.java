package network.aika.neuron.activation;

public class Gradient {

    private Gradient lastRound;
    private double gradient = 0.0;
    private boolean isFixed;


    public Gradient(Gradient lastRound) {
        this.lastRound = lastRound;
    }

    public double getGradient() {
        return gradient;
    }

    public void addGradient(double gradient) {
        this.gradient += gradient;
    }

    public double getFinalGradient() {
        return gradient + (lastRound != null ? lastRound.getFinalGradient() : 0.0);
    }

    public boolean isFixed() {
        return isFixed;
    }

    public void setFixed() {
        isFixed = true;
    }
}
