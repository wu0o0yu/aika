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
import network.aika.neuron.excitatory.PatternNeuron;

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

    private double rangeCoverage;

    private int id;
    private Neuron<?> neuron;
    private Thought thought;

    private double p = 1.0;

    Map<NeuronProvider, Link> inputLinks;
    NavigableMap<Activation, Link> outputLinks;

    private boolean isFinal;

    private long visited;
    private long visitedDown;

    private int round; // Only used as stopping criteria
    private Activation lastRound;

    private Set<Activation> branches = new TreeSet<>();
    private Activation mainBranch;

    private Reference groundRef;
    private Gradient latestGradient;

    public Activation(Thought t, Neuron<?> n) {
        this(t.createActivationId(), t, n);
    }

    private Activation(int id, Neuron<?> n) {
        this.id = id;
        this.neuron = n;
    }

    private Activation(int id, Thought t, Neuron<?> n) {
        this.id = id;
        this.thought = t;
        this.neuron = n;

        thought.addActivation(this);

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

    public Reference getGroundRef() {
        return groundRef;
    }

    public void setReference(Reference groundRef) {
        this.groundRef = groundRef;
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

    public Stream<Link> getOutputLinks(NeuronProvider n) {
        return outputLinks
                .values()
                .stream()
                .filter(l -> l.getOutput().getNeuronProvider().getId() == n.getId());
    }

    public void propagateInput() {
        isFinal = true;

        linkForward();
        thought.processActivations();
    }

    public Activation createBranch() {
        Activation clonedAct = new Activation(thought.createActivationId(), thought, neuron);
        clonedAct.round = round + 1;
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct);
        return clonedAct;
    }

    public Activation getModifiable() {
        if(!isFinal) return this;

        Activation clonedAct = new Activation(id, thought, neuron);
        clonedAct.round = round + 1;
        clonedAct.lastRound = this;
        clonedAct.value = value;
        clonedAct.sum = sum;
        clonedAct.lateSum = lateSum;
        linkClone(clonedAct);

        return clonedAct;
    }

    private void linkClone(Activation clonedAct) {
        inputLinks
                .values()
                .forEach(l ->
                        new Link(l.getSynapse(), l.getInput(), clonedAct)
                            .link()
                );
    }

    public void setValue(double v) {
        value = v;
    }

    public void setFired(Fired fired) {
        this.fired = fired;
    }

    public void setRangeCoverage(double rangeCoverage) {
        this.rangeCoverage = rangeCoverage;
    }

    public double getRangeCoverage() {
        return rangeCoverage;
    }

    public boolean isActive() {
        return value > 0.0;
    }

    public double getP() {
        return p;
    }

    public boolean isConflicting(long v) {
        return inputLinks.values().stream()
                .filter(l -> l.isNegative() && !l.isSelfRef())
                .flatMap(l -> l.getInput().getLinks(INPUT))  // Walk through to the inhib. Activation.
                .anyMatch(l -> l.getInput().visitedDown != v);
    }

    public void linkForward() {
        if(lastRound == null) {
            propagate();
        } else {
            lastRound.outputLinks
                    .values()
                    .forEach(l ->
                            Link.link(l.getSynapse(), this, l.getOutput())
                    );
            lastRound.unlink();
            lastRound = null;
        }

        thought.processLinks();
    }

    public void propagate() {
        followDown(thought.createVisitedId(), this, OUTPUT);
        getModel().linkInputRelations(this, OUTPUT);
        thought.processLinks();

        Phase p = thought.getPhase();
        Neuron<?> n = getNeuron();

        if(p == INITIAL_LINKING || p == FINAL_LINKING) {
            n.getOutputSynapses()
                    .filter(s -> s.isPropagate())
                    .filter(s -> !outputLinkExists(s))
                    .forEach(s ->
                            Link.link(
                                    s,
                                    this,
                                    createActivation(s.getOutput())
                            )
                    );
        } else if(p == INDUCTION) {
            n.induceNeuron(this);
        }
    }

    public Link connectInducedNeuron(Neuron on) {
        Activation oAct = new Activation(thought.createActivationId(), thought, on);
        return Link.link(
                on.induceSynapse(this, oAct),
                this,
                oAct
        );
    }

    private Activation createActivation(Neuron n) {
        Activation act = new Activation(thought.createActivationId(), thought, n);
        getModel().linkInputRelations(act, INPUT);
        return act;
    }

    public Link addLink(Link l) {
        l.link();

        sumUpLink(l);

        l.propagate();
        return l;
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

    public void sumUpLink(Link l) {
        if(l.isNegative() && l.isSelfRef()) return;

        double w = l.getSynapse().getWeight();
        double s = l.getInput().value * w;

        if(isFinal || l.isNegative()) {
            lateSum += s;
        } else {
            sum += s;
        }

        rangeCoverage += getNeuron().propagateRangeCoverage(l);

        checkIfFired(l);
    }

    public void updateForFinalPhase() {
        double initialValue = computeValue(INITIAL_LINKING);
        double finalValue = computeValue(FINAL_LINKING);

        if(Math.abs(finalValue - initialValue) > TOLERANCE) {
            thought.add(getModifiable());
        }
    }

    public void checkIfFired(Link l) {
        if(fired == NOT_FIRED && getNet() > 0.0) {
            fired = neuron.incrementFired(l.getInput().fired);
            thought.add(this);
        }
    }

    public void process() {
        value = computeValue(thought.getPhase());
        isFinal = true;
        if (lastRound == null || !equals(lastRound)) {
            linkForward();
        }
    }

    private double computeValue(Phase phase) {
        return p *
                neuron.getActivationFunction().f(
                        getNet(phase)
                );
    }

    public void processGradient() {
        assert !latestGradient.isFixed;
        latestGradient.isFixed = true;

        double g = latestGradient.gradient *
                getNeuron().getActivationFunction().outerGrad(
                        getNet()
                );

        inputLinks
                .values()
                .forEach(l ->
                        l.propagateGradient(thought.getTrainingConfig().getLearnRate(), g)
                );
    }

    public Gradient getMutableGradient() {
        if(latestGradient == null || latestGradient.isFixed) {
            latestGradient = new Gradient(latestGradient);
            getThought().addToGradientQueue(this);
        }
        return latestGradient;
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

        Activation cAct = getModifiable();
        cAct.p = p;

        thought.add(cAct);
    }

    public void count() {
        getNeuron().count(this);
    }

    public boolean equals(Activation act) {
        return Math.abs(value - act.value) <= TOLERANCE;
    }

    private Activation getMostRecentFinalActivation() {
        return !isFinal && lastRound != null ? lastRound : this;
    }

    public Stream<Link> getLinks(Direction dir) {
        switch(dir) {
            case OUTPUT:
                Activation act = getMostRecentFinalActivation();
                return act.outputLinks.values().stream();
            case INPUT:
                return inputLinks.values().stream();
        }
        return null;
    }

    public boolean hasBranches() {
        return branches.isEmpty();
    }

    public void followDown(long v, Activation originAct, Direction dir) {
        if(visited == v) return;
        visitedDown = v;

        followUp(v, originAct, dir);

        if(this == originAct || !(getNeuron() instanceof PatternNeuron)) {
            inputLinks
                    .values()
                    .stream()
                    .filter(l -> l.getInput() != null)
                    .map(l -> l.getInput())
                    .forEach(act -> act.followDown(v, originAct, dir));
        }
    }

    public void followUp(long v, Activation originAct, Direction dir) {
        if(visited == v) return;
        visited = v;

        if(this == originAct || isConflicting(v)) return;

        Activation iAct = dir == INPUT ? this : originAct;
        Activation oAct = dir == OUTPUT ? this : originAct;

        if(!oAct.getNeuron().isBlocked()) {
            oAct.getNeuron().tryToLink(iAct, oAct);
        }

        outputLinks
                .values()
                .stream()
                .map(l -> l.getOutput())
                .forEach(act -> act.followUp(v, originAct, dir));
    }

    public String toString() {
        return "Act id:" + getId() + " " +
                getNeuron().getClass().getSimpleName() + ":" + getDescriptionLabel() +
                " value:" + Utils.round(value) +
                " net:" + Utils.round(getNet()) +
                " p:" + Utils.round(p) +
                " round:" + round;
    }

    @Override
    public int compareTo(Activation act) {
        return Integer.compare(id, act.id);
    }
}
