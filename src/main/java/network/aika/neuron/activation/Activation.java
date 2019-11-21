/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.neuron.activation;

import network.aika.Document;
import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.INeuron.SynapseSummary;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.link.Direction;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.activation.search.Decision;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.activation.search.SearchNode;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.neuron.activation.search.Decision.*;
import static network.aika.neuron.activation.link.Link.INPUT_COMP;
import static network.aika.neuron.activation.link.Link.OUTPUT_COMP;
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
public abstract class Activation implements Comparable<Activation> {

    public static final Comparator<Activation> ACTIVATION_ID_COMP = Comparator.comparingInt(act -> act.id);
    public static int MAX_SELF_REFERENCING_DEPTH = 5;
    public static boolean DEBUG_OUTPUT = false;

    public static final Activation MIN_ACTIVATION = new ExcitatoryActivation(Integer.MIN_VALUE);
    public static final Activation MAX_ACTIVATION = new ExcitatoryActivation(Integer.MAX_VALUE);

    private int id;
    private INeuron<?, ?> neuron;
    private Document doc;

    protected TreeMap<Link, Link> inputLinks = new TreeMap<>(INPUT_COMP);
    protected TreeMap<Link, Link> outputLinks = new TreeMap<>(OUTPUT_COMP);

    private double upperBound;
    private double lowerBound;

    public Option rootOption = new Option(null, this, null);
    public Option currentOption = rootOption;
    public Option finalOption;

    boolean ubQueued = false;
    private long markedHasCandidate;

    private Double targetValue;
    private Double inputValue;

    private Integer sequence;
    private Integer candidateId;

    /**
     * The cached decision is used to avoid having to explore the same currentSearchState twice even though nothing that
     * influences this currentSearchState has changed.
     */
    public Decision cachedDecision = UNKNOWN;
    public double alternativeCachedWeightSum;

    /**
     * The cached search node is used to avoid having to recompute the activation values and weights that are associated
     * with this search node.
     */
    public SearchNode cachedSearchNode;
    public SearchNode bestChildNode;

    public int[] debugCounts = new int[3];
    public int[] debugDecisionCounts = new int[3];


    public Activation(int id) {
        this.id = id;
    }


    public Activation(Document doc, INeuron neuron) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = neuron;

