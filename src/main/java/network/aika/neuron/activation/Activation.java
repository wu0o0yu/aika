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
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.VisitorPhase;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.neuron.phase.link.LinkPhase;
import network.aika.neuron.phase.link.PropagateGradient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.phase.activation.ActivationPhase.*;
import static network.aika.neuron.phase.link.LinkPhase.PROPAGATE_GRADIENT_RANK;

/**
 * @author Lukas Molzberger
 */
public class Activation implements ActivationGraphElement {

    public static double TOLERANCE = 0.001;

    private double value;
    private double sum;
    private double lateSum;
    private Fired fired = NOT_FIRED;
    private boolean fixed;
    private boolean marked;

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

    private double gradient;
    private double gradientSum;


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

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    @Override
    public void onProcessEvent() {
        thought.onActivationProcessedEvent(this);
    }


    @Override
    public void afterProcessEvent() {
        thought.afterActivationProcessedEvent(this);
    }

    public void initInput(Reference ref) {
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

    public double getGradient() {
        return gradient;
    }

    public double getGradientSum() {
        return gradientSum;
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
        setFired(new Fired(inputTimestamp, 0));
    }

    public void setFired(Fired f) {
        if(isQueued()) {
            updateQueueEntry(() -> {
                this.fired = f;
                return this;
            });
        } else {
            this.fired = f;
        }
    }

    public boolean isFinal() {
        return isFinal;
    }

    public Thought getThought() {
        return thought;
    }

    @Override
    public int compareTo(ActivationGraphElement ge) {
        return Integer.compare(getId(), ((Activation) ge).getId());
    }

    public OutputKey getOutputKey() {
        return new OutputKey(getNeuronProvider(), getId());
    }

    public String getLabel() {
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
        return getNeuron().getConfig();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
    }

    public Activation createBranch(Synapse excludedSyn) {
        Activation clonedAct = new Activation(thought.createActivationId(), thought, neuron);
        thought.onActivationCreationEvent(clonedAct, this);

        clonedAct.copyPhases(this);
        clonedAct.round = round + 1;
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);
        return clonedAct;
    }

    public Activation getModifiable(Synapse excludedSyn) {
        if (!isFinal) return this;

        Activation clonedAct = new Activation(id, thought, neuron);
        thought.onActivationCreationEvent(clonedAct, this);

        clonedAct.copyPhases(this);
        clearPendingPhases();

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
        if (isMarked())
            return true;

        return getOutputLinks()
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

    public boolean updateValue(boolean finalLinkingMode) {
        if (!fixed) {
            value = computeValue(finalLinkingMode);
        }
        isFinal = true;
        return !equals(lastRound);
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
//        lastRound.unlinkInputs();
        lastRound = null;
    }


    public static Activation createActivation(Thought t, Neuron n) {
        Activation act = new Activation(
                t.createActivationId(),
                t,
                n
        );

        return act;
    }

    public void addNextActivationPhases(VisitorPhase p) {
        getThought().addToQueue(
                this,
                p.getNextActivationPhases(getConfig())
        );
    }

    public void followLinks(Visitor v) {
        v.onEvent(false);

        v.tryToLink(this);

        Direction dir = v.downUpDir;

        setMarked(true);
        dir.getLinks(this)
                .filter(l -> l.follow(dir))
                .collect(Collectors.toList()).stream()
                .forEach(l ->
                        l.follow(v)
                );
        setMarked(false);

        v.onEvent(true);
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

        getThought().onLinkCreationEvent(nl);

        nl.linkInput();
        nl.linkOutput();

        sumUpLink(ol, nl);
        checkIfFired();

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
            nl.getSynapse().updateReference(nl);
        }
    }

    public void updateForFinalPhase() {
        if (fixed)
            return;

        double initialValue = computeValue(false);
        double finalValue = computeValue(true);

        if (Math.abs(finalValue - initialValue) > TOLERANCE) {
            boolean hasChanged = updateValue();

            if (hasChanged) {
                getThought().addToQueue(
                        getModifiable(null),
                        FINAL_LINKING
                );
            }
        }
    }

    private void checkIfFired() {
        if (fired == NOT_FIRED && getNet() > 0.0) {
            setFired(neuron.incrementFired(getLatestFired()));
            getThought().addToQueue(this, INITIAL_LINKING);
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

    public boolean gradientSumIsZero() {
        return Math.abs(gradientSum) < TOLERANCE;
    }

    public void processGradient() {
        gradientSum += gradient;

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

        getThought().addToQueue(this, PROPAGATE_GRADIENT);
    }

    public void linkInputs() {
        inputLinks
                .values()
                .forEach(l -> l.linkInput());
    }

    public void unlinkInputs() {
        inputLinks
                .values()
                .forEach(l -> l.unlinkInput());
    }

    public void linkOutputs() {
        outputLinks
                .values()
                .forEach(l -> l.linkOutput());
    }

    public void unlinkOutputs() {
        outputLinks
                .values()
                .forEach(l -> l.unlinkOutput());
    }

    public void link() {
        linkInputs();
        linkOutputs();
    }

    public void unlink() {
        unlinkInputs();
        unlinkOutputs();
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

    public void addLinksToQueue(Direction dir, LinkPhase... phases) {
        dir.getLinks(this)
                .forEach(l ->
                        getThought().addToQueue(l, phases)
                );
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

    public boolean hasBranches() {
        return !branches.isEmpty();
    }

    public String getShortString() {
        return "id:" +
                getId() +
                " n:[" + getNeuron() + "]";
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
}
