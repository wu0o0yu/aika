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
import network.aika.neuron.ActivationFunction;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.steps.activation.PropagateValueChange;
import network.aika.neuron.steps.activation.UpdateBias;
import network.aika.neuron.steps.link.LinkStep;
import network.aika.neuron.steps.link.PropagateGradient;
import network.aika.neuron.steps.link.SumUpLink;
import network.aika.neuron.sign.Sign;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.steps.activation.ActivationStep.*;
import static network.aika.neuron.sign.Sign.POS;

/**
 * @author Lukas Molzberger
 */
public class Activation extends Element<Activation> {

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(act -> act.id);

    private Double value = null;
    private Double inputValue = null;
    private double net;
    private double lastNet = 0.0;
    private Fired fired = NOT_FIRED;
    private boolean marked;

    private int id;
    private Neuron<?> neuron;
    private Thought thought;

    private double branchProbability = 1.0;

    Map<NeuronProvider, Link> inputLinks;
    NavigableMap<OutputKey, Link> outputLinks;

    private Set<Activation> branches = new TreeSet<>();
    private Activation mainBranch;

    private Reference reference;

    private double lastEntropyGradient = 0.0;
    private double inputGradient;

    /**
     * Accumulates all gradients in case a new link is added that needs be get informed about the gradient.
     */
    private double outputGradientSum;
    private double inputGradientSum;


