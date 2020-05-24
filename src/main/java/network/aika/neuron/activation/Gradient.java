package network.aika.neuron.activation;

public class Gradient {

    public Gradient(Gradient lastRound) {
        this.lastRound = lastRound;
    }

    Gradient lastRound;

    public double gradient;

    public boolean isFixed;

}
