package org.aika.neuron;

import org.aika.Utils;
import org.aika.corpus.*;
import org.aika.lattice.Node;
import org.aika.lattice.NodeActivation;
import org.aika.lattice.OrNode;
import org.aika.neuron.Linker.Direction;
import org.aika.corpus.SearchNode.Weight;
import org.aika.corpus.SearchNode.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.aika.corpus.SearchNode.Decision.SELECTED;
import static org.aika.neuron.Activation.State.DIR;
import static org.aika.neuron.Activation.State.REC;
import static org.aika.neuron.Activation.SynapseActivation.INPUT_COMP;
import static org.aika.neuron.Activation.SynapseActivation.OUTPUT_COMP;
import static org.aika.neuron.INeuron.ALLOW_WEAK_NEGATIVE_WEIGHTS;


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
    public static final Comparator<Activation> ACTIVATION_ID_COMP = Comparator.comparingInt(act -> act.id);
    public static int MAX_SELF_REFERENCING_DEPTH = 5;

    private static final Logger log = LoggerFactory.getLogger(Activation.class);

    public TreeSet<SynapseActivation> selectedNeuronInputs = new TreeSet<>(INPUT_COMP);
    public TreeSet<SynapseActivation> neuronInputs = new TreeSet<>(INPUT_COMP);
    public TreeSet<SynapseActivation> neuronOutputs = new TreeSet<>(OUTPUT_COMP);

    public Integer sequence;

    public double upperBound;
    public double lowerBound;

    public Rounds rounds = new Rounds();
    public Rounds finalRounds = rounds;
    public double maxActValue = 0.0;

    public boolean ubQueued = false;
    public boolean isQueued = false;
    public long queueId;
    public long markedHasCandidate;

    public long currentStateV;
    public StateChange currentStateChange;
    public long markedDirty;

    public double errorSignal;
    public Double targetValue;
    public Double inputValue;



    public Decision inputDecision = Decision.UNKNOWN;
    public Decision decision = Decision.UNKNOWN;
    public Decision finalDecision = Decision.UNKNOWN;
    public Candidate candidate;
    public Conflicts conflicts = new Conflicts();
    public long markedConflict;
    private long visitedState;


    public Activation(int id, Document doc, Key key) {
        super(id, doc, key);
    }


    public void setTargetValue(Double targetValue) {
        this.targetValue = targetValue;
        if (targetValue != null) {
            doc.supervisedTraining.targetActivations.add(this);
        } else {
            doc.supervisedTraining.targetActivations.remove(this);
        }
    }


    public String getLabel() {
        return getINeuron().label;
    }


    public String getText() {
        return doc.getText(key.range);
    }


    public INeuron getINeuron() {
        return getNeuron().get(doc);
    }


    public Neuron getNeuron() {
        return key.node.neuron;
    }


    public void addSynapseActivation(Direction dir, SynapseActivation sa) {
        switch(dir) {
            case INPUT:
                neuronOutputs.add(sa);
                break;
            case OUTPUT:
                if(sa.input.decision == SELECTED) {
                    selectedNeuronInputs.add(sa);
                }
                neuronInputs.add(sa);
                break;
        }
    }


    public Weight process(SearchNode sn, int round, long v) {
        Weight delta = Weight.ZERO;
        State s;
        if(inputValue != null) {
            s = new State(inputValue, 0, Weight.ZERO);
        } else {
            s = computeValueAndWeight(round);
        }

        if (round == 0 || !rounds.get(round).equalsWithWeights(s)) {
            saveOldState(sn.modifiedActs, v);

            State oldState = rounds.get(round);

            boolean propagate = rounds.set(round, s) && (oldState == null || !oldState.equals(s));

            saveNewState();

            if (propagate) {
                if(round > Document.MAX_ROUND) {
                    log.error("Error: Maximum number of rounds reached. The network might be oscillating.");
                    log.info(doc.activationsToString(true, true));

                    doc.dumpOscillatingActivations();
                    throw new RuntimeException("Maximum number of rounds reached. The network might be oscillating.");
                } else {
                    doc.vQueue.propagateActivationValue(round, this);
                }
            }

            if (round == 0) {
                // In case that there is a positive feedback loop.
                doc.vQueue.add(1, this);
            }

            if (rounds.getLastRound() != null && round >= rounds.getLastRound()) { // Consider only the final round.
                delta = delta.add(s.weight.sub(oldState.weight));
            }
        }
        return delta;
    }


    public State computeValueAndWeight(int round) {
        INeuron n = getINeuron();
        double[] sum = {n.biasSum, 0.0};

        int fired = -1;

        for (InputState is: getInputStates(round)) {
            Synapse s = is.sa.synapse;
            Activation iAct = is.sa.input;

            if (iAct == this) continue;

            int t = s.key.isRecurrent ? REC : DIR;
            sum[t] += is.s.value * s.weight;

            if (!s.key.isRecurrent && !s.isNegative() && sum[DIR] + sum[REC] >= 0.0 && fired < 0) {
                fired = iAct.rounds.get(round).fired + 1;
            }
        }

        double drSum = sum[DIR] + sum[REC];
        double currentActValue = n.activationFunction.f(drSum);

        maxActValue = Math.max(maxActValue, currentActValue);

        // Compute only the recurrent part is above the threshold.
        Weight newWeight = Weight.create(
                decision == SELECTED ? (sum[DIR] + n.negRecSum) < 0.0 ? Math.max(0.0, drSum) : sum[REC] - n.negRecSum : 0.0,
                (sum[DIR] + n.negRecSum) < 0.0 ? Math.max(0.0, sum[DIR] + n.negRecSum + n.maxRecurrentSum) : n.maxRecurrentSum
        );

        return new State(
                decision == SELECTED || ALLOW_WEAK_NEGATIVE_WEIGHTS ? currentActValue : 0.0,
                decision == SELECTED || ALLOW_WEAK_NEGATIVE_WEIGHTS ? fired : -1,
                newWeight
        );
    }


    public void processBounds() {
        double oldUpperBound = upperBound;

        computeBounds();

        if(Math.abs(upperBound - oldUpperBound) > 0.01) {
            for(Activation.SynapseActivation sa: neuronOutputs) {
                doc.ubQueue.add(sa.output);
            }
        }

        if (oldUpperBound <= 0.0 && upperBound > 0.0) {
            getINeuron().propagate(this);
        }
    }


    public void computeBounds() {
        INeuron n = getINeuron();
        double ub = n.biasSum + n.posRecSum;
        double lb = n.biasSum + n.posRecSum;

        for (SynapseActivation sa : neuronInputs) {
            Synapse s = sa.synapse;
            if(s.inactive) {
                continue;
            }
            Activation iAct = sa.input;

            if (iAct == this) continue;

            if (s.isNegative()) {
                if (!s.key.isRecurrent && !checkSelfReferencing(this, iAct, false, 0)) {
                    ub += iAct.lowerBound * s.weight;
                }

                lb += s.weight;
            } else {
                ub += iAct.upperBound * s.weight;
                lb += iAct.lowerBound * s.weight;
            }
        }

        upperBound = n.activationFunction.f(ub);
        lowerBound = n.activationFunction.f(lb);
    }


    private static State getInitialState(Decision c) {
        return new State(
                c == SELECTED ? 1.0 : 0.0,
                0,
                Weight.ZERO
        );
    }



    private List<InputState> getInputStates(int round) {
        ArrayList<InputState> tmp = new ArrayList<>();
        Synapse lastSynapse = null;
        InputState maxInputState = null;
        for (SynapseActivation sa : neuronInputs) {
            if(sa.synapse.inactive) {
                continue;
            }
            if (lastSynapse != null && lastSynapse != sa.synapse) {
                tmp.add(maxInputState);
                maxInputState = null;
            }

            State s = sa.input.getInputState(round, this, sa.synapse);
            if (maxInputState == null || maxInputState.s.value < s.value) {
                maxInputState = new InputState(sa, s);
            }
            lastSynapse = sa.synapse;
        }
        if (maxInputState != null) {
            tmp.add(maxInputState);
        }

        return tmp;
    }


    private static class InputState {
        public InputState(SynapseActivation sa, State s) {
            this.sa = sa;
            this.s = s;
        }

        SynapseActivation sa;
        State s;
    }


    private State getInputState(int round, Activation act, Synapse s) {
        State is = State.ZERO;
        if (s.key.isRecurrent) {
            if (!s.isNegative() || !checkSelfReferencing(act, this, true, 0)) {
                is = round == 0 ? getInitialState(decision) : rounds.get(round - 1);
            }
        } else {
            is = rounds.get(round);
        }
        return is;
    }


    public List<SynapseActivation> getFinalInputActivations() {
        ArrayList<SynapseActivation> results = new ArrayList<>();
        for (SynapseActivation inputAct : neuronInputs) {
            if (inputAct.input.isFinalActivation()) {
                results.add(inputAct);
            }
        }
        return results;
    }


    public List<SynapseActivation> getFinalOutputActivations() {
        ArrayList<SynapseActivation> results = new ArrayList<>();
        for (SynapseActivation inputAct : neuronOutputs) {
            if (inputAct.output.isFinalActivation()) {
                results.add(inputAct);
            }
        }
        return results;
    }


    public void adjustSelectedNeuronInputs(Decision d) {
        for(SynapseActivation sa: neuronOutputs) {
            if(d == SELECTED) {
                sa.output.selectedNeuronInputs.add(sa);
            } else {
                sa.output.selectedNeuronInputs.remove(sa);
            }
        }
    }


    public static boolean checkSelfReferencing(Activation nx, Activation ny, boolean onlySelected, int depth) {
        if (nx == ny) return true;

        if (depth > MAX_SELF_REFERENCING_DEPTH) return false;

        Set<SynapseActivation> inputs = onlySelected ? ny.selectedNeuronInputs : ny.neuronInputs;
        for (SynapseActivation sa: inputs) {
            if (checkSelfReferencing(nx, sa.input, onlySelected, depth + 1)) return true;
        }

        return false;
    }


    public void setDecision(Decision newDecision, long v) {
        if(inputDecision != Decision.UNKNOWN && newDecision != inputDecision) return;

        if (newDecision == Decision.UNKNOWN && v != visitedState) return;

        if(decision == Decision.SELECTED != (newDecision == Decision.SELECTED)) {
            adjustSelectedNeuronInputs(newDecision);
        }

        decision = newDecision;
        visitedState = v;
    }


    public boolean isFinalActivation() {
        return getFinalState().value > 0.0;
    }


    public State getFinalState() {
        return finalRounds.getLast();
    }


    public <T extends Node> boolean filter(T n, Integer rid, Range r, Range.Relation rr) {
        return (n == null || key.node == n) &&
                (rid == null || (key.rid != null && key.rid.intValue() == rid.intValue())) &&
                (r == null || rr == null || rr.compare(key.range, r));
    }


    public Integer getSequence() {
        if (sequence != null) return sequence;

        sequence = 0;
        neuronInputs.stream().filter(sa -> !sa.synapse.key.isRecurrent).forEach(sa -> sequence = Math.max(sequence, sa.input.getSequence() + 1));
        return sequence;
    }


    public void markDirty(long v) {
        markedDirty = Math.max(markedDirty, v);
    }


    /**
     * The {@code SynapseActivation} mirror the synapse link in the network of activations.
     */
    public static class SynapseActivation {
        public final Synapse synapse;
        public final Activation input;
        public final Activation output;

        public static Comparator<SynapseActivation> INPUT_COMP = (sa1, sa2) -> {
            int r = Synapse.INPUT_SYNAPSE_COMP.compare(sa1.synapse, sa2.synapse);
            if (r != 0) return r;
            return sa1.input.compareTo(sa2.input);
        };

        public static Comparator<SynapseActivation> OUTPUT_COMP = (sa1, sa2) -> {
            int r = Synapse.OUTPUT_SYNAPSE_COMP.compare(sa1.synapse, sa2.synapse);
            if (r != 0) return r;
            return sa1.output.compareTo(sa2.output);
        };


        public SynapseActivation(Synapse s, Activation input, Activation output) {
            this.synapse = s;
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


        public Rounds() {
            rounds.put(0, State.ZERO);
        }


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
            return me != null ? me.getValue() : null;
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
            return !rounds.isEmpty() ? rounds.lastEntry().getValue() : State.ZERO;
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


        public void reset() {
            rounds.clear();
            rounds.put(0, State.ZERO);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            rounds.forEach((r, s) -> sb.append(r + ":" + s.value + " "));
            return sb.toString();
        }


        public boolean compare(Rounds r) {
            if(rounds.size() != r.rounds.size()) {
                return false;
            }
            for(Map.Entry<Integer, State> me: rounds.entrySet()) {
                State sa = me.getValue();
                State sb = r.rounds.get(me.getKey());
                if(sb == null || Math.abs(sa.value - sb.value) > 0.0000001) {
                    return false;
                }
            }

            return true;
        }


        public boolean isActive() {
            return rounds.size() <= 1 && getLast().value > 0.0;
        }
    }


    /**
     * A <code>State</code> object contains the activation value of an activation object that belongs to a neuron.
     * It furthermore contains a weight that is used to evaluate the interpretations during the search for the best
     * interpretation.
     */
    public static class State {
        public static final int DIR = 0;
        public static final int REC = 1;

        public final double value;

        public final int fired;
        public final Weight weight;

        public static final State ZERO = new State(0.0, -1, Weight.ZERO);

        public State(double value, int fired, Weight weight) {
            assert !Double.isNaN(value);
            this.value = value;
            this.fired = fired;
            this.weight = weight;
        }


        public boolean equals(State s) {
            return Math.abs(value - s.value) <= INeuron.WEIGHT_TOLERANCE;
        }

        public boolean equalsWithWeights(State s) {
            return equals(s) && weight.equals(s.weight);
        }

        public String toString() {
            return "V:" + Utils.round(value) + " " + weight;
        }
    }


    public String toString() {
        return key + " -" +
                " UB:" + Utils.round(upperBound) +
                (inputValue != null ? " IV:" + Utils.round(inputValue) : "") +
                (targetValue != null ? " TV:" + Utils.round(targetValue) : "") +
                " V:" + Utils.round(rounds.getLast().value) +
                " FV:" + Utils.round(finalRounds.getLast().value);
    }


    public String toString(boolean withTextSnippet, boolean withLogic) {
        StringBuilder sb = new StringBuilder();
        sb.append(id + " - ");

        sb.append("ID:" + inputDecision + " ");
        sb.append("D:" + decision + " ");
        sb.append("FD:" + finalDecision + " - ");

        sb.append(key.range);

        if(withTextSnippet) {
            sb.append(" \"");
            if(key.node.neuron.get().outputText != null) {
                sb.append(Utils.collapseText(key.node.neuron.get().outputText, 7));
            } else {
                sb.append(Utils.collapseText(doc.getText(key.range), 7));
            }
            sb.append("\"");
        }
        sb.append(" - ");

        sb.append(withLogic ? key.node.toString() : key.node.getNeuronLabel());

        sb.append(" - RID:");
        sb.append(key.rid);

        sb.append(" - UB:");
        sb.append(Utils.round(upperBound));

        sb.append(" - ");
        for (Map.Entry<Integer, State> me : rounds.rounds.entrySet()) {
            State s = me.getValue();
            sb.append("[R: " + me.getKey() + " " + s + "]");
        }

        if (isFinalActivation()) {
            State fs = getFinalState();
            sb.append(" - Final: " + fs);
        }

        if (inputValue != null) {
            sb.append(" - IV:" + Utils.round(inputValue));
        }

        if (targetValue != null) {
            sb.append(" - TV:" + Utils.round(targetValue));
        }

        return sb.toString();
    }


    public String linksToString() {
        StringBuilder sb = new StringBuilder();
        for(SynapseActivation sa: neuronInputs) {
            sb.append("  " + sa.input.getLabel() + "  W:" + sa.synapse.weight + "\n");
        }

        return sb.toString();
    }


    public enum Mode {OLD, NEW}

    public void saveOldState(Map<Activation, StateChange> changes, long v) {
        StateChange sc = currentStateChange;
        if (sc == null || currentStateV != v) {
            sc = new StateChange();
            sc.oldRounds = rounds.copy();
            currentStateChange = sc;
            currentStateV = v;
            if (changes != null) {
                changes.put(sc.getActivation(), sc);
            }
        }
    }

    public void saveNewState() {
        StateChange sc = currentStateChange;

        sc.newRounds = rounds.copy();
        sc.newState = decision;
        doc.fsQueue.add(candidate);
    }


    /**
     * The {@code StateChange} class is used to store the state change of an activation that occurs in each node of
     * the binary search tree. When a candidate refinement is selected during the search, then the activation values of
     * all affected activation objects are adjusted. The changes to the activation values are also propagated through
     * the network. The old state needs to be stored here in order for the search to be able to restore the old network
     * state before following the alternative search branch.
     */
    public class StateChange {
        public Rounds oldRounds;
        public Rounds newRounds;
        public Decision newState;

        public void restoreState(Mode m) {
            rounds = (m == Mode.OLD ? oldRounds : newRounds).copy();
        }

        public void setFinalState() {
            if (isFinalActivation()) {
                doc.finallyActivatedNeurons.remove(getINeuron());
            }

            finalRounds = newRounds.copy();

            if (isFinalActivation()) {
                doc.finallyActivatedNeurons.add(getINeuron());
            }
        }

        public Activation getActivation() {
            return Activation.this;
        }
    }


    public static class Builder {
        public Range range;
        public Integer rid;
        public double value = 1.0;
        public Double targetValue;
        public int fired;


        public Builder setRange(int begin, int end) {
            this.range = new Range(begin, end);
            return this;
        }

        public Builder setRange(Range range) {
            this.range = range;
            return this;
        }

        public Builder setRelationalId(Integer rid) {
            this.rid = rid;
            return this;
        }

        public Builder setValue(double value) {
            this.value = value;
            return this;
        }

        public Builder setTargetValue(Double targetValue) {
            this.targetValue = targetValue;
            return this;
        }

        public Builder setFired(int fired) {
            this.fired = fired;
            return this;
        }
    }
}

