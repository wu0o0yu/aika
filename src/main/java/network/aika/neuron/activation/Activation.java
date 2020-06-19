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
import network.aika.neuron.excitatory.pattern.PatternNeuron;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.Phase.*;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;

/**
 *
 * @author Lukas Molzberger
 */
public class Activation implements Comparable<Activation> {

    public static double TOLERANCE = 0.001;

    private double value;
    private double sum;
    private double negSum;
    private Fired fired = NOT_FIRED;

    public double rangeCoverage;

    private int id;
    private Neuron<?> neuron;
    private Thought thought;

    private double p = 1.0;

    TreeMap<Link, Link> inputLinksFiredOrder;
    Map<NeuronProvider, Link> inputLinks;
    NavigableMap<Activation, Link> outputLinks;

    private boolean isFinal;

    private long visitedDown;
    private long visitedUp;

    private int round; // Only used as stopping criteria
    private Activation lastRound;

    private Set<Activation> branches = new TreeSet<>();
    private Activation mainBranch;

    private Reference groundRef;
    private Gradient latestGradient;

    private Activation(int id, Neuron<?> n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(int id, Thought t, Neuron<?> n) {
        this.id = id;
        this.thought = t;
        this.neuron = n;

        thought.addActivation(this);

        inputLinksFiredOrder = new TreeMap<>(Comparator
                .<Link, Fired>comparing(l -> l.getInput().getFired())
                .thenComparing(l -> l.getInput())
        );

        inputLinks = new TreeMap<>();
        outputLinks = new TreeMap<>(Comparator
                .<Activation, NeuronProvider>comparing(act -> act.getNeuronProvider())
                .thenComparing(act -> act)
        );

        System.out.println(toString());
    }

    public int getId() {
        return id;
    }

    public double getValue() {
        return value;
    }

    public double getNet(Phase p) {
        return sum + (p == INITIAL_LINKING ? 0.0 : negSum) + getNeuron().getTotalBias(p, CURRENT);
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

    public String getLabel() {
        return getNeuron().getLabel();
    }

    public Reference getGroundRef() {
        return groundRef;
    }

    public void setReference(Reference groundRef) {
        this.groundRef = groundRef;
    }

    public void setLastRound(Activation lrAct) {
        this.lastRound = lrAct;
    }

    public Activation getLastRound() {
        return lastRound;
    }

    public <N extends Neuron<?>> N getNeuron() {
        return (N) neuron;
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

    public void propagate(Activation.Builder input) {
        setValue(input.value);
        setFired(new Fired(input.inputTimestamp, input.fired));
        setRangeCoverage(input.rangeCoverage);

        input.getInputLinks()
                .stream()
                .map(iAct -> new Link(
                        getNeuron().getInputSynapse(iAct.getNeuronProvider()),
                        iAct,
                        this
                        )
                )
                .forEach(l -> addLink(l));

        isFinal = true;

        linkForward();
        thought.processActivations();
    }

    public Activation createBranch() {
        Activation clonedAct = new Activation(thought.createActivationId(), thought, neuron);
        clonedAct.setRound(round + 1);
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct);
        return clonedAct;
    }

    public Activation createUpdate() {
        Activation clonedAct = new Activation(id, thought, neuron);
        clonedAct.setRound(round + 1);
        clonedAct.setLastRound(this);
        linkClone(clonedAct);
        return clonedAct;
    }

    private void linkClone(Activation clonedAct) {
        inputLinks
                .values()
                .forEach(l -> {
                    new Link(l.getSynapse(), l.getInput(), clonedAct)
                            .link();
                });
    }

    private void setRound(int r) {
        this.round = r;
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

    public boolean isActive() {
        return value > 0.0;
    }

    public double getP() {
        return p;
    }

    public boolean isConflicting(long v) {
        return inputLinks.values().stream()
                .filter(l -> l.isNegative() && !l.isSelfRef())
                .flatMap(l -> l.getInput().inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .anyMatch(l -> l.getInput().visitedDown != v);
    }

    public void linkForward() {
        if(lastRound != null) {
            lastRound.outputLinks
                    .values()
                    .forEach(l ->
                            Link.link(l.getSynapse(), this, l.getOutput())
                    );
            lastRound.unlink();
            lastRound = null;
        }

        propagate();

        thought.processLinks();
    }

    public void propagate() {
        followDown(thought.createVisitedId(), this);

        Phase p = thought.getPhase();
        if(p == INITIAL_LINKING || p == FINAL_LINKING) {
            getNeuron()
                    .getOutputSynapses()
                    .filter(s -> s.isPropagate())
                    .filter(s -> outputLinkExists(s))
                    .map(s -> {
                        Activation oAct = createActivation(s.getOutput());
                        return new Link(s, this, oAct);
                    })
                    .forEach(l -> thought.add(l));
        } else if(p == INDUCTION) {
            // Todo: check if the outgoing link already exists.

            Neuron n = getNeuron().induceNeuron(this);
            if(n == null) return;
            Activation oAct = createActivation(n);
            Synapse s = n.induceSynapse(this, oAct);
            Link l = new Link(s, this, oAct);
            thought.add(l);
        }
    }

    private Activation createActivation(Neuron n) {
        return new Activation(thought.createActivationId(), thought, n);
    }

    public void addLink(Link l) {
        l.link();

        if(isFinal) return;

        if(!l.isNegative()) {
            sumUpLink(l);
        }

        l.propagate();
        propagate();
    }

    public boolean inputLinkExists(Synapse s) {
        return inputLinks.containsKey(s);
    }

    private boolean outputLinkExists(Synapse s) {
        return !outputLinks
                .subMap(
                        new Activation(Integer.MIN_VALUE, s.getOutput()),
                        new Activation(Integer.MAX_VALUE, s.getOutput())
                )
                .isEmpty();
    }

    public void sumUpLink(Link l) {
        double w = l.getSynapse().getWeight();
        sum += l.getInput().value * w;
        rangeCoverage += getNeuron().propagateRangeCoverage(l.getInput());

        checkIfFired(l);
    }

    public void updateForFinalPhase() {
        double initialNet = getNet(INITIAL_LINKING);
        double finalNet = getNet(FINAL_LINKING);

        if(Math.abs(finalNet - initialNet) > TOLERANCE) {
            thought.add(this);
        }
    }

    public void checkIfFired(Link l) {
        if(fired == NOT_FIRED && getNet(getThought().getPhase()) > 0.0) {
            fired = neuron.incrementFired(l.getInput().fired);
            thought.add(this);
        }
    }

    public void process() {
        Phase phase = getThought().getPhase();
        value = p * neuron.getActivationFunction().f(getNet(phase));
        isFinal = true;
        if(lastRound == null || !equals(lastRound)) {
            linkForward();
        }
    }

    public void processGradient() {
        assert !latestGradient.isFixed;
        latestGradient.isFixed = true;

        double g = latestGradient.gradient * getNeuron().getActivationFunction().outerGrad(getNet(FINAL_LINKING));

        ArrayList<Synapse> modSyns = new ArrayList<>();
        inputLinks
                .values()
                .forEach(l -> {
                    l.propagateGradient(thought.getTrainingConfig().getLearnRate(), g);
                    modSyns.add(l.getSynapse());
                });

        getNeuron().commit(modSyns);
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

        double net = getNet(getThought().getPhase());
        Set<Activation> conflictingActs = branches
                .stream()
                .flatMap(bAct -> bAct.inputLinks.values().stream())
                .filter(l -> l.isNegative())
                .flatMap(l -> l.getInput().inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .map(l -> l.getInput())
                .collect(Collectors.toSet());

        final double[] offset = new double[] {net};
        conflictingActs
                .stream()
                .forEach(
                        cAct -> offset[0] = Math.min(offset[0], cAct.getNet(getThought().getPhase()))
                );

        final double[] norm = new double[] {Math.exp(net - offset[0])};
        conflictingActs
                .stream()
                .forEach(
                        cAct -> norm[0] += Math.exp(cAct.getNet(getThought().getPhase()) - offset[0])
                );

        double p = Math.exp(net - offset[0]) / norm[0];

        if(Math.abs(p - getP()) <= TOLERANCE) return;

        Activation cAct = isFinal ? createUpdate() : this;
        cAct.sum = sum;
        cAct.negSum = negSum;
        cAct.p = p;

        thought.add(cAct);
    }

    public void count() {
        if(!isActive()) return;
        getNeuron().count(this);
    }

    public boolean equals(Activation act) {
        return Math.abs(value - act.value) <= TOLERANCE;
    }

    private Activation getMostRecentFinalActivation() {
        if(!isFinal && lastRound != null) {
            return lastRound;
        }
        return this;
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

    public void followDown(long v, Activation originAct) {
        if(visitedDown == v) return;
        visitedDown = v;

        inputLinks
                .values()
                .stream()
                .map(l -> l.getInput())
                .forEach(act -> {
                    if(!(act.getNeuron() instanceof PatternNeuron)) {
                        act.followDown(v, originAct);
                    }
                    act.followUp(v, originAct);
                });
    }

    public void followUp(long v, Activation originAct) {
        if(visitedUp == v) return;
        visitedUp = v;

        if(isConflicting(v)) return;

        tryToLink(originAct);

        outputLinks
                .values()
                .stream()
                .map(l -> l.getOutput())
                .forEach(act -> act.followUp(v, originAct));
    }

    private void tryToLink(Activation originAct) {
        Phase p = thought.getPhase();
        if(p == INITIAL_LINKING || p == FINAL_LINKING) {
            getNeuron()
                    .getOutputSynapses()
                    .filter(s -> !originAct.inputLinkExists(s))
                    .forEach(s -> Link.link(s, originAct, this));
        } else if(p == INDUCTION) {
            getNeuron()
                    .induceSynapse(this, originAct);
        }
    }

    public String toString() {
        return "A " + getId() + " " +
                getNeuron().getClass().getSimpleName() + ":" + getLabel() +
                " value:" + Utils.round(value) +
                " net:" + Utils.round(getNet(getThought().getPhase())) +
                " p:" + Utils.round(p) +
                " round:" + round;
    }

    @Override
    public int compareTo(Activation act) {
        return Integer.compare(id, act.id);
    }

    public static class Builder {
        private double value = 1.0;
        private int inputTimestamp;
        private int fired;
        private Set<Activation> inputLinks = new TreeSet<>();
        private double rangeCoverage;

        public Builder setValue(double value) {
            this.value = value;
            return this;
        }

        public Builder setInputTimestamp(int inputTimestamp) {
            this.inputTimestamp = inputTimestamp;
            return this;
        }

        public Builder setFired(int fired) {
            this.fired = fired;
            return this;
        }

        public Set<Activation> getInputLinks() {
            return this.inputLinks;
        }

        public Builder addInputLink(Activation iAct) {
            inputLinks.add(iAct);
            return this;
        }

        public Builder setRangeCoverage(double rangeCoverage) {
            this.rangeCoverage = rangeCoverage;
            return this;
        }
    }
}
