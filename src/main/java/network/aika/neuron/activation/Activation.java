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

import network.aika.Config;
import network.aika.Model;
import network.aika.Thought;
import network.aika.Utils;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Sign;
import network.aika.neuron.Synapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.neuron.phase.link.LinkPhase;
import network.aika.neuron.phase.link.PropagateGradient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.phase.activation.ActivationPhase.*;
import static network.aika.neuron.phase.link.LinkPhase.PROPAGATE_GRADIENT_RANK;

/**
 * @author Lukas Molzberger
 */
public class Activation extends QueueEntry<ActivationPhase> {

    public static double TOLERANCE = 0.001;

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

    private int round; // Only used as stopping criteria
    private Activation lastRound;

    private Set<Activation> branches = new TreeSet<>();
    private Activation mainBranch;

    private Reference reference;

//    private double selfGradient;
    private double gradient;


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
        queueState = new QueueState(
                this,
                getInitialPhases(getThought().getConfig())
        );

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

    public double getNet(boolean isFinal) {
        return sum + (isFinal ? lateSum : 0.0) + getNeuron().getBias(isFinal);
    }

    public double getNet() {
        return getNet(ActivationPhase.isFinal(getPhase()));
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

    @Override
    protected int innerCompareTo(QueueEntry<ActivationPhase> qe) {
        return Integer.compare(getId(), ((Activation) qe).getId());
    }

    public OutputKey getOutputKey() {
        return new OutputKey(getNeuronProvider(), getId());
    }

    public String getDescriptionLabel() {
        return getNeuron().getLabel();
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

    public void setNeuron(Neuron n) {
        this.neuron = n;
    }

    public Model getModel() {
        return neuron.getModel();
    }

    public Config getConfig() {
        return getThought().getConfig();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
    }

    public Activation createBranch(Synapse excludedSyn) {
        Activation clonedAct = new Activation(thought.createActivationId(), thought, neuron);
        clonedAct.queueState = queueState.copy(clonedAct);
        clonedAct.round = round + 1;
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);
        return clonedAct;
    }

    public Activation getModifiable(Synapse excludedSyn) {
        if (!isFinal) return this;

        Activation clonedAct = new Activation(id, thought, neuron);
        queueState.setEntryToQueue(clonedAct);
        clonedAct.queueState = queueState;
        clonedAct.round = round + 1;
        clonedAct.lastRound = this;
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
        if (queueState.isMarked()) return true;

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
        if (!fixed) {
            value = computeValue(ActivationPhase.isFinal(getPhase()));
        }
        isFinal = true;
        if (!equals(lastRound)) {
            getPhase().propagate(this,
                    new Visitor(this, OUTPUT)
            );
        }
    }

    public void updateOutgoingLinks() {
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

    public void propagate(Visitor v) {
        v.followLinks(this);

        getNeuron().getOutputSynapses()
                .filter(s -> !outputLinkExists(s))
                .forEach(s ->
                        s.transition(v, this, null, true)
                );
    }

    public Activation createActivation(Neuron n) {
        Activation act = new Activation(thought.createActivationId(), thought, n);
        act.queueState = new QueueState(act,
                getPhase().getNextActivationPhases(
                        getThought().getConfig()
                )
        );
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

        if (nl.getSynapse().getWeight() > 0.0 || nl.getSynapse().isTemplate()) {
            nl.addToQueue(
                    nl.getStartPhase().getNextLinkPhases(getConfig())
            );
        }

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
        if (fixed) {
            return;
        }

        double initialValue = computeValue(false);
        double finalValue = computeValue(true);

        if (Math.abs(finalValue - initialValue) > TOLERANCE) {
            getModifiable(null).addToQueue(FINAL_LINKING);
        }
    }

    private void checkIfFired() {
        if (fired == NOT_FIRED && getNet() > 0.0) {
            fired = neuron.incrementFired(getLatestFired());
            addToQueue(INITIAL_LINKING);
        }
    }

    private Fired getLatestFired() {
        return inputLinks.values().stream()
                .map(il -> il.getInput().getFired())
                .max(Fired::compareTo)
                .orElse(null);
    }

    private double computeValue(boolean isFinal) {
        return branchProbability *
                neuron.getActivationFunction().f(
                        getNet(isFinal)
                );
    }

    public void initSelfGradient() {
        gradient += getNorm() * getActFunctionDerivative() *
                getNeuron().getSurprisal(
                        Sign.getSign(this)
                );
    }

    public double getNorm() {
        return (1 / (1 + getNeuron().getSampleSpace().getN()));
    }

    public boolean gradientIsZero() {
        return Math.abs(gradient) < TOLERANCE;
    }

    public void processGradient() {
        if (getNeuron().isInputNeuron())
            return;

        if(gradientIsZero())
            return;

        addLinksToQueue(INPUT, new PropagateGradient(PROPAGATE_GRADIENT_RANK, gradient));

        gradient = 0.0;
    }

    public double getActFunctionDerivative() {
        return getNeuron()
                .getActivationFunction()
                .outerGrad(
                        getNet()
                );
    }

    public void propagateGradient(double g) {
        gradient += g;

        addToQueue(PROPAGATE_GRADIENT);
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
    }

    public void count() {
        getNeuron().count(this);

        addLinksToQueue(INPUT, LinkPhase.COUNTING);
    }

    public void addLinksToQueue(Direction dir, LinkPhase... phases) {
        (dir == INPUT ? getInputLinks() : getOutputLinks())
                .forEach(l -> l.addToQueue(phases));
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

    public String getShortString() {
        return "id:" +
                getId() +
                " n:[" + getNeuron() + "]" +
                Phase.toString(getPhase());
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

        if (includeLink) {
            sb.append("\n");
            getInputLinks().forEach(l ->
                    sb.append("   " + l.toDetailedString() + "\n")
            );
            sb.append("\n");
        }

        return sb.toString();
    }

    public double getGradient() {
        return gradient;
    }
}
