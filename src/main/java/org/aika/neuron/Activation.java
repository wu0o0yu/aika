package org.aika.neuron;

import org.aika.Utils;
import org.aika.lattice.NodeActivation;
import org.aika.lattice.OrNode;

import java.util.*;

import static org.aika.neuron.Activation.SynapseActivation.INPUT_COMP;
import static org.aika.neuron.Activation.SynapseActivation.OUTPUT_COMP;


/**
 * The {@code Activation} class is the most central class in Aika. On the one hand it stores the activation value
 * for a given neuron in the {@code State} substructure. On the other hand it specifies where this activation is
 * located within the document and to which interpretation it belongs. The {@code Activation.Key} therefore
 * consists of the logic node to which this activation belongs. If this logic node is an or-node, then this activation
 * automatically also belongs to the neuron as well. Furthermore, the key contains the char range within the document
 * and the relational id (rid). The relational id might be used to store the word pos for instance. Lastly, the key
 * contain the interpretation node of this activation, specifying to which interpretation this activation belongs.
 *
 * <p>The activations are linked to each other on two levels. The fields {@code inputs} and {@code outputs}
 * contain the activation links within the logic layer. The fields {@code neuronInputs} and
 * {@code neuronOutputs} contain the links on the neural layer.
 *
 * @author Lukas Molzberger
 */
public final class Activation extends NodeActivation<OrNode> {

    static final SynapseActivation[] EMPTY_SYN_ACTS = new SynapseActivation[0];
    public SynapseActivation[] neuronInputs = EMPTY_SYN_ACTS;
    public SynapseActivation[] neuronOutputs = EMPTY_SYN_ACTS;

    public double upperBound;
    public double lowerBound;

    public Rounds rounds = new Rounds();

    public State finalState;

    public boolean ubQueued = false;
    public boolean isQueued = false;
    public long queueId;

    public boolean isInput;


    public Activation(int id, Key key) {
        super(id, key);
    }


    public void addSynapseActivation(int dir, SynapseActivation sa) {
        if(dir == 0) {
            if(Utils.contains(neuronOutputs, sa, OUTPUT_COMP)) return;
            neuronOutputs = Utils.addToArray(neuronOutputs, sa);
        } else {
            if(Utils.contains(neuronInputs, sa, INPUT_COMP)) return;
            neuronInputs = Utils.addToArray(neuronInputs, sa);
        }
    }


    public void removeSynapseActivation(int dir, SynapseActivation sa) {
        if(dir == 0) {
            neuronOutputs = Utils.removeToArray(neuronOutputs, sa);
        } else {
            neuronInputs = Utils.removeToArray(neuronInputs, sa);
        };
    }

    /**
     * The {@code SynapseActivation} mirror the synapse link in the network of activations.
     */
    public static class SynapseActivation {
        public final Synapse s;
        public final Activation input;
        public final Activation output;

        public static Comparator<SynapseActivation> INPUT_COMP = new Comparator<SynapseActivation>() {
            @Override
            public int compare(SynapseActivation sa1, SynapseActivation sa2) {
                int r = Synapse.INPUT_SYNAPSE_COMP.compare(sa1.s, sa2.s);
                if(r != 0) return r;
                return sa1.input.compareTo(sa2.input);
            }
        };

        public static Comparator<SynapseActivation> OUTPUT_COMP = new Comparator<SynapseActivation>() {
            @Override
            public int compare(SynapseActivation sa1, SynapseActivation sa2) {
                int r = Synapse.OUTPUT_SYNAPSE_COMP.compare(sa1.s, sa2.s);
                if(r != 0) return r;
                return sa1.output.compareTo(sa2.output);
            }
        };


        public SynapseActivation(Synapse s, Activation input, Activation output) {
            this.s = s;
            this.input = input;
            this.output = output;
        }
    }


    /**
     * Since Aika is a recurrent neural network, it is necessary to compute several rounds of activation values. The
     * computation stops if no further changes occur to the state. Only the recurrent synapses depend on the previous
     * round.
     *
     */
    public static class Rounds {
        private boolean[] isQueued = new boolean[3];

        public TreeMap<Integer, State> rounds = new TreeMap<>();

        public boolean set(int r, State s) {
            State lr = get(r - 1);
            if(lr != null && lr.equalsWithWeights(s)) {
                State or = rounds.get(r);
                if(or != null) {
                    rounds.remove(r);
                    return !or.equalsWithWeights(s);
                }
                return false;
            } else {
                State or = rounds.put(r, s);

                for(Iterator<Map.Entry<Integer, State>> it = rounds.tailMap(r + 1).entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Integer, State> me = it.next();
                    if(me.getValue().equalsWithWeights(s)) it.remove();
                }
                return or == null || !or.equalsWithWeights(s);
            }
        }

        public State get(int r) {
            Map.Entry<Integer, State> me = rounds.floorEntry(r);
            return me != null ? me.getValue() : State.ZERO;
        }

        public Rounds copy() {
            Rounds nr = new Rounds();
            nr.rounds.putAll(rounds);
            return nr;
        }

        public Integer getLastRound() {
            return !rounds.isEmpty() ? rounds.lastKey() : null;
        }

        public State getLast() {
            return !rounds.isEmpty() ? rounds.lastEntry().getValue() : null;
        }

        public void setQueued(int r, boolean v) {
            if(r >= isQueued.length) {
                isQueued = Arrays.copyOf(isQueued, isQueued.length * 2);
            }
            isQueued[r] = v;
        }

        public boolean isQueued(int r) {
            return r < isQueued.length ? isQueued[r] : false;
        }
    }


    /**
     * A <code>State</code> object contains the activation value of an activation object that belongs to a neuron.
     * It furthermore contains a weight that is used to evaluate the interpretations during the search for the best
     * interpretation.
     */
    public static class State {
        public final double value;
        public final double ub;
        public final double lb;

        public final int fired;
        public final Neuron.NormWeight weight;
        public final Neuron.NormWeight weightUB;

        public static final State ZERO = new State(0.0, 0.0, 0.0, -1, Neuron.NormWeight.ZERO_WEIGHT, Neuron.NormWeight.ZERO_WEIGHT);

        public State(double value, double ub, double lb, int fired, Neuron.NormWeight weight, Neuron.NormWeight weightUB) {
            assert lb <= value && value <= ub;
            assert weight.w <= weightUB.w && weightUB.n <= weight.n;
            this.value = value;
            this.ub = ub;
            this.lb = lb;
            this.fired = fired;
            this.weight = weight;
            this.weightUB = weightUB;
        }

        public Neuron.NormWeight getWeight(int t) {
            return t == 0 ? weight : weightUB;
        }

        public boolean equals(State s) {
            return Math.abs(value - s.value) <= Neuron.WEIGHT_TOLERANCE || Math.abs(ub - s.ub) <= Neuron.WEIGHT_TOLERANCE || Math.abs(lb - s.lb) <= Neuron.WEIGHT_TOLERANCE;
        }

        public boolean equalsWithWeights(State s) {
            return equals(s) && weight.equals(s.weight);
        }

        public String toString() {
            return "V:" + value;
        }
    }

}

