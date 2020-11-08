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

import network.aika.*;
import network.aika.neuron.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.phase.Phase;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.phase.Phase.*;

/**
 *
 * @author Lukas Molzberger
 */
public class Activation implements Comparable<Activation> {

    public static double TOLERANCE = 0.001;

    private Phase phase = INITIAL_LINKING;
    private boolean[] isQueued;

    private double value;
    private double sum;
    private double lateSum;
    private Fired fired = NOT_FIRED;
    private boolean fixed;

    private int id;
    private Neuron<?> neuron;
    private Thought thought;

    private double branchProbability = 1.0;

    Map<NeuronProvider, Link> inputLinks;
    NavigableMap<OutputKey, Link> outputLinks;

    private boolean isFinal;
    public boolean marked;

    private int round; // Only used as stopping criteria
    private Activation lastRound;

    private Set<Activation> branches = new TreeSet<>();
    private Activation mainBranch;

    private Reference reference;

    private double selfGradient;
    private double unpropagatedGradient;


    private Activation(int id, Neuron<?> n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(Thought t, Neuron<?> n) {
        this(t.createActivationId(), t, n);
    }

    private Activation(int id, Thought t, Neuron<?> n) {
        this(id, n);
        this.thought = t;

        thought.registerActivation(this);

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>();
    }

    public void initInput(Reference ref) {
        isQueued = new boolean[1];
        setReference(ref);

        setValue(1.0);
        setFired(ref.getBegin());

        isFinal = true;
        addToQueue(INITIAL_LINKING);
    }

    public int getId() {
        return id;
    }

    public double getValue() {
        return value;
    }

    public double getNet(Phase p) {
        return sum + (p.isFinal() ? lateSum : 0.0) + getNeuron().getBias(p);
    }

    public double getNet() {
        return getNet(phase);
    }

    public Fired getFired() {
        return fired;
    }

    public void setFired(int inputTimestamp) {
        this.fired = new Fired(inputTimestamp, 0);
    }

    public boolean isFinal() {
        return isFinal;
    }

    public Thought getThought() {
        return thought;
    }

    public OutputKey getOutputKey() {
        return new OutputKey(getNeuronProvider(), getId());
    }

    public String getDescriptionLabel() {
        return getNeuron().getDescriptionLabel();
    }

    public void updateQueue(Activation oldAct) {
        if(isQueued[0]) {
            thought.removeActivationFromQueue(oldAct);
            thought.addActivationToQueue(this);
        }
    }

    public void addToQueue(Phase p) {
        if(p == null) {
            return;
        }

        if(isQueued[0] && phase.getRank() <= p.getRank()) {
            thought.removeActivationFromQueue(this);
        }

        phase = p;

        setQueued(true);
        thought.addActivationToQueue(this);
    }

    public void process() {
        setQueued(false);
        phase.process(this);

        if(isActive() && !isQueued()) {
            addToQueue(phase.nextPhase(getConfig()));
        }
    }

    public boolean isQueued() {
        return isQueued[0];
    }

    private void setQueued(boolean isQueued) {
        this.isQueued[0] = isQueued;
    }

    public Phase getPhase() {
        return phase;
    }


    public <R extends Reference> R getReference() {
        return (R) reference;
    }

    public void setReference(Reference ref) {
        this.reference = ref;
    }

    public void propagateReference(Reference ref) {
        setReference(ref);
        getModel().linkInputRelations(this, INPUT);
    }

    public Activation getLastRound() {
        return lastRound;
    }

    public Neuron<?> getNeuron() {
        return neuron;
    }

    public Model getModel() {
        return neuron.getModel();
    }

    public Config getConfig() {
        return getThought().getTrainingConfig();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
    }

    public Activation createBranch(Synapse excludedSyn) {
        Activation clonedAct = new Activation(thought.createActivationId(), thought, neuron);
        clonedAct.isQueued = isQueued;
        clonedAct.round = round + 1;
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);
        return clonedAct;
    }

    public Activation getModifiable(Synapse excludedSyn) {
        if (!isFinal) return this;

        Activation clonedAct = new Activation(id, thought, neuron);
        clonedAct.isQueued = isQueued;
        clonedAct.round = round + 1;
        clonedAct.lastRound = this;
        clonedAct.updateQueue(this);
        linkClone(clonedAct, excludedSyn);

        return clonedAct;
    }

    /*
        public Activation cloneToReplaceLink(Synapse excludedSyn) {
            Activation clonedAct = new Activation(id, thought, neuron);
            linkClone(clonedAct, excludedSyn);
            return clonedAct;
        }
    */
    private void linkClone(Activation clonedAct, Synapse excludedSyn) {
        inputLinks
                .values()
                .stream()
                .filter(l -> l.getSynapse() != excludedSyn)
                .forEach(l -> {
                    Link nl = new Link(l.getSynapse(), l.getInput(), clonedAct, l.isSelfRef());
                            nl.linkInput();
                            nl.linkOutput();
                            clonedAct.sumUpLink(null, nl);
                        }
                );
    }

    public void setValue(double v) {
        value = v;
        fixed = true;
    }

