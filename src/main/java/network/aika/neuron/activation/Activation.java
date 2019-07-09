package network.aika.neuron.activation;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Utils;
import network.aika.lattice.InputNode.InputActivation;
import network.aika.lattice.OrNode.OrActivation;
import network.aika.neuron.INeuron;
import network.aika.neuron.INeuron.SynapseSummary;
import network.aika.neuron.INeuron.Type;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.Document.MAX_ROUND;
import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.INeuron.Type.INHIBITORY;
import static network.aika.neuron.activation.Decision.*;
import static network.aika.neuron.activation.Linker.Direction.INPUT;
import static network.aika.neuron.activation.Linker.Direction.OUTPUT;
import static network.aika.neuron.activation.Activation.Link.INPUT_COMP;
import static network.aika.neuron.activation.Activation.Link.OUTPUT_COMP;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.State.ZERO;


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
public class Activation implements Comparable<Activation> {

    public static int BEGIN = 0;
    public static int END = 1;

    public static final Comparator<Activation> ACTIVATION_ID_COMP = Comparator.comparingInt(act -> act.id);
    public static int MAX_SELF_REFERENCING_DEPTH = 5;
    public static boolean DEBUG_OUTPUT = false;

    public static final Activation MIN_ACTIVATION = new Activation(Integer.MIN_VALUE);
    public static final Activation MAX_ACTIVATION = new Activation(Integer.MAX_VALUE);

    private static final Logger log = LoggerFactory.getLogger(Activation.class);

    private int id;
    private INeuron neuron;
    private Document doc;
    private long visited = 0;

    private Map<Integer, Position> slots = new TreeMap<>();
    private OrActivation inputNodeActivation;
    private InputActivation outputNodeActivation;
    private TreeMap<Link, Link> inputLinks = new TreeMap<>(INPUT_COMP);
    private TreeMap<Link, Link> outputLinks = new TreeMap<>(OUTPUT_COMP);

    private double upperBound;
    private double lowerBound;

    public Option rootOption = new Option(this, null, UNKNOWN);
    public Option currentOption = rootOption;
    public Option finalOption;

    boolean ubQueued = false;
    private long markedHasCandidate;

    long markedDirty;

    private Double targetValue;
    private Double inputValue;

    Decision inputDecision = Decision.UNKNOWN;
    public Decision finalDecision = Decision.UNKNOWN;
    CurrentSearchState currentSearchState = new CurrentSearchState();

    private Integer sequence;
    private Integer candidateId;

    private long visitedState;
    public long markedAncDesc;

    public boolean blocked;


    public static Comparator<Activation> CANDIDATE_COMP = (act1, act2) -> {
        Iterator<Map.Entry<Integer, Position>> ita = act1.getSlots().entrySet().iterator();
        Iterator<Map.Entry<Integer, Position>> itb = act2.getSlots().entrySet().iterator();

        Map.Entry<Integer, Position> mea;
        Map.Entry<Integer, Position> meb;
        while (ita.hasNext() || itb.hasNext()) {
            mea = ita.hasNext() ? ita.next() : null;
            meb = itb.hasNext() ? itb.next() : null;

            if (mea == null && meb == null) {
                break;
            } else if (mea == null && meb != null) {
                return -1;
            } else if (mea != null && meb == null) {
                return 1;
            }

            int r = Integer.compare(mea.getKey(), meb.getKey());
            if (r != 0) return r;
            r = Position.compare(act1.lookupSlot(mea.getKey()), act2.lookupSlot(meb.getKey()));
            if (r != 0) return r;
        }

        int r = Integer.compare(act1.getSequence(), act2.getSequence());
        if (r != 0) return r;

        return Integer.compare(act1.getCandidateId(), act2.getCandidateId());
    };


    private Activation(int id) {
        this.id = id;
    }


    public Activation(Document doc, INeuron neuron, Map<Integer, Position> slots) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = neuron;
        this.slots = slots;

        neuron.register(this);
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


    public Map<Integer, Position> getSlots() {
        return slots;
    }

    public Position lookupSlot(int slot) {
        Position pos = slots.get(slot);
        if(pos == null) {
            pos = new Position(doc);
            slots.put(slot, pos);
        }

        return pos;
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
        return getINeuron().getLabel();
    }

    public Type getType() {
        return getINeuron().getType();
    }

    public String getText() {
        return doc.getText(lookupSlot(BEGIN), lookupSlot(END));
    }


    public INeuron getINeuron() {
        return neuron;
    }


    public Neuron getNeuron() {
        return neuron.getProvider();
    }


    public Synapse getSynapseById(int synapseId) {
        return getNeuron().getSynapseById(synapseId);
    }


    public boolean checkDependenciesSatisfied(long v) {
        return !getInputLinks()
                .anyMatch(l -> l.getInput().markedHasCandidate != v && !l.isRecurrent() && l.getInput().getUpperBound() > 0.0);
    }