    private Activation(int id, Neuron<?> n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(int id, Thought t, Neuron<?> n) {
        this(id, n);
        this.thought = t;

        net = n.getBias();

        thought.registerActivation(this);

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public void initInput(Reference ref) {
        setReference(ref);

        setInputValue(1.0);
        setFired(ref.getBegin());

        updateValue();

        QueueEntry.add(this, ENTROPY_GRADIENT);
        propagate();
    }

    public int getId() {
        return id;
    }

    public Double getValue() {
        return value;
    }

    public double getNet() {
        return net;
    }

    public double getInputGradient() {
        return inputGradient;
    }

    public double getOutputGradientSum() {
        return outputGradientSum;
    }

    public Fired getFired() {
        return fired;
    }

    public void setFired(int inputTimestamp) {
        setFired(new Fired(inputTimestamp, 0));
    }

    public void setFired(Fired f) {
        fired = f;
    }

    public Thought getThought() {
        return thought;
    }

    @Override
    public int compareTo(Activation act) {
        return ID_COMPARATOR.compare(this, act);
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

    public Neuron<?> getNeuron() {
        return neuron;
    }

    public void setNeuron(Neuron n) {
        this.neuron = n;
    }

    public ActivationFunction getActivationFunction() {
        return neuron.getActivationFunction();
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
        Activation clonedAct = thought.createActivation(neuron);
        thought.onActivationCreationEvent(clonedAct, this);

        copyPhases(clonedAct);
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);
        return clonedAct;
    }

    public Activation clone(Synapse excludedSyn) {
        if (value == null)
            return this;

        Activation clonedAct = new Activation(id, thought, neuron);
        thought.onActivationCreationEvent(clonedAct, this);

        replaceElement(clonedAct);

        linkClone(clonedAct, excludedSyn);

        return clonedAct;
    }

    private void linkClone(Activation clonedAct, Synapse excludedSyn) {
        inputLinks
                .values()
                .stream()
                .filter(l -> l.getSynapse() != excludedSyn)
                .forEach(l -> {
                    Link nl = new Link(l.getSynapse(), l.getInput(), clonedAct, l.isSelfRef());
                            nl.linkInput();
                            nl.linkOutput();
                            nl.sumUpLink(nl.getInputValue(POS));
                        }
                );
    }

    public void setInputValue(double v) {
        inputValue = v;
    }

    public boolean isActive(boolean defaultValue) {
        if(value == null)
            return defaultValue;

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
                .filter(act ->
                        act.fired != NOT_FIRED && Fired.COMPARATOR.compare(fired, act.fired) == -1
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

    public void updateOutgoingLinks(double delta) {
        getOutputLinks()
                .forEach(l -> {
                            double w = l.getSynapse().getWeight();
                            QueueEntry.add(l, new SumUpLink(delta * w));
                            QueueEntry.add(l.getOutput(), USE_FINAL_BIAS);
                        }
                );
    }

    public void followLinks(Visitor v) {
        v.onEvent(false);

        v.tryToLink(this);

        Direction dir = v.downUpDir;

        setMarked(true);
        dir.getLinks(this)
                .filter(l ->
                        l.followAllowed(dir)
                ).collect(Collectors.toList()).stream()
                .forEach(l ->
                        l.follow(v)
                );
        setMarked(false);

        v.onEvent(true);
    }

    public Link getInputLink(Neuron n) {
        return inputLinks.get(n.getProvider());
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
                        new OutputKey(s.getOutput().getProvider(), MAX_VALUE),
                        true
                );
    }

    public Link addLink(Synapse s, Activation input, boolean isSelfRef) {
        Link ol = getInputLink(s);
        Link nl = new Link(
                ol,
                s,
                input,
                this,
                isSelfRef
        );

        double w = s.getWeight();

        if (w > 0.0 || !nl.isSelfRef()) {
            QueueEntry.add(
                    nl,
                    new SumUpLink(w * (nl.getInputValue(POS) - nl.getInputValue(POS, ol)))
            );
        }

        return nl;
    }

    public void updateNet(double netDelta) {
        net += netDelta;

        Utils.checkTolerance(netDelta);

        QueueEntry.add(this, PROPAGATE_GRADIENTS_NET);
        QueueEntry.add(this, CHECK_IF_FIRED);
    }

    private double computeValue() {
        return branchProbability * getActivationFunction().f(net);
    }

    public void updateValue() {
        Double oldValue = value;

        value = inputValue != null ?
                inputValue :
                computeValue();

        double valueDelta = value - (oldValue != null ? oldValue : 0.0);

        Utils.checkTolerance(valueDelta);

        QueueEntry.add(this,
                new PropagateValueChange(valueDelta)
        );
    }

    public boolean checkIfFired() {
        if (fired == NOT_FIRED && value != null && value > 0.0) {
            setFired(neuron.incrementFired(getLatestFired()));
            return true;
        }
        return false;
    }

    private Fired getLatestFired() {
        return inputLinks.values().stream()
                .map(il -> il.getInput().getFired())
                .max(Fired.COMPARATOR)
                .orElse(null);
    }

    public void initEntropyGradient() {
        double g = getNeuron().getSurprisal(
                        Sign.getSign(this),
                        getReference()
                );

        inputGradient += g - lastEntropyGradient;
        lastEntropyGradient = g;
    }

    public void propagate() {
        QueueEntry.add(this, LINKING);
        QueueEntry.add(this, PROPAGATE);
        QueueEntry.add(this, USE_FINAL_BIAS);

        if (this.hasBranches())
            QueueEntry.add(this, DETERMINE_BRANCH_PROBABILITY);

        QueueEntry.add(this, COUNTING);
        this.addLinksToQueue(INPUT, LinkStep.COUNTING);
    }

    public void propagateGradientsFromSumUpdate() {
        ActivationFunction actF = getActivationFunction();

        inputGradientSum += inputGradient;
        double g = inputGradient;
        inputGradient = 0.0;

        g *= getNorm();
        g *= actF.outerGrad(net);

        propagateGradientsOut(g);
    }

    public void propagateGradientsFromNetUpdate() {
        ActivationFunction actF = getActivationFunction();

        double g = actF.outerGrad(net) - actF.outerGrad(lastNet);
        lastNet = net;

        Utils.checkTolerance(g);

        g *= getNorm();

        propagateGradientsOut(
                inputGradientSum * g
        );
    }

    public void propagateGradientsOut(double g) {
        outputGradientSum += g;

        if(!getNeuron().isInputNeuron())
            addLinksToQueue(INPUT, new PropagateGradient(g));

        if (getNeuron().isAllowTraining())
            QueueEntry.add(this,
                    new UpdateBias(getConfig().getLearnRate() * g)
            );

        addLinksToQueue(INPUT, LinkStep.TEMPLATE);

        QueueEntry.add(this, TEMPLATE_PROPAGATE_INPUT);
        QueueEntry.add(this, TEMPLATE_CLOSE_CYCLE_OUTPUT);
        QueueEntry.add(this, TEMPLATE_PROPAGATE_OUTPUT);
    }

    public double getNorm() {
        return (1 / (1 + getNeuron().getSampleSpace().getN(getReference())));
    }

    public void propagateGradientIn(double g) {
        inputGradient += g;

        Utils.checkTolerance(inputGradient);

        QueueEntry.add(this, PROPAGATE_GRADIENTS_SUM);
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
        Set<Activation> conflictingActs = branches
                .stream()
                .flatMap(bAct -> bAct.getInputLinks())
                .filter(l -> l.isNegative())
                .flatMap(l -> l.getInput().getInputLinks())  // Walk through to the inhib. Activation.
                .map(l -> l.getInput())
                .collect(Collectors.toSet());

        double offset = conflictingActs
                .stream()
                .mapToDouble(cAct -> cAct.net)
                .min()
                .getAsDouble();

        double norm = Math.exp(net - offset);
        norm += conflictingActs
                .stream()
                .mapToDouble(cAct -> Math.exp(cAct.net - offset))
                .sum();

        double p = Math.exp(net - offset) / norm;

        Utils.checkTolerance(p - getBranchProbability());

        Activation cAct = clone(null);
        cAct.branchProbability = p;
    }

    public void addLinksToQueue(Direction dir, LinkStep p) {
        dir.getLinks(this)
                .forEach(l ->
                        QueueEntry.add(l, p)
                );
    }

    public Stream<Link> getInputLinks() {
        return inputLinks.values().stream();
    }

    public Stream<Link> getOutputLinks() {
        return outputLinks.values().stream();
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
                " net:" + Utils.round(net) +
                " bp:" + Utils.round(branchProbability)
        );

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
