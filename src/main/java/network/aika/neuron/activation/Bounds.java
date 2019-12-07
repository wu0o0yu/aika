package network.aika.neuron.activation;

public class Bounds {

    public Bounds(State lb, State ub) {
        this.lb = lb;
        this.ub = ub;
    }

    public final State lb;
    public final State ub;


    public boolean equals(Bounds b) {
        return lb.equals(b.lb) && ub.equals(b.ub);
    }

    public boolean equalsWithWeights(Bounds b) {
        return lb.equalsWithWeights(b.lb) && ub.equalsWithWeights(b.ub);
    }
}
