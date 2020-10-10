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

import network.aika.Model;
import network.aika.Phase;
import network.aika.Thought;
import network.aika.Utils;
import network.aika.neuron.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.Phase.*;
import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;

/**
 *
 * @author Lukas Molzberger
 */
public class Activation implements Comparable<Activation> {

    public static double TOLERANCE = 0.001;

    private double value;
    private double sum;
    private double lateSum;
    private Fired fired = NOT_FIRED;

    private int id;
    private Neuron<?> neuron;
    private Thought thought;

    private double p = 1.0;

    Map<NeuronProvider, Link> inputLinks;
    NavigableMap<Activation, Link> outputLinks;

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
        outputLinks = new TreeMap<>(Comparator
                .<Activation, NeuronProvider>comparing(act -> act.getNeuronProvider())
                .thenComparing(act -> act)
        );
    }

    public int getId() {
        return id;
    }

    public double getValue() {
        return value;
    }

    public double getNet(Phase p) {
        return sum + (p == INITIAL_LINKING ? 0.0 : lateSum) + getNeuron().getBias(p);
    }

    public double getNet() {
        return getNet(thought.getPhase());
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

    public String getDescriptionLabel() {
        return getNeuron().getDescriptionLabel();
    }

    public Phase getPhase() {
        return thought.getPhase();
    }

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference groundRef) {
        this.reference = groundRef;
    }

    public Activation getLastRound() {
        return lastRound;
    }

    public Neuron getNeuron() {
        return neuron;
    }

    public Model getModel() {
        return neuron.getModel();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
    }

    public void propagateInput() {
        isFinal = true;

        linkForward();
        thought.processActivations();
    }

    public Activation createBranch(Synapse excludedSyn) {
        Activation clonedAct = new Activation(thought.createActivationId(), thought, neuron);
        clonedAct.round = round + 1;
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);
        return clonedAct;
    }

    public Activation getModifiable(Synapse excludedSyn) {
        if(!isFinal) return this;

        Activation clonedAct = new Activation(id, thought, neuron);
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
    }

    public boolean isActive() {
        return value > 0.0 || !getNeuron().isInitialized();
    }

    public double getP() {
        return p;
    }

    public boolean isConflicting() {
        return getConflictingMainBranches()
                .anyMatch(act -> act.searchWithinBranch());
    }

    public boolean searchWithinBranch() {
        if(marked) return true;

        return getLinks(OUTPUT)
                .filter(l -> !l.isNegative() || l.isCausal())
                .map(l -> l.getOutput())
                .filter(act -> act.fired != NOT_FIRED && fired.compareTo(act.fired) == -1)
                .anyMatch(act -> act.searchWithinBranch());
    }

    public Stream<Activation> getConflictingMainBranches() {
        if(mainBranch != null) {
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

    public void linkForward() {
        if(lastRound == null) {
            propagate();
        } else {
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

        thought.processLinks();
    }

    public void propagate() {
        new Visitor(this, OUTPUT)
                .followLinks(this);

        getModel().linkInputRelations(this, OUTPUT);
        thought.processLinks();

        Neuron<?> n = getNeuron();

        if(thought.getPhase() == INDUCTION) {
            n.induceNeuron(this);
        } else {
            n.getOutputSynapses()
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
    }

    public Activation createActivation(Neuron n) {
        Activation act = new Activation(thought.createActivationId(), thought, n);
        getModel().linkInputRelations(act, INPUT);
        return act;
    }

    public Link getInputLink(Synapse s) {
        return inputLinks.get(s.getPInput());
    }

    public boolean inputLinkExists(Synapse s) {
        return inputLinks.containsKey(s.getPInput());
    }

    public boolean outputLinkExists(Activation oAct) {
        return outputLinks.containsKey(oAct);
    }

    public boolean outputLinkExists(Synapse s) {
        return !getOutputLinks(s).isEmpty();
    }

    public SortedMap<Activation, Link> getOutputLinks(Synapse s) {
        return outputLinks
                .subMap(
                        new Activation(Integer.MIN_VALUE, s.getOutput()),
                        true,
                        new Activation(Integer.MAX_VALUE, s.getOutput()),
                        true
                );
    }

    public Link addLink(Synapse s, Activation input, boolean isSelfRef) {
        Link ol = getInputLink(s);
        Link nl = new Link(s, input, this, isSelfRef);
        nl.linkInput();
        nl.linkOutput();

        sumUpLink(ol, nl);
        checkIfFired(nl);

        if(!s.isNegative()) {
            getThought().addLinkToQueue(nl);
        }
        return nl;
    }

    public void sumUpLink(Link ol, Link nl) {
        assert ol == null || !isFinal;

        if(nl.isNegative() && nl.isSelfRef()) return;

        double w = nl.getSynapse().getWeight();
        double x = nl.getInput().value - (ol != null ? ol.getInput().value : 0.0);
        double s = x * w;

        if(isFinal) {
            lateSum += s;
        } else {
            sum += s;
        }

        if(!isFinal) {
            nl.getOutput().getNeuron().updateReference(nl);
        }
    }

    public void updateForFinalPhase() {
        double initialValue = computeValue(INITIAL_LINKING);
        double finalValue = computeValue(FINAL_LINKING);

        if(Math.abs(finalValue - initialValue) > TOLERANCE) {
            thought.addActivationToQueue(getModifiable(null));
        }
    }

    private void checkIfFired(Link l) {
        if(fired == NOT_FIRED && getNet() > 0.0) {
            fired = neuron.incrementFired(getLatestFired());
            thought.addActivationToQueue(this);
        }
    }

    private Fired getLatestFired() {
        return inputLinks.values().stream()
                .map(il -> il.getInput().getFired())
                .max(Fired::compareTo)
                .orElse(null);
    }

    public void process() {
        value = computeValue(thought.getPhase());
        isFinal = true;
        if (!equals(lastRound)) {
            linkForward();
        }
    }

    private double computeValue(Phase phase) {
        return p *
                neuron.getActivationFunction().f(
                        getNet(phase)
                );
    }

    public void computeInitialLinkGradients() {
        getInputLinks()
                .forEach(l -> l.computeInitialGradient());

        getInputLinks()
                .forEach(l -> l.removeGradientDependencies());
    }

    public void initSelfGradient() {
        selfGradient = getActFunctionDerivative() *
                getNeuron().getSurprisal(
                        Sign.getSign(this)
                );
    }

    public void addInitialLinkGradient(double initialLinkGradient) {
        selfGradient += initialLinkGradient;
    }

    public void updateSelfGradient() {
        getInputLinks().forEach(l ->
                l.updateAndPropagateSelfGradient()
        );
    }

    public double getNormSelfGradient() {
        return selfGradient / getN();
    }

    public int getN() {
        return getModel().getN() + reference.getEnd() - getNeuron().getInstances().getOffset();
    }

    public void processGradient() {
        if(getNeuron().isInputNeuron())
            return;

        inputLinks.values()
                .forEach(l ->
                        l.propagateGradient(unpropagatedGradient)
                );

        unpropagatedGradient = 0.0;

        getNeuron().updatePropagateFlag();
    }

    public double getActFunctionDerivative() {
        if(!getNeuron().isInitialized())
            return 1.0;

        return getNeuron()
                .getActivationFunction()
                .outerGrad(
                        getNet()
                );
    }

    public void propagateGradient(double g) {
        unpropagatedGradient += g;

        getThought().addToGradientQueue(this);
    }

    public void unlink() {
        inputLinks
                .values()
                .forEach(l -> l.unlink());
    }

    public void computeP() {
        if(!isActive()) return;

        double net = getNet();
        Set<Activation> conflictingActs = branches
                .stream()
                .flatMap(bAct -> bAct.inputLinks.values().stream())
                .filter(l -> l.isNegative())
                .flatMap(l -> l.getInput().inputLinks.values().stream())  // Walk through to the inhib. Activation.
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

        updateP(Math.exp(net - offset) / norm);
    }

    private void updateP(double p) {
        if(Math.abs(p - getP()) <= TOLERANCE) return;

        Activation cAct = getModifiable(null);
        cAct.p = p;

        thought.addActivationToQueue(cAct);
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
        return branches.isEmpty();
    }

    public Stream<Link> getLinks(Direction dir) {
        return (dir == INPUT ? inputLinks : outputLinks)
                .values()
                .stream();
    }

    @Override
    public int compareTo(Activation act) {
        return Integer.compare(id, act.id);
    }

    public String toString() {
        return "Act " +
                getIdString() +
                " value:" + Utils.round(value) +
                " net:" + Utils.round(getNet()) +
                " p:" + Utils.round(p) +
                " round:" + round;
    }

    public String getIdString() {
        return "id:" +
                getId() + " " +
                getNeuron();
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
}
