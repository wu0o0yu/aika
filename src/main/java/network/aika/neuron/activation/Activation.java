package network.aika.neuron.activation;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Utils;
import network.aika.Writable;
import network.aika.lattice.InputNode.InputActivation;
import network.aika.lattice.NodeActivation;
import network.aika.lattice.OrNode.OrActivation;
import network.aika.neuron.INeuron;
import network.aika.neuron.INeuron.Type;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.SearchNode.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Linker.Direction.INPUT;
import static network.aika.neuron.activation.Linker.Direction.OUTPUT;
import static network.aika.neuron.activation.SearchNode.Decision.EXCLUDED;
import static network.aika.neuron.activation.SearchNode.Decision.SELECTED;
import static network.aika.neuron.activation.Activation.Link.INPUT_COMP;
import static network.aika.neuron.activation.Activation.Link.OUTPUT_COMP;
import static network.aika.neuron.INeuron.ALLOW_WEAK_NEGATIVE_WEIGHTS;
import static network.aika.neuron.activation.SearchNode.Decision.UNKNOWN;


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
 * contain the activation links within the logic layer. The fields {@code inputLinks} and
 * {@code outputLinks} contain the links on the neural layer.
 *
 * @author Lukas Molzberger
 */
public final class Activation implements Comparable<Activation> {

    public static int BEGIN = 0;
    public static int END = 1;

    public static final Comparator<Activation> ACTIVATION_ID_COMP = Comparator.comparingInt(act -> act.id);
    public static int MAX_SELF_REFERENCING_DEPTH = 5;
    public static int MAX_PREDECESSOR_DEPTH = 100;
    public static boolean DEBUG_OUTPUT = false;

    public static Activation MIN_ACTIVATION = new Activation(Integer.MIN_VALUE);
    public static Activation MAX_ACTIVATION = new Activation(Integer.MAX_VALUE);

    private static final Logger log = LoggerFactory.getLogger(Activation.class);

    private int id;
    private INeuron neuron;
    private Document doc;
    private long visited = 0;

    public Map<Integer, Position> slots = new TreeMap<>();
    private OrActivation inputNodeActivation;
    private InputActivation outputNodeActivation;
    private TreeSet<Link> selectedInputLinks = new TreeSet<>(INPUT_COMP);
    private TreeMap<Link, Link> inputLinks = new TreeMap<>(INPUT_COMP);
    private TreeMap<Link, Link> outputLinks = new TreeMap<>(OUTPUT_COMP);

    public Integer sequence;

    public double upperBound;
    public double lowerBound;

    public List<Option> options;

    public Rounds rounds = new Rounds();
    public Rounds finalRounds = rounds;

    public boolean ubQueued = false;
    public long markedHasCandidate;

    public long currentStateV;
    public StateChange currentStateChange;
    public long markedDirty;
    public long markedPredecessor;

    public Double targetValue;
    public Double inputValue;

    public Writable extension;


    public Decision inputDecision = Decision.UNKNOWN;
    public Decision decision = Decision.UNKNOWN;
    public Decision finalDecision = Decision.UNKNOWN;
    public Candidate candidate;
    private long visitedState;
    public long markedAncDesc;

    public boolean blocked;


    private List<Activation> conflicts;

    private Activation(int id) {
        this.id = id;
    }

    public Activation(Document doc, INeuron neuron) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = neuron;