    public boolean isActive() {
        return value > 0.0;
    }

    public double getBranchProbability() {
        return branchProbability;
    }

    public boolean isConflicting() {
        return getConflictingMainBranches()
                .anyMatch(act -> act.searchWithinBranch());
    }

    public boolean searchWithinBranch() {
        if (marked) return true;

        return getLinks(OUTPUT)
                .filter(l -> !l.isNegative() || l.isCausal())
                .map(l -> l.getOutput())
                .filter(act -> act.fired != NOT_FIRED && fired.compareTo(act.fired) == -1)
                .anyMatch(act -> act.searchWithinBranch());
    }

    public Stream<Activation> getConflictingMainBranches() {
        if (mainBranch != null) {
            return Stream.of(mainBranch);
        }

        return branches.stream()
                .flatMap(act -> act.getInputLinks())
                .filter(l -> l.isNegative())
                .map(l -> l.getInput())
                .filter(act -> act.getNeuron() instanceof InhibitoryNeuron)
                .flatMap(act -> act.getInputLinks())
                .map(l -> l.getInput());
    }

    public void updateValueAndPropagate() {
        if(!fixed) {
            value = computeValue(phase);
        }
        isFinal = true;
        if (!equals(lastRound)) {
            linkForward();
        }
    }

    public void train() {
        if(getNeuron().isInputNeuron()) {
            return;
        }

        initSelfGradient();
        computeInitialLinkGradients();
        updateSelfGradient();
    }

    public void linkForward() {
        if (lastRound == null || getPhase() == TRAINING) {
            propagate();
        } else {
            updateOutgoingLinks();
        }

        thought.processLinks();
    }

    private void updateOutgoingLinks() {
        lastRound.outputLinks
                .values()
                .forEach(l ->
                        Link.link(
                                l.getSynapse(),
                                this,
                                l.getOutput(),
                                l.isSelfRef()
                        )
                );
        lastRound.unlink();
        lastRound = null;
    }

    public void propagate() {
        new Visitor(this, OUTPUT)
                .followLinks(this);

        phase.propagate(this);
    }

    public void propagateIntern() {
        getThought().processLinks();

        getNeuron().getOutputSynapses()
                .filter(s -> !outputLinkExists(s))
                .forEach(s ->
                        Link.link(
                                s,
                                this,
                                createActivation(s.getOutput()),
                                false
                        )
                );
    }

    public Activation createActivation(Neuron n) {
        Activation act = new Activation(thought.createActivationId(), thought, n);
        act.isQueued = new boolean[1];
        return act;
    }

    public Link getInputLink(Synapse s) {
        return inputLinks.get(s.getPInput());
    }

    public boolean inputLinkExists(Synapse s) {
        return inputLinks.containsKey(s.getPInput());
    }

    public boolean outputLinkExists(Activation oAct) {
        return outputLinks.containsKey(oAct.getOutputKey());
    }

    public boolean outputLinkExists(Synapse s) {
        return !getOutputLinks(s).isEmpty();
    }

    public SortedMap<OutputKey, Link> getOutputLinks(Synapse s) {
        return outputLinks
                .subMap(
                        new OutputKey(s.getOutput().getProvider(), Integer.MIN_VALUE),
                        true,
                        new OutputKey(s.getOutput().getProvider(), Integer.MAX_VALUE),
                        true
                );
    }

    public Link addLink(Synapse s, Activation input, boolean isSelfRef) {
        Link ol = getInputLink(s);
        Link nl = new Link(s, input, this, isSelfRef);
        return addLink(ol, nl);
    }

    public Link addLink(Link ol, Link nl) {
        nl.linkInput();
        nl.linkOutput();

        sumUpLink(ol, nl);
        checkIfFired();

        nl.addToQueue();

        return nl;
    }

    public void sumUpLink(Link ol, Link nl) {
        assert ol == null || !isFinal;

        double w = nl.getSynapse().getWeight();

        if (w <= 0.0 && nl.isSelfRef()) return;

        double x = nl.getInput().value - (ol != null ? ol.getInput().value : 0.0);
        double s = x * w;

        if (isFinal) {
            lateSum += s;
        } else {
            sum += s;
        }

        if (!isFinal) {
            nl.getOutput().getNeuron().updateReference(nl);
        }
    }

    public void updateForFinalPhase() {
        if(fixed) {
            return;
        }

        double initialValue = computeValue(INITIAL_LINKING);
        double finalValue = computeValue(FINAL_LINKING);

        if (Math.abs(finalValue - initialValue) > TOLERANCE) {
            getModifiable(null).addToQueue(FINAL_LINKING);
        }
    }

    private void checkIfFired() {
        if (fired == NOT_FIRED && getNet() > 0.0) {
            fired = neuron.incrementFired(getLatestFired());
            addToQueue(getPhase());
        }
    }

    private Fired getLatestFired() {
        return inputLinks.values().stream()
                .map(il -> il.getInput().getFired())
                .max(Fired::compareTo)
                .orElse(null);
    }

