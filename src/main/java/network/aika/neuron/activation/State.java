package network.aika.neuron.activation;

import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.search.Decision;

import static network.aika.neuron.activation.search.Decision.EXCLUDED;
import static network.aika.neuron.activation.search.Decision.SELECTED;


/**
 * A <code>State</code> object contains the activation value of an activation object that belongs to a neuron.
 * It furthermore contains a weight that is used to check the interpretations during the search for the best
 * interpretation.
 */
public class State {
    public final double value;
    public final double ub;

    public final double net;

    public final Integer fired;
    public final double weight;

    public static final State ZERO = new State(0.0, 0.0, 0.0, null, 0.0);


    public State(double value, double ub, double net, Integer fired, double weight) {
        assert !Double.isNaN(value);
        this.value = value;
        this.ub = ub;
        this.net = net;
        this.fired = fired;
        this.weight = weight;
    }


    public boolean equals(State s) {
        return Math.abs(value - s.value) <= INeuron.WEIGHT_TOLERANCE && Math.abs(ub - s.ub) <= INeuron.WEIGHT_TOLERANCE;
    }


    public boolean equalsWithWeights(State s) {
        return equals(s) && Math.abs(weight - s.weight) <= INeuron.WEIGHT_TOLERANCE;
    }


    public Decision getPreferredDecision() {
        return value > 0.0 ? SELECTED : EXCLUDED;
    }


    public String toString() {
        return "V:" + Utils.round(value) + " UB:" + Utils.round(ub) + " Net:" + Utils.round(net) + " W:" + Utils.round(weight);
    }
}
