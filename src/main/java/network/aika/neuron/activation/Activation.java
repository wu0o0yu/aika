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
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.activation.*;
import network.aika.neuron.steps.link.LinkCounting;
import network.aika.neuron.steps.link.PropagateGradientAndUpdateWeight;
import network.aika.utils.Utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.callbacks.VisitorEvent.AFTER;
import static network.aika.callbacks.VisitorEvent.BEFORE;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.utils.Utils.logChange;

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
    private boolean finalMode = false;
    private boolean marked;

    private final int id;
    private Neuron<?> neuron;
    private Thought thought;

    private double branchProbability = 1.0;

    Map<NeuronProvider, Link> inputLinks;
    NavigableMap<OutputKey, Link> outputLinks;

    private final Set<Activation> branches = new TreeSet<>();
    private Activation mainBranch;

    private Reference reference;

    public final static int OWN = 0;
    public final static int INCOMING = 1;

    private double lastEntropyGradient = 0.0;
    private double[] inputGradient = new double[2];

    /**
     * Accumulates all gradients in case a new link is added that needs be get informed about the gradient.
     */
    private double[] outputGradientSum;
    private double[] inputGradientSum;

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

        EntropyGradient.add(this);

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

    public boolean isFinalMode() {
        return finalMode;
    }

    public void setFinalMode(boolean finalMode) {
        this.finalMode = finalMode;
    }

    public Activation getMainBranch() {
        return mainBranch;
    }

    public Set<Activation> getBranches() {
        return branches;
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

    public Stream<Activation> getAllBranches() {
        if (mainBranch != null)
            return Stream.concat(Stream.of(mainBranch), branches.stream());
        else
            return branches.stream();
    }

    public void followLinks(ActVisitor v) {
        v.onEvent(BEFORE);

        Direction dir = v.getCurrentDir();

        setMarked(true);
        dir.getLinks(this)
                .forEach(l ->
                        v.getVisitorTask()
                                .synapseTransition(v, l.getSynapse(), l)
                );

        setMarked(false);

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

    public boolean templateLinkExists(Direction dir, Synapse ts) {
        return dir.getLinks(this)
                .map(Link::getSynapse)
                .anyMatch(s -> s.isOfTemplate(ts));
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

        s.propagateActValue(
                nl,
                Link.getInputValueDelta(POS, nl, ol)
        );

        return nl;
    }

    public void updateNet(double netDelta) {
        double oldNet = net;
        net += netDelta;
        logChange(neuron, oldNet, net, "updateNet: net");

        Utils.checkTolerance(this, netDelta);

        PropagateGradientsNet.add(this);
        CheckIfFired.add(this);
    }

    private double computeValue() {
        return branchProbability * getActivationFunction().f(net);
    }

    public void updateValue() {
        double oldValue = value != null ? value : 0.0;

        value = inputValue != null ?
                inputValue :
                computeValue();

        logChange(neuron, oldValue, value, "updateValue: value");

        PropagateValueChange.add(this, value - oldValue);
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
        Linking.add(this);
        Propagate.add(this);
        SetFinalMode.add(this);

        BranchProbability.add(this);

        Counting.add(this);
        getInputLinks().forEach(l -> LinkCounting.add(l));

        if(Utils.belowTolerance(outputGradientSum))
            return;

        TemplatePropagate.add(this, INPUT);

        TemplateCloseLoop.add(this, OUTPUT);
        TemplatePropagate.add(this, OUTPUT);
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

        PropagateGradientAndUpdateWeight.addInputs(this, g);

        UpdateBias.add(this, getConfig().getLearnRate() * Utils.sum(g));

        TemplateCloseLoop.add(this, INPUT);

//        addLinksToQueue(INPUT, LinkStep.TEMPLATE);

        if(!isActive(false))
            return;

        TemplatePropagate.add(this, INPUT);
        TemplateCloseLoop.add(this, OUTPUT);
        TemplatePropagate.add(this, OUTPUT);
    }

    public void propagateGradientIn(double g) {
        inputGradient[INCOMING] += g;

        if(Utils.belowTolerance(inputGradient))
            return;

        PropagateGradientsSum.add(this);
    }

    public void linkInputs() {
        inputLinks
                .values()
                .forEach(Link::linkInput);
    }

    public void unlinkInputs() {
        inputLinks
                .values()
                .forEach(Link::unlinkInput);
    }

    public void linkOutputs() {
        outputLinks
                .values()
                .forEach(Link::linkOutput);
    }

    public void unlinkOutputs() {
        outputLinks
                .values()
                .forEach(Link::unlinkOutput);
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
                .flatMap(Activation::getInputLinks)
                .filter(Link::isNegative)
                .flatMap(l -> l.getInput().getInputLinks())  // Walk through to the inhib. Activation.
                .map(Link::getInput)
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
                .forEach(l -> {
                    sb.append(l.gradientsToString());
                    sb.append(" \n");
                });

        sb.append("\n");
        return sb.toString();
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean includeLink) {
        StringBuilder sb = new StringBuilder();
        sb.append("act ");
        sb.append(toShortString());
        sb.append(" value:" + (value != null ? Utils.round(value) : "X"));
        sb.append(" net:" + Utils.round(net));
        sb.append(" bp:" + Utils.round(branchProbability));

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
