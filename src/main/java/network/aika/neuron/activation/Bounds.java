package network.aika.neuron.activation;

public class Bounds {

    public Bounds(State lb, State ub) {
        this.lb = lb;
        this.ub = ub;
    }

    State lb;
    State ub;


    public boolean equals(State s) {
        return lb.equals(s) && ub.equals(s);
    }

}