    private double computeValue(Phase phase) {
        return branchProbability *
                neuron.getActivationFunction().f(
                        getNet(phase)
                );
    }

    public void computeInitialLinkGradients() {
        getInputLinks()
                .forEach(l -> l.computeOutputGradient());

        getInputLinks()
                .forEach(l -> l.removeGradientDependencies());

        getInputLinks()
                .forEach(l -> addInputGradient(l.getOutputGradient()));
    }

    public void initSelfGradient() {
        selfGradient = getNorm() * getActFunctionDerivative() *
                getNeuron().getSurprisal(
                        Sign.getSign(this)
                );
    }

    public void addInputGradient(double initialLinkGradient) {
        selfGradient += initialLinkGradient;
    }

    public void updateSelfGradient() {
        getInputLinks().forEach(l ->
                l.updateAndPropagateSelfGradient()
        );
    }

    public double getSelfGradient() {
        return selfGradient;
    }

    public double getNorm() {
        return (1 / (1 + getNeuron().getInstances().getN()));
    }

    public void processGradient() {
        if (getNeuron().isInputNeuron())
            return;

        inputLinks.values()
                .forEach(l ->
                        l.propagateGradient(unpropagatedGradient)
                );

        unpropagatedGradient = 0.0;
    }

    public double getActFunctionDerivative() {
        return getNeuron()
                .getActivationFunction()
                .outerGrad(
                        getNet()
                );
    }

    public void propagateGradient(double g) {
        unpropagatedGradient += g;

        addToQueue(GRADIENTS);
    }

    public void unlink() {
        inputLinks
                .values()
                .forEach(l -> l.unlink());
    }

    public void computeBranchProbability() {
        if (!isActive() || !hasBranches()) return;

        double net = getNet();
        Set<Activation> conflictingActs = branches
                .stream()
                .flatMap(bAct -> bAct.getInputLinks())
                .filter(l -> l.isNegative())
                .flatMap(l -> l.getInput().getInputLinks())  // Walk through to the inhib. Activation.
                .map(l -> l.getInput())
                .collect(Collectors.toSet());

        double offset = conflictingActs
                .stream()
                .mapToDouble(cAct -> cAct.getNet())
                .min()
                .getAsDouble();

        double norm = Math.exp(net - offset);
        norm += conflictingActs
                .stream()
                .mapToDouble(cAct -> Math.exp(cAct.getNet() - offset))
                .sum();

        double p = Math.exp(net - offset) / norm;

        if (Math.abs(p - getBranchProbability()) <= TOLERANCE) return;

        Activation cAct = getModifiable(null);
        cAct.branchProbability = p;

        thought.addActivationToQueue(cAct); // Linking
    }

    public void count() {
        getNeuron().count(this);

        getInputLinks()
                .forEach(l -> l.count());

        getOutputLinks()
                .forEach(l -> l.count());
    }

    public boolean equals(Activation act) {
        return act != null && Math.abs(value - act.value) <= TOLERANCE;
    }

    private Activation getMostRecentFinalActivation() {
        return !isFinal && lastRound != null ? lastRound : this;
    }

    public Stream<Link> getInputLinks() {
        return inputLinks.values().stream();
    }

    public Stream<Link> getOutputLinks() {
        Activation act = getMostRecentFinalActivation();
        return act.outputLinks.values().stream();
    }

    public boolean isConnected(Activation input) {
        return inputLinks.values().stream()
                .anyMatch(l -> l.getInput() == input);
    }

    public boolean hasBranches() {
        return !branches.isEmpty();
    }

    public Stream<Link> getLinks(Direction dir) {
        return (dir == INPUT ? inputLinks : outputLinks)
                .values()
                .stream();
    }

    public void updateSynapseWeights() {
        getInputLinks()
                .forEach(l -> l.updateSynapse());

        getNeuron().updatePropagateFlag();
    }

    @Override
    public int compareTo(Activation act) {
        int r = Integer.compare(getPhase().getRank(), act.getPhase().getRank());
        if(r != 0) return r;
        r = getPhase().compare(this, act);
        if(r != 0) return r;
        return Integer.compare(getId(), act.getId());
    }

    public String getShortString() {
        return "id:" +
                getId() +
                " n:[" + getNeuron() + "]" +
                " (" + (phase != null ? phase.getClass().getSimpleName() : "X") +
                (isQueued() ? "+" : "-") + ")";
    }

    public String gradientsToString() {
        StringBuilder sb = new StringBuilder();

        inputLinks.values()
                .forEach(l ->
                        sb.append(
                                l.gradientsToString() + " \n"
                        )
                );

        sb.append("\n");
        return sb.toString();
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean includeLink) {
        StringBuilder sb = new StringBuilder();
        sb.append("act " +
                getShortString() +
                " value:" + Utils.round(value) +
                " net:" + Utils.round(getNet()) +
                " bp:" + Utils.round(branchProbability) +
                " round:" + round);

        if(includeLink) {
            sb.append("\n");
            getInputLinks().forEach(l ->
                    sb.append("   " + l.toDetailedString() + "\n")
            );
            sb.append("\n");
        }

        return sb.toString();
    }
}
