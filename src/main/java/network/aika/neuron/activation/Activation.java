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
import network.aika.neuron.phase.link.LinkPhase;
import network.aika.neuron.phase.link.PropagateGradient;
import network.aika.neuron.phase.link.SumUpLink;
import network.aika.neuron.sign.Sign;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.neuron.activation.RoundType.ACT;
import static network.aika.neuron.activation.RoundType.GRADIENT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.phase.activation.ActivationPhase.*;
import static network.aika.neuron.sign.Sign.POS;

/**
 * @author Lukas Molzberger
 */
public class Activation extends Element<Activation> {

    public static final Comparator<Activation> ID_COMPARATOR = Comparator.comparingInt(act -> act.id);

    public static final Comparator<Activation> FIRED_COMPARATOR = (act1, act2) -> Fired.COMPARATOR.compare(act1.getFired(), act2.getFired());
    public static final Comparator<Activation> FIRED_COMPARATOR_REVERSED = FIRED_COMPARATOR.reversed();

    public static double TOLERANCE = 0.001;

    private Double value = null;
    private Double inputValue = null;
    private double sum;
    private double lateSum;
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

    private double lastNet = 0.0;

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

        QueueEntry.add(this, LINK_AND_PROPAGATE);
        QueueEntry.add(this, ENTROPY_GRADIENT);
        QueueEntry.add(this, COUNTING);
    }

    public int getId() {
        return id;
    }

    public Double getValue() {
        return value;
    }

    public double getInputGradient() {
        return inputGradient;
    }

    public double getOutputGradientSum() {
        return outputGradientSum;
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
        fired = f;
    }

    public Thought getThought() {
        return thought;
    }

    protected int getElementType() {
        return 0;
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
                            QueueEntry.add(l, new SumUpLink(delta));
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

    public Link addLink(Synapse s, Activation input, boolean isSelfRef, int round) {
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
                    this,
                    new SumUpLink(w * (nl.getInputValue(POS) - nl.getInputValue(POS, ol)))
            );
        }

        return nl;
    }

    public void addToSum(double x) {
        if (value != null) {
            lateSum += x;
        } else {
            sum += x;
        }
    }


    private double computeValue(boolean isFinal) {
        return branchProbability *
                neuron.getActivationFunction().f(
                        getNet(isFinal)
                );
    }

    public double updateValue(boolean isFinal) {
        Double oldValue = value;

        value = inputValue != null ?
                inputValue :
                computeValue(isFinal);

        return value - (oldValue != null ? oldValue : 0.0);
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

    public void propagateGradientsFromSumUpdate() {
        if (gradientIsZero())
            return;

        ActivationFunction actF = getNeuron().getActivationFunction();

        double g = inputGradient;
        inputGradientSum += inputGradient;
        inputGradient = 0.0;

        double net = getNet(true);
        g *= getNorm();
        g *= actF.outerGrad(net);
        lastNet = net;

        propagateGradients(g);
    }

    public void propagateGradientsFromNetUpdate() {
        ActivationFunction actF = getNeuron().getActivationFunction();

        double net = getNet(true);
        double netDerivedLast = actF.outerGrad(lastNet);
        double netDerivedCurrent = actF.outerGrad(net);

        lastNet = net;

        double netDerivedDelta = netDerivedCurrent - netDerivedLast;
        if(Math.abs(netDerivedDelta) < TOLERANCE)
            return;

        netDerivedDelta *= getNorm();

        double g = inputGradientSum * netDerivedDelta;

        propagateGradients(g);
    }

    public void propagateGradients(double g) {
        outputGradientSum += g;

        if(!getNeuron().isInputNeuron())
            addLinksToQueue(INPUT, new PropagateGradient(g));

        addLinksToQueue(INPUT, LinkPhase.TEMPLATE);

        if (getNeuron().isAllowTraining())
            QueueEntry.add(this, UPDATE_BIAS);

        QueueEntry.add(this, TEMPLATE_INPUT);
        QueueEntry.add(this, TEMPLATE_OUTPUT);
    }

    public double getNorm() {
        return (1 / (1 + getNeuron().getSampleSpace().getN(getReference())));
    }

    public boolean gradientIsZero() {
        return Math.abs(inputGradient) < TOLERANCE;
    }

    public boolean gradientSumIsZero() {
        return Math.abs(outputGradientSum) < TOLERANCE;
    }

    public void propagateGradient(double g) {
        inputGradient += g;

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

        Activation cAct = clone(null);
        cAct.branchProbability = p;
    }

    public void addLinksToQueue(Direction dir, LinkPhase p) {
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
                " net:" + Utils.round(getNet(false)) +
                " netFinal:" + Utils.round(getNet(true)) +
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
