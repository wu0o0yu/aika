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
import network.aika.neuron.TSynapse;
import network.aika.neuron.activation.link.Direction;
import network.aika.neuron.activation.search.Decision;
import network.aika.neuron.activation.search.Input;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.activation.search.SearchNode;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.neuron.activation.link.Direction.INPUT;
import static network.aika.neuron.activation.link.Direction.OUTPUT;
import static network.aika.neuron.activation.search.Decision.*;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.State.ZERO;


/**
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

    public TreeMap<Link, Link> inputLinks = new TreeMap<>(INPUT_COMP);
    public TreeMap<Link, Link> outputLinks = new TreeMap<>(OUTPUT_COMP);

    private Bounds bounds;

    private Option rootOption;
    public Option currentOption;
    public Option finalOption;

    boolean ubQueued = false;

    private Double targetValue;
    private State inputState;

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



    public static Comparator<Link> INPUT_COMP = (l1, l2) -> {
        int r = Synapse.INPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.input.getId(), l2.input.getId());
    };

    public static Comparator<Link> OUTPUT_COMP = (l1, l2) -> {
        int r = Synapse.OUTPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.output.getId(), l2.output.getId());
    };


    public Activation(int id) {
        this.id = id;
    }


    public Activation(Document doc, INeuron neuron) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = neuron;

        neuron.register(this);
    }


    public Option getRootOption() {
        if(rootOption == null) {
            rootOption = new Option(null, this, null);
        }

        return rootOption;
    }

    public Option getCurrentOption() {
        if(currentOption == null) {
            currentOption = getRootOption();
        }

        return currentOption;
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


    public Bounds getBounds() {
        return bounds;
    }

    public Double getTargetValue() {
        return targetValue;
    }

    public State getInputState() {
        return inputState;
    }


    public Decision getDecision() {
        return getCurrentOption().decision;
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
        return finalOption.decision;
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
        Option oldOption = getCurrentOption();

        currentOption = new Option(oldOption, this, sn);

        if (currentOption.newSearchNode()) {
            if((oldOption.decision != UNKNOWN && oldOption.getState().equalsWithWeights(currentOption.getState()))) {
                return 0.0;
            }

            if(this == sn.getActivation() && currentOption.getState().getPreferredDecision() != sn.getDecision()) {
                return 0.0;
            }

            saveState(sn);
        }

        if (currentOption.hasChanged()) {
            doc.getValueQueue().propagateActivationValue(this, sn, !oldOption.getState().lowerBoundEquals(currentOption.getState()), !oldOption.getState().upperBoundEquals(currentOption.getState()));
        }

        return currentOption.getState().weight - oldOption.getState().weight;
    }




    public State computeValueAndWeight() {
        INeuron n = getINeuron();

        double bias = n.getTotalBias(CURRENT);

        return compute(bias, bias, inputs);
    }


    public void computeBounds() throws Activation.RecursiveDepthExceededException {
        INeuron n = getINeuron();
        INeuron.SynapseSummary ss = n.getSynapseSummary();

        double bias = n.getTotalBias(CURRENT);

        return compute(bias + ss.getNegRecSum(), bias + ss.getPosRecSum(), inputs);
    }


    private State compute(double net, Iterable<Input> inputs) {
        if(inputState != null) {
            return inputState;
        }

        INeuron n = getINeuron();
        INeuron.SynapseSummary ss = n.getSynapseSummary();

        Fired fired = null;

        for (Input in: inputs) {
            Synapse syn = in.getSynapse();
            Bounds bounds = in.getBounds();

            double w = syn.getWeight();

            netLB += state.lb * w;
            netUB += state.ub * w;

            if (!syn.isRecurrent() && !syn.isNegative(CURRENT)) {
                firedLatest = computeFiredLatest(firedLatest, state.firedLatest);
            }


            //----

            double x = s.getWeight();

            ub += !syn.isNegative(CURRENT) ? iAct.bounds.ub : iAct.bounds.lb * x;
            lb += !syn.isNegative(CURRENT) ? iAct.bounds.lb : iAct.bounds.ub * x;

            firedLatest = computeFiredLatest(firedLatest, iAct.bounds.firedLatest);
            firedEarliest = computeFiredEarliest(firedEarliest, iAct.bounds.firedEarliest, s);


        }

        return new State(
                n.getActivationFunction().f(netLB),
                n.getActivationFunction().f(netUB),
                netLB,
                netLB > 0.0 ? incrementFired(firedLatest) : null,
                netUB > 0.0 ? incrementFired(firedEarliest) : null,
                Math.max(0.0, Math.min(-ss.getNegRecSum(), netLB))
        );
    }



/*
    public State computeValueAndWeight(SearchNode sn) throws RecursiveDepthExceededException {
        if(inputState != null) {
            return inputState;
        }

        INeuron n = getINeuron();
        INeuron.SynapseSummary ss = n.getSynapseSummary();

        double net = n.getTotalBias(CURRENT);
        double netUB = net;

        Fired firedLatest = null;
        Fired firedEarliest = null;

        for (InputState is: getInputStates(sn)) {
            Synapse s = is.link.getSynapse();
            Activation iAct = is.link.getInput();

            if (iAct == this) continue;

            double x = is.state.lb * s.getWeight();
            net += x;
            netUB += is.state.ub * s.getWeight();

            net += s.computeRelationWeights(is.link);

            if (!s.isRecurrent() && !s.isNegative(CURRENT)) {
                firedLatest = Fired.max(firedLatest, is.state.firedLatest);
                firedEarliest = Fired.max(firedEarliest, is.state.firedEarliest);
            }
        }

        return new State(
                n.getActivationFunction().f(net),
                n.getActivationFunction().f(netUB),
                net,
                net > 0.0 ? incrementFired(firedLatest) : null,
                netUB > 0.0 ? incrementFired(firedEarliest) : null,
                Math.max(0.0, Math.min(-ss.getNegRecSum(), net))
        );
    }



    public void computeBounds() throws RecursiveDepthExceededException {
        INeuron n = getINeuron();
        SynapseSummary ss = n.getSynapseSummary();

        Fired firedLatest = Fired.MIN;
        Fired firedEarliest = null;

        double ub = n.getTotalBias(CURRENT) + ss.getPosRecSum();
        double lb = n.getTotalBias(CURRENT) + ss.getNegRecSum();

        for (Link l : inputLinks.values()) {
            Synapse s = l.getSynapse();
            if(s.isInactive()) {
                continue;
            }

            Activation iAct = l.getInput();

            if (iAct == this || s.isRecurrent()) continue;

            double x = s.getWeight();

            ub += !s.isNegative(CURRENT) ? iAct.bounds.ub : iAct.bounds.lb * x;
            lb += !s.isNegative(CURRENT) ? iAct.bounds.lb : iAct.bounds.ub * x;

            firedLatest = computeFiredLatest(firedLatest, iAct.bounds.firedLatest);
            firedEarliest = computeFiredEarliest(firedEarliest, iAct.bounds.firedEarliest, s);
        }

        bounds = new State(
                n.getActivationFunction().f(lb),
                n.getActivationFunction().f(ub),
                0.0,
                firedLatest,
                firedEarliest,
                0.0
        );
    }
*/


    public void processBounds() throws RecursiveDepthExceededException {
        Bounds oldBounds = bounds;

        computeBounds();

        if(oldBounds == null || Math.abs(bounds.ub.value - oldBounds.ub.value) > 0.01) {
            for(Link l: outputLinks.values()) {
                doc.getUpperBoundQueue().add(l);
            }
        }

        if ((oldBounds == null || oldBounds.ub.value <= 0.0) && bounds.ub.value > 0.0) {
            getINeuron().propagate(this);
        }
    }



    protected abstract Fired incrementFired(Fired f);


    private Fired computeFiredLatest(Fired firedLatest, Fired inputFiredLatest) {
        return firedLatest != null && inputFiredLatest != null ? Fired.max(firedLatest, inputFiredLatest) : null;
    }


    protected Fired computeFiredEarliest(Fired firedEarliest, Fired inputFiredEarliest, Synapse s) {
        if(!s.isWeak(CURRENT)) {
            return Fired.max(firedEarliest, inputFiredEarliest);
        } else {
            return firedEarliest;
        }
    }


    public void setInputState(Builder input) {
        Fired f = new Fired(input.inputTimestamp, input.fired);

        State is = new State(input.value, 0.0, f, 0.0);
        bounds = new Bounds(is, is);

        rootOption = new Option(null, this, null);
        rootOption.decision = SELECTED;
        rootOption.p = 1.0;
        rootOption.setBounds(bounds);
        currentOption = rootOption;
        finalOption = rootOption;

        inputState = is;

        targetValue = input.targetValue;
    }


    public abstract Option getInputExcitatoryOption(Option o);

    public abstract boolean addToValueQueue(Deque<Activation> queue, SearchNode sn);


    public abstract State getInputState(Synapse s, Activation act, SearchNode sn);


    public abstract boolean needsPropagation(SearchNode sn, boolean lowerBoundChange, boolean upperBoundChange);


    public boolean checkSelfReferencing(Activation act) {
        Activation act1 = getInputExcitatoryActivation();
        if(act1 == null) {
            return false;
        } else if(act == act1) {
            return true;
        }

        Fired f1 = act1.bounds.firedLatest;
        Fired f2 = act.bounds.firedLatest;
        if(f1 == null) {
            return false;
        } else if (f2 != null && f1.compareTo(f2) == 1) {
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
                .filter(l -> l.getInput().getCurrentOption().getBounds().lb.value > 0.0)
                .max(Comparator.comparing(l -> l.getInput().getCurrentOption().getBounds().lb.value))
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
        return finalOption != null ? finalOption.getBounds().lb : ZERO;
    }


    public double getValue() {
        return getFinalState().value;
    }


    public void computeOptionProbabilities() {

    }


    public String toString() {
        return id + " " + getNeuron().getId() + ":" + getINeuron().typeToString() + " " + getLabel() + " " + identityToString() + " - " +
                " UB:" + Utils.round(bounds.ub.value) +
                (inputState != null ? " IV:" + Utils.round(inputState.value) : "") +
                (targetValue != null ? " TV:" + Utils.round(targetValue) : "") +
                " V:" + (getCurrentOption() != null ? Utils.round(getCurrentOption().getBounds().lb.value) : "-") +
                " FV:" + (finalOption != null ? Utils.round(finalOption.getBounds().lb.value) : "-");
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
            sb.append(Utils.round(bounds.ub.value));
        }

        sb.append(" Exp:");
        sb.append(getExpectedState());

        sb.append(" - ");
        State fs = getFinalState();
        if (fs != null) {
            sb.append(fs);
        }


        if (inputState != null) {
            sb.append(" - IV:" + Utils.round(inputState.value));
        }

        if (targetValue != null) {
            sb.append(" - TV:" + Utils.round(targetValue));
        }

        return sb.toString();
    }


    protected abstract String getDecisionString();


    private State getExpectedState(Function<Bounds, State> callback) {
        double value = 0.0;
        double net = 0.0;

        for (Option option : getOptions()) {
            double p = option.getP();
            State s = callback.apply(option.getBounds());

            value += p * s.value;
            net += p * s.net;
        }
        return new State(value, net, null, 0.0);
    }


    public Bounds getExpectedBounds() {
        return new Bounds(getExpectedState(b -> b.lb), getExpectedState(b -> b.ub));
    }


    public Collection<Option> getOptions() {
        if (getRootOption() == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList<Option> results = new ArrayList<>();
        getRootOption().traverse(o -> results.add(o));
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
        if (sn.getModifiedActivations() != null) {
            sn.getModifiedActivations().put(getCurrentOption().act, getCurrentOption());
        }
    }


    public static class Builder {
        public double value = 1.0;
        public Double targetValue;
        public int inputTimestamp;
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


        public Builder setInputTimestamp(int inputTimestamp) {
            this.inputTimestamp = inputTimestamp;
            return this;
        }

        public Builder setFired(int fired) {
            this.fired = fired;
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



    public static class Link {
        private final Synapse synapse;
        private TSynapse targetSynapse;

        private final Activation input;
        private final Activation output;


        public Link(Synapse s, Activation input, Activation output) {
            this.synapse = s;
            this.targetSynapse = null;
            this.input = input;
            this.output = output;
        }


        public Link(Synapse s, TSynapse targetSynapse, Activation input, Activation output) {
            this.synapse = s;
            this.targetSynapse = targetSynapse;
            this.input = input;
            this.output = output;
        }


        public Synapse getSynapse() {
            return synapse;
        }


        public TSynapse getTargetSynapse() {
            return targetSynapse;
        }

        public void setTargetSynapse(TSynapse targetSynapse) {
            this.targetSynapse = targetSynapse;
        }


        public Activation getInput() {
            return input;
        }


        public Activation getOutput() {
            return output;
        }


        public boolean isNegative(Synapse.State s) {
            return synapse.isNegative(s);
        }


        public boolean isInactive() {
            return synapse.isInactive();
        }


        public boolean isRecurrent() {
            return synapse.isRecurrent();
        }


        public void link() {
            input.addLink(INPUT, this);
            output.addLink(OUTPUT, this);
        }


        public String toString() {
            return synapse + ": " + input + " --> " + output;
        }
    }

}

