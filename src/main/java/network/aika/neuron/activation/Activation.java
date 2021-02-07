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
import network.aika.neuron.phase.link.LinkPhase;
import network.aika.neuron.phase.link.PropagateGradient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.phase.activation.ActivationPhase.*;

/**
 * @author Lukas Molzberger
 */
public class Activation extends Element {

    public static double TOLERANCE = 0.001;

    private Double value = null;
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

    private int round; // Only used as stopping criteria
    private Activation lastRound;

    private Set<Activation> branches = new TreeSet<>();
    private Activation mainBranch;

    private Reference reference;

    private double lastEntropyGradient = 0.0;
    private double gradient;
    private double gradientSum;


    private Activation(int id, Neuron<?> n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(Thought t, Neuron<?> n) {
        this(t.createActivationId(), t, n);
    }

    public Activation(int id, Thought t, Neuron<?> n) {
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
    public void onProcessEvent(Phase p) {
        thought.onActivationProcessedEvent(p, this);
    }

    @Override
    public void afterProcessEvent(Phase p) {
        thought.afterActivationProcessedEvent(p, this);
    }

    public void initInput(Reference ref) {
        setReference(ref);

        setValue(1.0);
        setFired(ref.getBegin());

        getThought().addToQueue(
                this,
                LINK_AND_PROPAGATE,
                ENTROPY_GRADIENT
        );
    }

    public int getId() {
        return id;
    }

    public Double getValue() {
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

    public Fired getFired() {
        return fired;
    }

    public void setFired(int inputTimestamp) {
        setFired(new Fired(inputTimestamp, 0));
    }

    public void setFired(Fired f) {
        // TODO: check if really necessary
/*        if(isQueued()) {
            updateQueueEntry(() -> {
                this.fired = f;
                return this;
            });
        } else {
 */
            fired = f;
//        }
    }

    public Thought getThought() {
        return thought;
    }

    @Override
    public int compareTo(Element ge) {
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

        copyPhases(clonedAct);
        clonedAct.round = round + 1;
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);
        return clonedAct;
    }

    public Activation getModifiable(Synapse excludedSyn) {
        if (value == null)
            return this;

        Activation clonedAct = new Activation(id, thought, neuron);
        thought.onActivationCreationEvent(clonedAct, this);

        replaceElement(clonedAct);

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
                            nl.sumUpLink(nl.getInputValue());
                        }
                );
    }

    public void setValue(double v) {
        value = v;
        fixed = true;
    }

    public boolean isActive() {
        return value != null && value > 0.0;
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
                .filter(act ->
                        act.fired != NOT_FIRED && fired.compareTo(act.fired) == -1
                )
                .anyMatch(act ->
                        act.searchWithinBranch()
                );
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

    public boolean updateValue(boolean isFinal) {
        if (!fixed) {
            value = computeValue(isFinal);
        }
        return !equals(lastRound);
    }

    public void updateOutgoingLinks() {
        lastRound.addLinksToQueue(
                OUTPUT,
                LinkPhase.PROPAGATE_CHANGE
        );
//        lastRound.unlinkInputs();
        lastRound = null;
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
        return new Link(
                getInputLink(s),
                s,
                input,
                this,
                isSelfRef
        );
    }

    public void addToSum(double x) {
        if (value != null) {
            lateSum += x;
        } else {
            sum += x;
        }
    }

    public boolean updateForFinalPhase() {
        if (fixed)
            return false;

        double initialValue = computeValue(false);
        double finalValue = computeValue(true);

        if (Math.abs(finalValue - initialValue) > TOLERANCE) {
            return updateValue(true);
        }
        return false;
    }

    public boolean checkIfFired() {
        if (fired == NOT_FIRED && getNet(false) > 0.0) {
            setFired(neuron.incrementFired(getLatestFired()));
            return true;
        }
        return false;
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

    public void initEntropyGradient() {
        double selfGradient = getNeuron().getSurprisal(
                        Sign.getSign(this)
                );

        gradient += selfGradient - lastEntropyGradient;
        lastEntropyGradient = selfGradient;
    }

    public void propagateGradients() {
        if(gradientIsZero())
            return;

        double g = gradient;
        gradient = 0.0;

        g *= getNorm();
        g *= getNeuron()
                .getActivationFunction()
                .outerGrad(
                        getNet(true)
                );

        gradientSum += g;

        addLinksToQueue(
                INPUT,
                ! getNeuron().isInputNeuron() ? new PropagateGradient(g) : null,
                LinkPhase.TEMPLATE
        );

        getThought().addToQueue(
                this,
                TEMPLATE_INPUT,
                TEMPLATE_OUTPUT
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

    public void propagateGradient(double g) {
        gradient += g;

        getThought().addToQueue(
                this,
                PROPAGATE_GRADIENTS
        );
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
        double net = getNet(true);
        Set<Activation> conflictingActs = branches
                .stream()
                .flatMap(bAct -> bAct.getInputLinks())
                .filter(l -> l.isNegative())
                .flatMap(l -> l.getInput().getInputLinks())  // Walk through to the inhib. Activation.
                .map(l -> l.getInput())
                .collect(Collectors.toSet());

        double offset = conflictingActs
                .stream()
                .mapToDouble(cAct -> cAct.getNet(true))
                .min()
                .getAsDouble();

        double norm = Math.exp(net - offset);
        norm += conflictingActs
                .stream()
                .mapToDouble(cAct -> Math.exp(cAct.getNet(true) - offset))
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
        return value == null && lastRound != null ? lastRound : this;
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

    public String toShortString() {
        return "Act id:" +
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
                toShortString() +
                " value:" + (value != null ? Utils.round(value) : "X") +
                " net:" + Utils.round(getNet(false)) +
                " netFinal:" + Utils.round(getNet(true)) +
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