        if(doc != null && doc.getModel().getActivationExtensionFactory() != null) {
            extension = doc.getModel().getActivationExtensionFactory().createObject();
        }
    }


    public void setInputNodeActivation(OrActivation inputNodeActivation) {
        this.inputNodeActivation = inputNodeActivation;
    }


    public OrActivation getInputNodeActivation() {
        return inputNodeActivation;
    }

    public InputActivation getOutputNodeActivation() {
        return outputNodeActivation;
    }

    public void setOutputNodeActivation(InputActivation outputNodeActivation) {
        this.outputNodeActivation = outputNodeActivation;
    }

    public Position getSlot(int slot) {
        return slots.get(slot);
    }


    public void setSlot(int slot, Position pos) {
        slots.put(slot, pos);
    }


    public void setSlots(Map<Integer, Position> slots) {
        for(Map.Entry<Integer, Position> me: slots.entrySet()) {
            setSlot(me.getKey(), me.getValue());
        }
    }


    public Integer length() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for(Position pos: slots.values()) {
            if(pos.getFinalPosition() == null) {
                return null;
            }
            min = Math.min(min, pos.getFinalPosition());
            max = Math.max(max, pos.getFinalPosition());
        }

        if(min > max) return 0;
        return max - min;
    }


    public void setTargetValue(Double targetValue) {
        this.targetValue = targetValue;
    }

    public int getId() {
        return id;
    }

    public Document getDocument() {
        return doc;
    }

    public int getThreadId() {
        return doc.getThreadId();
    }

    public long getNewVisitedId() {
        return doc.getNewVisitedId();
    }

    public long getVisitedId() {
        return visited;
    }

    public boolean checkVisited(long v) {
        if(visited == v) return false;
        visited = v;
        return true;
    }

    public String getLabel() {
        return getINeuron().label;
    }

    public Type getType() {
        return getINeuron().type;
    }

    public String getText() {
        return doc.getText(getSlot(BEGIN), getSlot(END));
    }


    public INeuron getINeuron() {
        return neuron;
    }


    public Neuron getNeuron() {
        return neuron.provider;
    }


    public void addLink(Linker.Direction dir, Link l) {
        switch(dir) {
            case INPUT:
                outputLinks.put(l, l);
                break;
            case OUTPUT:
                if(l.input.decision == SELECTED) {
                    selectedInputLinks.add(l);
                }
                inputLinks.put(l, l);
                break;
        }
    }


    public Link getLinkBySynapseId(int synapseId) {
        for(Link l: inputLinks.values()) {
            if(l.synapse.getId() == synapseId) {
                return l;
            }
        }
        return null;
    }


    public SortedSet<Link> getInputLinksOrderedBySynapse() {
        return inputLinks.navigableKeySet();
    }


    public Stream<Link> getInputLinks(boolean onlySelected) {
        return (onlySelected ? selectedInputLinks : inputLinks.values()).stream();
    }


    public Stream<Link> getOutputLinks() {
        return outputLinks.values().stream();
    }


    public Link getInputLink(Link l) {
        return inputLinks.get(l);
    }


    public Stream<Link> getInputLinksBySynapse(Synapse syn) {
        return inputLinks.subMap(
                new Link(syn, MIN_ACTIVATION, MIN_ACTIVATION),
                new Link(syn, MAX_ACTIVATION, MAX_ACTIVATION))
                .values()
                .stream();
    }


    public Stream<Link> getOutputLinksBySynapse(Synapse syn) {
        return outputLinks.subMap(
                new Link(syn, MIN_ACTIVATION, MIN_ACTIVATION),
                new Link(syn, MAX_ACTIVATION, MAX_ACTIVATION))
                .values()
                .stream();
    }


    public double process(SearchNode sn, int round, long v) {
        double delta = 0.0;
        State s;
        if(inputValue != null) {
            s = new State(inputValue, inputValue, 0.0, 0.0, 0, 0.0);
        } else {
            s = computeValueAndWeight(round);
        }

        if (round == 0 || !rounds.get(round).equalsWithWeights(s)) {
            saveOldState(sn.getModifiedActivations(), v);

            State oldState = rounds.get(round);

            boolean propagate = rounds.set(round, s) && (oldState == null || !oldState.equals(s));

            saveNewState();

            if (propagate) {
                if(round > Document.MAX_ROUND) {
                    log.error("Error: Maximum number of rounds reached. The network might be oscillating.");

                    doc.dumpOscillatingActivations();
                    throw new RuntimeException("Maximum number of rounds reached. The network might be oscillating.");
                } else {
                    if(Document.ROUND_LIMIT < 0 || round < Document.ROUND_LIMIT) {
                        doc.getValueQueue().propagateActivationValue(round, this);
                    }
                }
            }

            if (round == 0) {
                // In case that there is a positive feedback loop.
                doc.getValueQueue().add(1, this);
            }

            if (rounds.getLastRound() != null && round >= rounds.getLastRound()) { // Consider only the final round.
                delta += s.weight - oldState.weight;
            }
        }
        return delta;
    }


    public State computeValueAndWeight(int round) {
        INeuron n = getINeuron();
        double net = n.biasSum;
        double posNet = n.biasSum;

        int fired = -1;

        long v = doc.getNewVisitedId();
        markPredecessor(v, 0);

        for (InputState is: getInputStates(round, v)) {
            Synapse s = is.l.synapse;
            Activation iAct = is.l.input;

            if (iAct == this) continue;

            double x = Math.min(s.limit, is.s.value) * s.weight;
            if(s.getDistanceFunction() != null) {
                x *= s.getDistanceFunction().f(iAct, this);
            }
            net += x;
            if(!s.isNegative()) {
                posNet += x;
            }

            if (!s.isRecurrent() && !s.isNegative() && net >= 0.0 && fired < 0) {
                fired = iAct.rounds.get(round).fired + 1;
            }
        }

        if(n.passiveInputSynapses != null) {
            for(Synapse s: n.passiveInputSynapses.values()) {
                double x = s.weight * s.getInput().get(doc).passiveInputFunction.getActivationValue(s, this);

                net += x;
                if(!s.isNegative()) {
                    posNet += x;
                }
            }
        }

        double actValue = n.activationFunction.f(net);
        double posActValue = n.activationFunction.f(posNet);

        double w = Math.min(-n.negRecSum, net);

        // Compute only the recurrent part is above the threshold.
        double newWeight = decision == SELECTED ? Math.max(0.0, w) : 0.0;

        if(decision == SELECTED || ALLOW_WEAK_NEGATIVE_WEIGHTS) {
            return new State(
                    actValue,
                    posActValue,
                    net,
                    posNet,
                    -1,
                    newWeight
            );
        } else {
            return new State(
                    0.0,
                    posActValue,
                    0.0,
                    posNet,
                    -1,
                    newWeight
            );
        }
    }


    public boolean isActiveable() {
        INeuron n = getINeuron();
        double net = n.biasSum;

        for (Link l: inputLinks.values()) {
            if(l.synapse.isInactive()) {
                continue;
            }

            Synapse s = l.synapse;
            Activation iAct = l.input;

            if (iAct == this) continue;

            double iv = 0.0;
            if(!l.synapse.isNegative() && l.input.decision != EXCLUDED) {
                iv = Math.min(l.synapse.limit, l.input.upperBound);
            }

            double x = iv * s.weight;
            if(s.getDistanceFunction() != null) {
                x *= s.getDistanceFunction().f(iAct, this);
            }
            net += x;
        }

        if(n.passiveInputSynapses != null) {
            for(Synapse s: n.passiveInputSynapses.values()) {
                double x = s.weight * s.getInput().get(doc).passiveInputFunction.getActivationValue(s, this);

                net += x;
            }
        }

        return net > 0.0;
    }


    public void processBounds() {
        double oldUpperBound = upperBound;

        computeBounds();

        if(Math.abs(upperBound - oldUpperBound) > 0.01) {
            for(Link l: outputLinks.values()) {
                doc.addToUpperBoundQueue(l);
            }
        }

        if (oldUpperBound <= 0.0 && upperBound > 0.0 && !blocked) {
            getINeuron().propagate(this);
        }
    }


    public void computeBounds() {
        INeuron n = getINeuron();
        double ub = n.biasSum + n.posRecSum;
        double lb = n.biasSum + n.posRecSum;

        long v = doc.getNewVisitedId();
        markPredecessor(v, 0);

        for (Link l : inputLinks.values()) {
            Synapse s = l.synapse;
            if(s.isInactive()) {
                continue;
            }

            Activation iAct = l.input;

            if (iAct == this) continue;

            double x = s.weight;
            if(s.getDistanceFunction() != null) {
                x *= s.getDistanceFunction().f(iAct, this);
            }

            if (s.isNegative()) {
                if (!s.isRecurrent() && !iAct.checkSelfReferencing(false, 0, v)) {
                    ub += Math.min(s.limit, iAct.lowerBound) * x;
                }

                lb += s.limit * x;
            } else {
                ub += Math.min(s.limit, iAct.upperBound) * x;
                lb += Math.min(s.limit, iAct.lowerBound) * x;
            }
        }

        if(n.passiveInputSynapses != null) {
            for(Synapse s: n.passiveInputSynapses.values()) {
                double x = s.weight * s.getInput().get(doc).passiveInputFunction.getActivationValue(s, this);

                ub += x;
                lb += x;
            }
        }

        upperBound = n.activationFunction.f(ub);
        lowerBound = n.activationFunction.f(lb);
    }


    private static State getInitialState(Decision c) {
        return new State(
                c == SELECTED ? 1.0 : 0.0,
                0.0,
                0.0,
                0.0,
                0,
                0.0
        );
    }



    private List<InputState> getInputStates(int round, long v) {
        ArrayList<InputState> tmp = new ArrayList<>();
        Synapse lastSynapse = null;
        InputState maxInputState = null;
        for (Link l : inputLinks.values()) {
            if(l.synapse.isInactive()) {
                continue;
            }
            if (lastSynapse != null && lastSynapse != l.synapse) {
                tmp.add(maxInputState);
                maxInputState = null;
            }

            State s = l.input.getInputState(round, l.synapse, v);
            if (maxInputState == null || maxInputState.s.value < s.value) {
                maxInputState = new InputState(l, s);
            }
            lastSynapse = l.synapse;
        }
        if (maxInputState != null) {
            tmp.add(maxInputState);
        }

        return tmp;
    }


    public ActivationFunction getActivationFunction() {
        return getINeuron().activationFunction;
    }

    /*
    An activable activation object might still be suppressed by an undecided positive feedback link.
     */
    public boolean hasUndecidedPositiveFeedbackLinks() {
        return getInputLinks(false)
                .anyMatch(l -> l.isRecurrent() && !l.isNegative() && l.input.decision == UNKNOWN);
    }


    private static class InputState {
        public InputState(Link l, State s) {
            this.l = l;
            this.s = s;
        }

        Link l;
        State s;
    }


    private State getInputState(int round, Synapse s, long v) {
        State is = State.ZERO;
        if (s.isRecurrent()) {
            if (!s.isNegative() || !checkSelfReferencing(true, 0, v)) {
                is = round == 0 ? getInitialState(decision) : rounds.get(round - 1);
            }
        } else {
            is = rounds.get(round);
        }
        return is;
    }


    public List<Link> getFinalInputActivationLinks() {
        ArrayList<Link> results = new ArrayList<>();
        for (Link l : inputLinks.values()) {
            if (l.input.isFinalActivation()) {
                results.add(l);
            }
        }
        return results;
    }


    public List<Link> getFinalOutputActivationLinks() {
        ArrayList<Link> results = new ArrayList<>();
        for (Link l : outputLinks.values()) {
            if (l.output.isFinalActivation()) {
                results.add(l);
            }
        }
        return results;
    }


    public Collection<Activation> getConflicts() {
        if(conflicts != null) {
            return conflicts;
        }

        long v = doc.getNewVisitedId();
        markPredecessor(v, 0);
        conflicts = new ArrayList<>();
        for(Link l: inputLinks.values()) {
            if (l.isNegative() && l.isRecurrent()) {
                l.input.collectIncomingConflicts(conflicts, v);
            }
        }
        collectOutgoingConflicts(conflicts, v);
        return conflicts;
    }


    private void collectIncomingConflicts(List<Activation> conflicts, long v) {
        if(markedPredecessor == v) return;

        if (getINeuron().type != INeuron.Type.INHIBITORY) {
            conflicts.add(this);
        } else {
            for (Link l : inputLinks.values()) {
                if (!l.isNegative() && !l.isRecurrent()) {
                    l.input.collectIncomingConflicts(conflicts, v);
                }
            }
        }
    }


    private void collectOutgoingConflicts(List<Activation> conflicts, long v) {
        if(markedPredecessor == v) return;

        for(Link l: outputLinks.values()) {
            if (l.output.getINeuron().type != INeuron.Type.INHIBITORY) {
                if (l.isNegative() && l.isRecurrent()) {
                    conflicts.add(l.output);
                }
            } else if (!l.isNegative() && !l.isRecurrent()) {
                l.output.collectOutgoingConflicts(conflicts, v);
            }
        }
    }


    public void adjustSelectedNeuronInputs(Decision d) {
        for(Link l: outputLinks.values()) {
            if(d == SELECTED) {
                l.output.selectedInputLinks.add(l);
            } else {
                l.output.selectedInputLinks.remove(l);
            }
        }
    }


    public boolean checkSelfReferencing(boolean onlySelected, int depth, long v) {
        if (markedPredecessor == v) {
            return true;
        }

        if (depth > MAX_SELF_REFERENCING_DEPTH) {
            return false;
        }

        for (Link l: onlySelected ? selectedInputLinks : inputLinks.values()) {
            if(!l.synapse.isNegative()) {
                if (l.input.checkSelfReferencing(onlySelected, depth + 1, v)) {
                    return true;
                }
            }
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


    public Integer getSequence() {
        if (sequence != null) return sequence;

        sequence = 0;
        inputLinks
                .values()
                .stream()
                .filter(l -> !l.isRecurrent())
                .forEach(l -> sequence = Math.max(sequence, l.input.getSequence() + 1));
        return sequence;
    }


    public void markDirty(long v) {
        markedDirty = Math.max(markedDirty, v);
    }


    public void markPredecessor(long v, int depth) {
        if(depth > MAX_PREDECESSOR_DEPTH) {
            throw new RuntimeException("MAX_PREDECESSOR_DEPTH limit exceeded. Probable cause is a non recurrent loop.");
        }

        markedPredecessor = v;

        for(Link l: inputLinks.values()) {
            if(!l.isNegative() && !l.isRecurrent()) {
                l.input.markPredecessor(v, depth + 1);
            }
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
     * It furthermore contains a weight that is used to check the interpretations during the search for the best
     * interpretation.
     */
    public static class State {
        public final double value;
        public final double posValue;
        public final double net;
        public final double posNet;

        public final int fired;
        public final double weight;

        public static final State ZERO = new State(0.0, 0.0, 0.0, 0.0, -1, 0.0);

        public State(double value, double posValue, double net, double posNet, int fired, double weight) {
            assert !Double.isNaN(value);
            this.value = value;
            this.posValue = posValue;
            this.net = net;
            this.posNet = posNet;
            this.fired = fired;
            this.weight = weight;
        }


        public boolean equals(State s) {
            return Math.abs(value - s.value) <= INeuron.WEIGHT_TOLERANCE;
        }

        public boolean equalsWithWeights(State s) {
            return equals(s) && Math.abs(weight - s.weight) <= INeuron.WEIGHT_TOLERANCE;
        }

        public String toString() {
            return "V:" + Utils.round(value) + (DEBUG_OUTPUT ? " pV:" + Utils.round(posValue) : "") + " Net:" + Utils.round(net) + " W:" + Utils.round(weight);
        }
    }


    public String toString() {
        return id + " " + slotsToString() + " " + identityToString() + " - " +
                (extension != null ? extension.toString() + " -" : "") +
                " UB:" + Utils.round(upperBound) +
                (inputValue != null ? " IV:" + Utils.round(inputValue) : "") +
                (targetValue != null ? " TV:" + Utils.round(targetValue) : "") +
                " V:" + Utils.round(rounds.getLast().value) +
                " FV:" + Utils.round(finalRounds.getLast().value);
    }



    public String toStringDetailed() {
        StringBuilder sb = new StringBuilder();
        sb.append(id + " - ");

        sb.append(finalDecision + " - ");

        sb.append(slotsToString());

        sb.append(" \"");
        if (getINeuron().getOutputText() != null) {
            sb.append(Utils.collapseText(getINeuron().getOutputText(), 7));
        } else {
            sb.append(Utils.collapseText(doc.getText(getSlot(BEGIN), getSlot(END)), 7));
        }
        sb.append("\"");

        sb.append(identityToString());
        sb.append(" - ");

        sb.append(getLabel());

        if(extension != null) {
            sb.append(" - " + extension);
        }

        if(DEBUG_OUTPUT) {
            sb.append(" - UB:");
            sb.append(Utils.round(upperBound));
        }

        if(SearchNode.COMPUTE_SOFT_MAX) {
            sb.append(" AVG:");
            sb.append(getAvgState());
        }

        sb.append(" - ");
        if (isFinalActivation()) {
            State fs = getFinalState();
            sb.append(fs);
        }


        if (inputValue != null) {
            sb.append(" - IV:" + Utils.round(inputValue));
        }

        if (targetValue != null) {
            sb.append(" - TV:" + Utils.round(targetValue));
        }

        return sb.toString();
    }

    public State getAvgState() {
        if (options == null) {
            return null;
        }

        double avgValue = 0.0;
        double avgPosValue = 0.0;
        double avgNet = 0.0;
        double avgPosNet = 0.0;

        for (Option option : options) {
            if (option.decision == SELECTED) {
                double p = option.p;
                Activation.State s = option.state;

                avgValue += p * s.value;
                avgPosValue += p * s.posValue;
                avgNet += p * s.net;
                avgPosNet += p * s.posNet;
            }
        }
        return new Activation.State(avgValue, avgPosValue, avgNet, avgPosNet, 0, 0.0);
    }



    @Override
    public int compareTo(Activation act) {
        return Integer.compare(id, act.id);
    }


    public String slotsToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean first = true;
        for(Map.Entry<Integer, Position> me: slots.entrySet()) {
            if(!first) {
                sb.append(", ");
            }
            first = false;

            sb.append(me.getKey());
            sb.append(":");
            sb.append(me.getValue());
        }
        sb.append(")");
        return sb.toString();
    }


    public String identityToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" (");
        boolean first = true;
        for(Link l: inputLinks.values()) {
            if(l.isIdentity()) {
                if(!first) {
                    sb.append(", ");
                }

                sb.append(l.input.id);

                first = false;
            }
        }
        sb.append(")");
        return sb.toString();
    }


    public String linksToString() {
        StringBuilder sb = new StringBuilder();
        for(Link l: inputLinks.values()) {
            sb.append("  " + l.input.getLabel() + "  W:" + l.synapse.weight + "\n");
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

        public Activation getActivation() {
            return Activation.this;
        }
    }


    public class Option {
        public int snId;
        public State state;
        public Decision decision;

        public double weight;
        public int cacheFactor = 1;
        public double p;

        public Map<Link, Option> inputOptions = new TreeMap<>(INPUT_COMP);
        public Map<Link, Option> outputOptions = new TreeMap<>(OUTPUT_COMP); // TODO:


        public Option(int snId, Decision d) {
            this.snId = snId;
            this.state = rounds.getLast();
            this.decision = d;

            if (options == null) {
                options = new ArrayList<>();
            }
            options.add(this);
        }

        public void setWeight(double weight) {
            this.weight = weight;

            for(Link l: inputLinks.values()) {
                if(l.input.decision == SELECTED) {
                    if(l.input.candidate != null) {
                        if (l.input.candidate.id < candidate.id) {
                            SearchNode inputSN = l.input.candidate.currentSearchNode.getParent();

                            link(l, inputSN.getCurrentOption());
                        }
                    } else {
                        link(l, l.input.options.get(0));
                    }
                }
            }

            for(Link l: outputLinks.values()) {
                if(l.input.decision == SELECTED) {
                    if(l.output.candidate != null) {
                        if(l.output.candidate.id < candidate.id) {
                            SearchNode outputSN = l.output.candidate.currentSearchNode.getParent();

                            outputSN.getCurrentOption().link(l, this);
                        }
                    }
                }
            }
        }

        public void link(Link l, Option in) {
            inputOptions.put(l, in);
            in.outputOptions.put(l, this);
        }

        public void setCacheFactor(int cf) {
            cacheFactor = cf;
        }

        public Activation getAct() {
            return Activation.this;
        }

        public String toString() {
            return " snId:" + snId + " d:"  + decision + " cacheFactor:" + cacheFactor + " w:" + Utils.round(weight) + " p:" + p + " " + state;
        }
    }


    /**
     * The {@code SynapseActivation} mirror the synapse link in the network of activations.
     */
    public static class Link {
        private final Synapse synapse;
        private final Activation input;
        private final Activation output;

        public static Comparator<Link> INPUT_COMP = (l1, l2) -> {
            int r = Synapse.INPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
            if (r != 0) return r;
            return Integer.compare(l1.input.id, l2.input.id);
        };

        public static Comparator<Link> OUTPUT_COMP = (l1, l2) -> {
            int r = Synapse.OUTPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
            if (r != 0) return r;
            return Integer.compare(l1.output.id, l2.output.id);
        };


        public Link(Synapse s, Activation input, Activation output) {
            this.synapse = s;
            this.input = input;
            this.output = output;
        }

        public Synapse getSynapse() {
            return synapse;
        }

        public Activation getInput() {
            return input;
        }

        public Activation getOutput() {
            return output;
        }

        public boolean isRecurrent() {
            return synapse.isRecurrent();
        }

        public boolean isIdentity() {
            return synapse.isIdentity();
        }

        public boolean isNegative() {
            return synapse.isNegative();
        }

        public void link() {
            input.addLink(INPUT, this);
            output.addLink(OUTPUT, this);
        }

        public String toString() {
            return synapse + ": " + input + " --> " + output;
        }
    }

    public static class Builder {
        public SortedMap<Integer, Integer> positions = new TreeMap<>();
        public double value = 1.0;
        public double net = 0.0;
        public Double targetValue;
        public int fired;


        public Builder setRange(int begin, int end) {
            setPosition(Activation.BEGIN, begin);
            setPosition(Activation.END, end);
            return this;
        }

        public Builder setPosition(int slot, int pos) {
            positions.put(slot, pos);
            return this;
        }

        public Builder setValue(double value) {
            this.value = value;
            return this;
        }

        public Builder setNet(double net) {
            this.net = net;
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

