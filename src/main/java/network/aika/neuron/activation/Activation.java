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
import network.aika.neuron.ActivationFunction;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.activation.visitor.Visitor;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.activation.PropagateValueChange;
import network.aika.neuron.steps.activation.UpdateBias;
import network.aika.neuron.steps.link.LinkStep;
import network.aika.neuron.steps.link.PropagateGradientAndUpdateWeight;
import network.aika.neuron.steps.link.SumUpLink;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.callbacks.VisitorEvent.AFTER;
import static network.aika.callbacks.VisitorEvent.BEFORE;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.neuron.steps.activation.ActivationStep.*;

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

    public final static int OWN = 0;
    public final static int INCOMING = 1;

    private double lastEntropyGradient = 0.0;
    private double inputGradient[] = new double[2];

    /**
     * Accumulates all gradients in case a new link is added that needs be get informed about the gradient.
     */
    private double outputGradientSum[];
    private double inputGradientSum[];

    public boolean markedNetUpdateOccurred; // Temporary hack


    private Activation(int id, Neuron<?> n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(int id, Thought t, Neuron<?> n, Activation fromAct) {
        this(id, n);
        this.thought = t;

        lastNet = n.isTemplate() || n.isInputNeuron() ? 0.0 : -1.0;
        net = n.getBias();

        thought.registerActivation(this);

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(OutputKey.COMPARATOR);

        t.onActivationCreationEvent(this, fromAct);
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

        if(getConfig().isEnableTraining())
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

    public double[] getInputGradient() {
        return inputGradient;
    }

    public double[] getOutputGradientSum() {
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
        assert ref.getThought() == getThought();
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
        return getThought().getConfig();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
    }

    public Activation createBranch(Synapse excludedSyn) {
        Activation clonedAct = thought.createActivation(neuron);

        copyPhases(clonedAct);
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);
        return clonedAct;
    }

    public Activation clone(Synapse excludedSyn) {
        if (value == null)
            return this;

        Activation clonedAct = new Activation(id, thought, neuron, null);

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
                .filter(l -> !l.getSynapse().isZero())
                .forEach(l -> {
                            double w = l.getSynapse().getWeight();
                            QueueEntry.add(l, new SumUpLink(delta * w));
                            QueueEntry.add(l.getOutput(), USE_FINAL_BIAS);
                        }
                );
    }

    public void follow(ActVisitor v) {
        getNeuron().transition(v);

        v.onEvent(BEFORE);

        Direction dir = v.getCurrentDir();

        setMarked(true);
        dir.getLinks(this)
                .filter(l -> {
                            Activation nextAct = dir.getActivation(l);
                            return !l.isNegative() &&
                                    nextAct != null &&
                                    !nextAct.isMarked();
                        }
                )
                .forEach(l ->
                        v.getVisitorTask().transition(v, l.getSynapse(), l)
                );

        setMarked(false);

//        v.tryToLink(this);

        v.getVisitorTask()
                .processTask(v);

        v.onEvent(AFTER);
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

    public boolean templateLinkExists(Direction dir, Synapse ts, boolean invertRecurrent) {
        if(invertRecurrent && ts.isRecurrent())
            dir = dir.invert();

        return dir.getLinks(this)
                .map(l -> l.getSynapse())
                .anyMatch(s -> s.isOfTemplate(ts));
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
                s,
                input,
                this,
                isSelfRef
        );

        double w = s.getWeight();

        if (!Utils.belowTolerance(w))
            QueueEntry.add(
                    nl,
                    new SumUpLink(w * Link.getInputValueDelta(POS, nl, ol))
            );

        return nl;
    }

    public void updateNet(double netDelta) {
        net += netDelta;

        Utils.checkTolerance(this, netDelta);

        if(!markedNetUpdateOccurred)
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

        Utils.checkTolerance(this, valueDelta);

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

        inputGradient[OWN] += g - lastEntropyGradient;
        lastEntropyGradient = g;
    }

    public void propagate() {
        QueueEntry.add(this, LINKING);
        QueueEntry.add(this, PROPAGATE);
        QueueEntry.add(this, USE_FINAL_BIAS);

        if (hasBranches())
            QueueEntry.add(this, DETERMINE_BRANCH_PROBABILITY);

        QueueEntry.add(this, COUNTING);
        addLinksToQueue(INPUT, LinkStep.COUNTING);

        if(Utils.belowTolerance(outputGradientSum))
            return;

        QueueEntry.add(this, TEMPLATE_PROPAGATE_INPUT);

        QueueEntry.add(this, TEMPLATE_CLOSE_LOOP_OUTPUT);
        QueueEntry.add(this, TEMPLATE_PROPAGATE_OUTPUT);
    }

    public void propagateGradientsFromSumUpdate() {
        ActivationFunction actF = getActivationFunction();

        inputGradientSum = Utils.add(inputGradientSum, inputGradient);
        double[] g = inputGradient;
        inputGradient = new double[2];

        g = Utils.scale(g, actF.outerGrad(lastNet));

        propagateGradientsOut(g);
    }

    public void propagateGradientsFromNetUpdate() {
        if(inputGradientSum == null)
            return;

        ActivationFunction actF = getActivationFunction();

        double g = actF.outerGrad(net) - actF.outerGrad(lastNet);
        lastNet = net;

        propagateGradientsOut(
                Utils.scale(inputGradientSum, g)
        );
    }

    public void propagateGradientsOut(double[] g) {
        Utils.checkTolerance(this, g);

        outputGradientSum = Utils.add(outputGradientSum, g);

        if(!getNeuron().isInputNeuron())
            addLinksToQueue(INPUT, new PropagateGradientAndUpdateWeight(g));

        if (getNeuron().isAllowTraining())
            QueueEntry.add(this,
                    new UpdateBias(getConfig().getLearnRate() * Utils.sum(g))
            );


        QueueEntry.add(this, TEMPLATE_CLOSE_LOOP_INPUT);

//        addLinksToQueue(INPUT, LinkStep.TEMPLATE);

        if(!isActive(false))
            return;

        QueueEntry.add(this, TEMPLATE_PROPAGATE_INPUT);

        QueueEntry.add(this, TEMPLATE_CLOSE_LOOP_OUTPUT);
        QueueEntry.add(this, TEMPLATE_PROPAGATE_OUTPUT);
    }

    public void propagateGradientIn(double g) {
        inputGradient[INCOMING] += g;

        if(Utils.belowTolerance(inputGradient))
            return;

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

        Utils.checkTolerance(this, p - getBranchProbability());

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