        neuron.register(this);
    }



    public abstract double getP(Option o);


    public int getId() {
        return id;
    }

    public Document getDocument() {
        return doc;
    }

    public int getThreadId() {
        return doc.getThreadId();
    }


    public String getLabel() {
        return getINeuron().getLabel();
    }


    public <N extends INeuron> N getINeuron() {
        return (N) neuron;
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
            l.getOutput().markHasCandidateRecursiveStep(v);
        }
    }


    protected abstract void markHasCandidateRecursiveStep(long v);


    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double ub) {
        upperBound = ub;
    }


    public double getLowerBound() {
        return lowerBound;
    }


    public void setLowerBound(double lb) {
        lowerBound = lb;
    }

    public Double getTargetValue() {
        return targetValue;
    }

    public Double getInputValue() {
        return inputValue;
    }


    public Decision getDecision() {
        return currentOption.decision;
    }


    public Decision getNextDecision(Option parent, SearchNode sn) {
        if(sn == null) {
            return UNKNOWN;
        } else if(this == sn.getActivation()) {
            return sn.getDecision();
        } else {
            return parent.decision;
        }
    }


    public Decision getFinalDecision() {
        return finalOption != null ? finalOption.decision : null;
    }


    public void addLink(Direction dir, Link l) {
        getLinks(dir.getInverted()).put(l, l); // TODO: Warum inverted?
    }


    public TreeMap<Link, Link> getLinks(Direction dir) {
        switch(dir) {
            case INPUT:
                return inputLinks;
            case OUTPUT:
                return outputLinks;
        }
        return null;
    }


    public Link getLinkBySynapseId(int synapseId) {
        for(Link l: inputLinks.values()) {
            if(l.getSynapse().getId() == synapseId) {
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


    public Stream<Link> getLinksBySynapse(Direction dir, Synapse syn) {
        return getLinks(dir).subMap(
                new Link(syn, MIN_ACTIVATION, MIN_ACTIVATION),
                new Link(syn, MAX_ACTIVATION, MAX_ACTIVATION))
                .values()
                .stream();
    }


    public double process(SearchNode sn) throws OscillatingActivationsException, RecursiveDepthExceededException {
        State oldState = currentOption.getState();
        State s = computeValueAndWeight(sn);

        if (currentOption.searchNode != sn) {
            if((currentOption.decision != UNKNOWN && currentOption.getState().equalsWithWeights(s))) {
                return 0.0;
            }

            if(this == sn.getActivation() && s.getPreferredDecision() != sn.getDecision()) {
                return 0.0;
            }

            saveState(sn);
        }

        if (currentOption.setState(s)) {
            doc.getValueQueue().propagateActivationValue(this, sn, !oldState.lowerBoundEquals(s), !oldState.upperBoundEquals(s));
        }

        return s.weight - oldState.weight;
    }


    public State computeValueAndWeight(SearchNode sn) throws RecursiveDepthExceededException {
        INeuron n = getINeuron();
        INeuron.SynapseSummary ss = n.getSynapseSummary();

        double net = n.getTotalBias(CURRENT);
        double netUB = net;

        Integer fired = null;

        for (InputState is: getInputStates(sn)) {
            Synapse s = is.l.getSynapse();
            Activation iAct = is.l.getInput();

            if (iAct == this) continue;

            double x = Math.min(s.getLimit(), is.s.value) * s.getWeight();
            net += x;
            netUB += Math.min(s.getLimit(), is.s.ub) * s.getWeight();

            net += s.computeRelationWeights(is.l);

            if (!s.isRecurrent() && !s.isNegative(CURRENT)) {
                fired = Utils.max(fired, iAct.currentOption.getState().fired);
            }
        }

        return new State(
                n.getActivationFunction().f(net),
                n.getActivationFunction().f(netUB),
                net,
                net > 0.0 ? (fired != null ? fired : 0) + getFiredIncrement() : null,
                Math.max(0.0, Math.min(-ss.getNegRecSum(), net))
        );
    }


    protected abstract int getFiredIncrement();


    public void processBounds() throws RecursiveDepthExceededException {
        double oldUpperBound = upperBound;

        computeBounds();

        if(Math.abs(upperBound - oldUpperBound) > 0.01) {
            for(Link l: outputLinks.values()) {
                doc.getUpperBoundQueue().add(l);
            }
        }

        if (oldUpperBound <= 0.0 && upperBound > 0.0) {
            getINeuron().propagate(this);
        }
    }


    public void computeBounds() throws RecursiveDepthExceededException {
        INeuron n = getINeuron();
        SynapseSummary ss = n.getSynapseSummary();

        double ub = n.getTotalBias(CURRENT) + ss.getPosRecSum();
        double lb = n.getTotalBias(CURRENT) + ss.getPosRecSum();

        for (Link l : inputLinks.values()) {
            Synapse s = l.getSynapse();
            if(s.isInactive()) {
                continue;
            }

            Activation iAct = l.getInput();

            if (iAct == this) continue;

            double x = s.getWeight();

            if (s.isNegative(CURRENT)) {
                if (!s.isRecurrent()) {
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

        upperBound = n.getActivationFunction().f(ub);
        lowerBound = n.getActivationFunction().f(lb);
    }


    private List<InputState> getInputStates(SearchNode sn) {
        ArrayList<InputState> tmp = new ArrayList<>();
        Synapse lastSynapse = null;
        InputState maxInputState = null;
        for (Link l : inputLinks.values()) {
            if(l.isInactive()) {
                continue;
            }
            if (lastSynapse != null && lastSynapse != l.getSynapse()) {
                tmp.add(maxInputState);
                maxInputState = null;
            }

            State s = l.getInput().getInputState(l.getSynapse(), this, sn);
            if (maxInputState == null || maxInputState.s.value < s.value) {
                maxInputState = new InputState(l, s);
            }
            lastSynapse = l.getSynapse();
        }
        if (maxInputState != null) {
            tmp.add(maxInputState);
        }

        return tmp;
    }


    public void setInputState(Builder input) {
        rootOption.decision = SELECTED;
        rootOption.setP(1.0);
        rootOption.setState(new State(input.value, input.value, 0.0, input.fired, 0.0));
        currentOption = rootOption;
        finalOption = rootOption;

        inputValue = input.value;
        upperBound = input.value;
        lowerBound = input.value;
        targetValue = input.targetValue;
    }


    public abstract Option getInputExcitatoryOption(Option o);

    public abstract boolean addToValueQueue(Deque<Activation> queue, SearchNode sn);


    private static class InputState {
        public InputState(Link l, State s) {
            this.l = l;
            this.s = s;
        }

        Link l;
        State s;
    }


    protected abstract State getInputState(Synapse s, Activation act, SearchNode sn);


    public abstract boolean needsPropagation(SearchNode sn, boolean lowerBoundChange, boolean upperBoundChange);


    public boolean checkSelfReferencing(Activation act) {
        Activation act1 = getInputExcitatoryActivation();
        if(act1 == null) {
            return false;
        } else if(act == act1) {
            return true;
        }

        Integer f1 = act1.currentOption.getState().fired;
        Integer f2 = act.currentOption.getState().fired;
        if(f1 == null) {
            return false;
        } else if (f2 != null && f1 > f2) {
            return act1.checkSelfReferencingRecursiveStep(act, 0);
        } else {
            return act.checkSelfReferencingRecursiveStep(act1, 0);
        }
    }


    public abstract boolean checkSelfReferencingRecursiveStep(Activation act, int depth);


    protected abstract Activation getInputExcitatoryActivation();


    protected Link getStrongestLink() {
        if(inputLinks.size() == 1) {
            return inputLinks.firstEntry().getValue();
        }
        return inputLinks
                .values()
                .stream()
                .filter(l -> l.getInput().currentOption.getState().value > 0.0)
                .max(Comparator.comparing(l -> l.getInput().currentOption.getState().value))
                .orElse(null);
    }


    public List<Link> getFinalInputActivationLinks() {
        ArrayList<Link> results = new ArrayList<>();
        for (Link l : inputLinks.values()) {
            if (l.getInput().isFinalActivation()) {
                results.add(l);
            }
        }
        return results;
    }


    public List<Link> getFinalOutputActivationLinks() {
        ArrayList<Link> results = new ArrayList<>();
        for (Link l : outputLinks.values()) {
            if (l.getOutput().isFinalActivation()) {
                results.add(l);
            }
        }
        return results;
    }


    public boolean isFinalActivation() {
        return getFinalState().value > 0.0;
    }


    public State getFinalState() {
        return finalOption != null ? finalOption.getState() : ZERO;
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
                .forEach(l -> sequence = Math.max(sequence, l.getInput().getSequence() + 1));
        return sequence;
    }


    public Integer getCandidateId() {
        return candidateId;
    }


    public void setCandidateId(Integer candidateId) {
        this.candidateId = candidateId;
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


    public void computeOptionProbabilities() {

    }


    public String toString() {
        return id + " " + getNeuron().getId() + ":" + getINeuron().typeToString() + " " + getLabel() + " " + identityToString() + " - " +
                " UB:" + Utils.round(upperBound) +
                (inputValue != null ? " IV:" + Utils.round(inputValue) : "") +
                (targetValue != null ? " TV:" + Utils.round(targetValue) : "") +
                " V:" + (currentOption != null ? Utils.round(currentOption.getState().value) : "-") +
                " FV:" + (finalOption != null ? Utils.round(finalOption.getState().value) : "-");
    }


    public String searchStateToString() {
        return id + " " +
                getNeuron().getId() + ":" +
                getLabel() + " " +
                " CD:" + cachedDecision +
                " LIMITED:" + debugCounts[SearchNode.DebugState.LIMITED.ordinal()] +
                " CACHED:" + debugCounts[SearchNode.DebugState.CACHED.ordinal()] +
                " EXPLORE:" + debugCounts[SearchNode.DebugState.EXPLORE.ordinal()] +
                " SELECTED:" + debugDecisionCounts[0] +
                " EXCLUDED:" + debugDecisionCounts[1];
    }


    public String toStringDetailed() {
        StringBuilder sb = new StringBuilder();
        sb.append(Utils.addPadding("" + id, 3) + " ");

        sb.append(Utils.addPadding(getINeuron().typeToString(), 10) + " - ");

        sb.append(getDecisionString() + " - ");

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
        State fs = getFinalState();
        if (fs != null) {
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


    protected abstract String getDecisionString();


    public State getExpectedState() {
        double value = 0.0;
        double ub = 0.0;
        double net = 0.0;

        for (Option option : getOptions()) {
            double p = option.getP();
            State s = option.getState();

            value += p * s.value;
            ub += p * s.ub;
            net += p * s.net;
        }
        return new State(value, ub, net, 0, 0.0);
    }


    public Collection<Option> getOptions() {
        if (rootOption == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList<Option> results = new ArrayList<>();
        rootOption.traverse(o -> results.add(o));
        return results;
    }


    @Override
    public int compareTo(Activation act) {
        return Integer.compare(id, act.id);
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

                sb.append(l.getInput().id);

                first = false;
            }
        }
        sb.append(")");
        return sb.toString();
    }


    public String linksToString() {
        StringBuilder sb = new StringBuilder();
        for(Link l: inputLinks.values()) {
            sb.append("  " + l.getInput().getLabel() + "  W:" + l.getSynapse().getWeight() + "\n");
        }

        return sb.toString();
    }


    public enum Mode {OLD, NEW}


    public void saveState(SearchNode sn) {
        currentOption = new Option(currentOption, this, sn);

        if (sn.getModifiedActivations() != null) {
            sn.getModifiedActivations().put(currentOption.act, currentOption);
        }
    }


    public static class Builder {
        public double value = 1.0;
        public Double targetValue;
        public int fired;
        public Map<Integer, Activation> inputLinks = new TreeMap<>();


        public Builder setValue(double value) {
            this.value = value;
            return this;
        }


        public Builder setTargetValue(Double targetValue) {
            this.targetValue = targetValue;
            return this;
        }


        public Map<Integer, Activation> getInputLinks() {
            return this.inputLinks;
        }


        public void addInputLink(Integer synId, Activation iAct) {
            inputLinks.put(synId, iAct);
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