    public void markHasCandidate(long v) {
        markedHasCandidate = v;

        for(Link l: outputLinks.values()) {
            if(l.getOutput().getType() == INHIBITORY) {
                l.getOutput().markHasCandidate(v);
            }
        }
    }


    public double getUpperBound() {
        return upperBound;
    }


    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }


    public double getLowerBound() {
        return lowerBound;
    }


    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Double getTargetValue() {
        return targetValue;
    }

    public Double getInputValue() {
        return inputValue;
    }

    public Decision getInputDecision() {
        return inputDecision;
    }

    public Decision getDecision() {
        return currentSearchState.decision;
    }

    public Decision getFinalDecision() {
        return finalDecision;
    }

    public void addLink(Linker.Direction dir, Link l) {
        switch(dir) {
            case INPUT:
                outputLinks.put(l, l);
                break;
            case OUTPUT:
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



    public Stream<Link> getInputLinks() {
        return inputLinks.values().stream();
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


    public double process(SearchNode sn) throws OscillatingActivationsException, RecursiveDepthExceededException {
        State oldState = currentOption.get();
        State s = computeValueAndWeight();

        if (currentOption.decision == UNKNOWN || currentOption.searchNode != sn) {
            if(currentOption.get().equalsWithWeights(s)) {
                return 0.0;
            }

            saveState(sn);
        }

        if (currentOption.set(s) && (oldState == null || !oldState.equals(s))) {
            doc.getValueQueue().propagateActivationValue(this);
        }

        return s.weight - oldState.weight;
    }


    public State computeValueAndWeight() throws RecursiveDepthExceededException {
        INeuron n = getINeuron();
        INeuron.SynapseSummary ss = n.getSynapseSummary();

        double net = n.getTotalBias(CURRENT);
        double netUB = net;
        double posNet = net;

        int fired = -1;

        for (InputState is: getInputStates()) {
            Synapse s = is.l.synapse;
            Activation iAct = is.l.input;

            if (iAct == this) continue;

            double x = Math.min(s.getLimit(), is.s.value) * s.getWeight();
            net += x;
            netUB += Math.min(s.getLimit(), is.s.ub) * s.getWeight();

            net += s.computeRelationWeights(is.l);

            if(!s.isNegative(CURRENT)) {
                posNet += x;
            }

            if (!s.isRecurrent() && !s.isNegative(CURRENT) && net >= 0.0 && fired < 0) {
                fired = iAct.currentOption.get().fired + 1;
            }
        }

        for(Synapse s : n.getPassiveInputSynapses()) {
            double x = s.getWeight() * s.getInput().getPassiveInputFunction().getActivationValue(s, this);

            net += x;
            netUB += x;
            if (!s.isNegative(CURRENT)) {
                posNet += x;
            }
        }

        double actValue = n.getActivationFunction().f(net);
        double actUBValue = n.getActivationFunction().f(netUB);
        double posActValue = n.getActivationFunction().f(posNet);

        double w = Math.min(-ss.getNegRecSum(), net);

        // Compute only the recurrent part is above the threshold.
        double newWeight = getDecision() == SELECTED ? Math.max(0.0, w) : 0.0;

        return new State(
                actValue,
                actUBValue,
                posActValue,
                net,
                posNet,
                fired,
                newWeight
        );
    }


    public void processBounds() throws RecursiveDepthExceededException {
        double oldUpperBound = upperBound;

        computeBounds();

        if(Math.abs(upperBound - oldUpperBound) > 0.01) {
            for(Link l: outputLinks.values()) {
                doc.getUpperBoundQueue().add(l);
            }
        }

        if (oldUpperBound <= 0.0 && upperBound > 0.0 && !blocked) {
            getINeuron().propagate(this);
        }
    }


    public void computeBounds() throws RecursiveDepthExceededException {
        INeuron n = getINeuron();
        SynapseSummary ss = n.getSynapseSummary();

        double ub = n.getTotalBias(CURRENT) + ss.getPosRecSum();
        double lb = n.getTotalBias(CURRENT) + ss.getPosRecSum();

        long v = doc.getNewVisitedId();

        for (Link l : inputLinks.values()) {
            Synapse s = l.synapse;
            if(s.isInactive()) {
                continue;
            }

            Activation iAct = l.input;

            if (iAct == this) continue;

            double x = s.getWeight();

            if (s.isNegative(CURRENT)) {
                if (!s.isRecurrent() && !iAct.checkSelfReferencing(this, 0)) {
                    ub += Math.min(s.getLimit(), iAct.lowerBound) * x;
                }

                lb += s.getLimit() * x;
            } else {
                ub += Math.min(s.getLimit(), iAct.upperBound) * x;
                lb += Math.min(s.getLimit(), iAct.lowerBound) * x;

                double rlw = s.computeRelationWeights(l);
                ub += rlw;
                lb += rlw;
            }
        }

        for(Synapse s : n.getPassiveInputSynapses()) {
            double x = s.getWeight() * s.getInput().getPassiveInputFunction().getActivationValue(s, this);

            ub += x;
            lb += x;
        }

        upperBound = n.getActivationFunction().f(ub);
        lowerBound = n.getActivationFunction().f(lb);
    }


    private List<InputState> getInputStates() {
        ArrayList<InputState> tmp = new ArrayList<>();
        Synapse lastSynapse = null;
        InputState maxInputState = null;
        for (Link l : inputLinks.values()) {
            if(l.isInactive()) {
                continue;
            }
            if (lastSynapse != null && lastSynapse != l.synapse) {
                tmp.add(maxInputState);
                maxInputState = null;
            }

            State s = l.input.getInputState(l.synapse, this);
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
        return getINeuron().getActivationFunction();
    }


    public void setInputState(Builder input) {
        rootOption.decision = SELECTED;
        rootOption.p = 1.0;
        rootOption.set(new State(input.value, input.value, input.value, input.net, 0.0, input.fired, 0.0));
        currentOption = rootOption;
        finalOption = rootOption;

        inputValue = input.value;
        upperBound = input.value;
        lowerBound = input.value;
        targetValue = input.targetValue;

        inputDecision = SELECTED;
        finalDecision = inputDecision;
        setDecision(inputDecision, doc.getNewVisitedId(), null);
    }


    private static class InputState {
        public InputState(Link l, State s) {
            this.l = l;
            this.s = s;
        }

        Link l;
        State s;
    }


    private State getInputState(Synapse s, Activation act) {
        double value = 0.0;
        double ub = 0.0;

        State is = currentOption.get();
        if (getType() == INHIBITORY) {
            if(act.getDecision() == SELECTED) {
                value = is.ub;
            } else {
                value = is.value;
            }
        } else {
            if(getDecision() == UNKNOWN) {
                if (s.isNegative(CURRENT)) {
                    if(!checkSelfReferencing(act, 0)) {
                        if(act.getDecision() == EXCLUDED) {
                            value = s.getLimit();
                        }
                    }
                } else {
                    if(act.getDecision() == SELECTED) {
                        ub = s.getLimit();
                        value = s.getLimit();
                    } else if(act.getDecision() == UNKNOWN) {
                        ub = s.getLimit();
                    }
                }
            } else {
                ub = is.value;
                value = is.value;
            }
        }

        return new State(
                value,
                ub,
                0.0,
                0.0,
                0.0,
                0,
                0.0
        );
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


    public boolean checkSelfReferencing(Activation act, int depth) {
        if (this == act) {
            return true;
        }

        if(getType() != INHIBITORY || depth > MAX_SELF_REFERENCING_DEPTH) {
            return false;
        }

        Link maxLink = null;
        double maxValue = 0.0;
        for (Link l: inputLinks.values()) {
            double v = l.getInput().currentOption.getLast().value;
            if(maxLink == null || v < maxValue) {
                maxLink = l;
                maxValue = v;
            }
        }

        return maxLink.input.checkSelfReferencing(act, depth + 1);
    }


    public void setDecision(Decision newDecision, long v, SearchNode sn) {
        if(inputDecision != Decision.UNKNOWN && newDecision != inputDecision) return;

        if (newDecision == Decision.UNKNOWN && v != visitedState) return;

        currentSearchState.decision = newDecision;
        visitedState = v;
/*
        if(SearchNode.COMPUTE_SOFT_MAX && newDecision != UNKNOWN) {
            new Option(this, sn, newDecision);
        }
*/
    }


    public Collection<Activation> getConflicts() {
        ArrayList<Activation> results = new ArrayList<>();
        for(Link l: inputLinks.values()) {
            if(l.isRecurrent() && l.isNegative(CURRENT)) {
                results.add(l.input);
            }
        }
        return results;
    }


    public boolean isFinalActivation() {
        return getFinalState().value > 0.0;
    }


    public State getFinalState() {
        return finalOption != null ? finalOption.getLast() : ZERO;
    }


    public double getValue() {
        return getFinalState().value;
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


    public Integer getCandidateId() {
        return candidateId;
    }


    public void setCandidateId(Integer candidateId) {
        this.candidateId = candidateId;
    }


    public void markDirty(long v) {
        markedDirty = Math.max(markedDirty, v);
    }


    public boolean match(Predicate<Link> filter) {
        Synapse ls = null;
        boolean matched = false;
        for(Link l: inputLinks.navigableKeySet()) {
            Synapse s = l.getSynapse();

            if(ls != null && ls != s) {
                if(!matched) {
                    return false;
                }
                matched = false;
            }

            if(filter.test(l)) {
                matched = true;
            }

            ls = s;
        }

        return matched;
    }


    public String toString() {
        return id + " " + getNeuron().getId() + ":" + getLabel() + " " + slotsToString() + " " + identityToString() + " - " +
                " UB:" + Utils.round(upperBound) +
                (inputValue != null ? " IV:" + Utils.round(inputValue) : "") +
                (targetValue != null ? " TV:" + Utils.round(targetValue) : "") +
                " V:" + Utils.round(currentOption.getLast().value) +
                " FV:" + Utils.round(finalOption.getLast().value);
    }


    public String searchStateToString() {
        return id + " " + getNeuron().getId() + ":" + getLabel() + " " + currentSearchState.toString();
    }


    public String toStringDetailed() {
        StringBuilder sb = new StringBuilder();
        sb.append(id + " - ");

        if(getType() == EXCITATORY) {
            sb.append((finalDecision != null ? finalDecision : "X") + " - ");
        }

        sb.append(slotsToString());

        sb.append(" \"");
        if (getINeuron().getOutputText() != null) {
            sb.append(Utils.collapseText(getINeuron().getOutputText(), 7));
        } else {
            sb.append(Utils.collapseText(doc.getText(lookupSlot(BEGIN), lookupSlot(END)), 7));
        }
        sb.append("\"");

        sb.append(identityToString());
        sb.append(" - ");

        sb.append(getLabel());

        if(DEBUG_OUTPUT) {
            sb.append(" - UB:");
            sb.append(Utils.round(upperBound));
        }

        if(SearchNode.COMPUTE_SOFT_MAX) {
            sb.append(" Exp:");
            sb.append(getExpectedState());
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


    public State getExpectedState() {
/*        if (options == null) {
            return null;
        }

        double value = 0.0;
        double ub = 0.0;
        double posValue = 0.0;
        double net = 0.0;
        double posNet = 0.0;

        for (Option option : options) {
            if (option.decision == SELECTED) {
                double p = option.p;
                State s = option.getLast();

                value += p * s.value;
                ub += p * s.ub;
                posValue += p * s.posValue;
                net += p * s.net;
                posNet += p * s.posNet;
            }
        }
        return new State(value, ub, posValue, net, posNet, 0, 0.0);
*/
        return ZERO;
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
            sb.append("  " + l.input.getLabel() + "  W:" + l.synapse.getWeight() + "\n");
        }

        return sb.toString();
    }


    public enum Mode {OLD, NEW}


    public void saveState(SearchNode sn) {
        StateChange sc = new StateChange();
        currentOption.fixed = true;
        sc.oldOption = currentOption;
        currentOption = new Option(this, sn, getDecision());
        sc.newOption = currentOption;
        sc.newState = currentOption.decision;

        sc.oldOption.child = sc.newOption;
        sc.newOption.parent = sc.oldOption;

        if (sn.getModifiedActivations() != null) {
            sn.getModifiedActivations().put(sc.getActivation(), sc);
        }
    }


    /**
     * The {@code StateChange} class is used to store the state change of an activation that occurs in each node of
     * the binary search tree. When a currentSearchState refinement is selected during the search, then the activation values of
     * all affected activation objects are adjusted. The changes to the activation values are also propagated through
     * the network. The old state needs to be stored here in order for the search to be able to restore the old network
     * state before following the alternative search branch.
     */
    public class StateChange {
        public Option oldOption;
        public Option newOption;
        public Decision newState;

        public void restoreState(Mode m) {
            currentOption = (m == Mode.OLD ? oldOption : newOption);
            assert currentOption.fixed;
        }

        public Activation getActivation() {
            return Activation.this;
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

        public boolean isNegative(Synapse.State s) {
            return synapse.isNegative(s);
        }

        public boolean isInactive() {
            return synapse.isInactive();
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

        public Map<Integer, Position> getSlots(Document doc) {
            TreeMap<Integer, Position> slots = new TreeMap<>();
            for(Map.Entry<Integer, Integer> me: positions.entrySet()) {
                slots.put(me.getKey(), doc.lookupFinalPosition(me.getValue()));
            }
            return slots;
        }
    }


    public static class OscillatingActivationsException extends RuntimeException {

        private String activationsDump;

        public OscillatingActivationsException(String activationsDump) {
            super("Maximum number of rounds reached. The network might be oscillating.");

            this.activationsDump = activationsDump;
        }


        public String getActivationsDump() {
            return activationsDump;
        }
    }


    public static class RecursiveDepthExceededException extends RuntimeException {

        public RecursiveDepthExceededException() {
            super("MAX_PREDECESSOR_DEPTH limit exceeded. Probable cause is a non recurrent loop.");
        }
    }
}

