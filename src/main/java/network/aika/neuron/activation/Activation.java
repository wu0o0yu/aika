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
import network.aika.Thought;
import network.aika.Utils;
import network.aika.neuron.*;
import network.aika.templates.LNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.InputKey.INPUT_COMP;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.Phase.PRELIMINARY_LINKING;

/**
 *
 * @author Lukas Molzberger
 */
public class Activation implements Comparable<Activation> {

    public static double TOLERANCE = 0.001;

    private double value;
    private double net;
    private Fired fired = NOT_FIRED;

    public double rangeCoverage;

    private int id;
    private Neuron<?> neuron;
    private Thought thought;

    private double p = 1.0;

    TreeMap<Link, Link> inputLinksFiredOrder;
    Map<InputKey, Link> inputLinks;
    NavigableMap<Activation, Link> outputLinks;

    private boolean assumePosRecLinks;
    private boolean requiresFullUpdate = false;
    private boolean isFinal;

    private LNode lNode;

    private int round; // Nur als Abbruchbedingung
    private Activation lastRound;

    private Set<Activation> branches = new TreeSet<>();
    private Activation mainBranch;

    private Reference groundRef;

    public Activation(int id, Thought t, Neuron<?> n) {
        this.id = id;
        this.thought = t;
        this.neuron = n;
        this.assumePosRecLinks = n.hasPositiveRecurrentSynapses() && t.getPhase() == PRELIMINARY_LINKING;
        this.net = n.getTotalBias(this.assumePosRecLinks, CURRENT);

        thought.addActivation(this);

        inputLinksFiredOrder = new TreeMap<>(Comparator
                .<Link, Boolean>comparing(l -> !l.isRecurrent())
                .thenComparing(l -> l.getInput().getFired())
                .thenComparing(l -> l.getInput())
        );

        inputLinks = new TreeMap<>(INPUT_COMP);
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

    public double getNet() {
        return net;
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

    public void setGroundRef(Reference groundRef) {
        this.groundRef = groundRef;
    }

    public LNode getLNode() {
        return lNode;
    }

    public void setLNode(LNode lNode) {
        this.lNode = lNode;
    }

    public void setLastRound(Activation lrAct) {
        this.lastRound = lrAct;
    }

    public Activation getLastRound() {
        return lastRound;
    }

    public <N extends Neuron> N getNeuron() {
        return (N) neuron;
    }

    public Model getModel() {
        return neuron.getModel();
    }

    public NeuronProvider getNeuronProvider() {
        return neuron.getProvider();
    }

    public Stream<Link> getOutputLinks(NeuronProvider n, PatternScope ps) {
        return outputLinks
                .values()
                .stream()
                .filter(l -> l.getOutput().getNeuronProvider().getId() == n.getId())
                .filter(l -> l.getSynapse().getPatternScope() == ps);
    }

    public void propagate(Activation.Builder input) {
        setValue(input.value);
        setFired(new Fired(input.inputTimestamp, input.fired));
        setRangeCoverage(input.rangeCoverage);

        input.getInputLinks()
                .entrySet()
                .stream()
                .map(me -> new Link(
                        getNeuron().getInputSynapse(me.getKey().getPInput(), me.getKey().getPatternScope()),
                        me.getValue(),
                        this
                        )
                )
                .forEach(l -> addLink(l));

        isFinal = true;
        assumePosRecLinks = false;

        linkForward();
        thought.processActivations();
    }

    public Activation createBranch() {
        Activation clonedAct = new Activation(thought.createActivationId(), thought, neuron);
        clonedAct.setRound(round + 1);
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct);
        clonedAct.requiresFullUpdate = true;
        return clonedAct;
    }

    public Activation createUpdate() {
        Activation clonedAct = new Activation(id, thought, neuron);
        clonedAct.setRound(round + 1);
        clonedAct.setLastRound(this);
        linkClone(clonedAct);
        clonedAct.requiresFullUpdate = true;
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
        this.value = v;
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

    public boolean isConflicting() {
        return inputLinks.values().stream()
                .filter(l -> l.isConflict() && !l.isSelfRef())
                .flatMap(l -> l.getInput().inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .anyMatch(l -> l.getInput().lNode == null);
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

        getNeuron().link(this);
        thought.processLinks();
    }

    public void addLink(Link l) {
        if(l.getSynapse().isRecurrent() || !isLastLink(l)) {
            requiresFullUpdate = true;
        }

        l.link();

        if(isFinal) return;

        if(requiresFullUpdate) {
            compute();
            requiresFullUpdate = false;
        }else{
            sumUpLink(l);
        }
    }

    public boolean inputLinkExists(Synapse s) {
        return inputLinks.containsKey(s);
    }

    public boolean outputLinkExists(Activation oAct) {
        return outputLinks.containsKey(oAct);
    }

    private boolean isLastLink(Link l) {
        return inputLinksFiredOrder.isEmpty() || l.getInput().fired.compareTo(inputLinksFiredOrder.lastKey().getInput().fired) >= 0;
    }

    public void sumUpLink(Link l) {
        double w = l.getSynapse().getWeight();
        net += l.getInput().value * w;
        rangeCoverage += getNeuron().propagateRangeCoverage(l.getInput());

        checkIfFired(l);
    }

    public void compute() {
        fired = NOT_FIRED;
        net = neuron.getTotalBias(assumePosRecLinks, CURRENT);
        for (Link l: inputLinksFiredOrder.values()) {
            sumUpLink(l);
        }
    }

    public void checkIfFired(Link l) {
        if(fired == NOT_FIRED && net > 0.0) {
            fired = neuron.incrementFired(l.getInput().fired);
            thought.add(this);
        }
    }

    public void process() {
        value = p * neuron.getActivationFunction().f(net);
        isFinal = true;
        if(lastRound == null || !equals(lastRound)) {
            linkForward();
        }
    }

    public void unlink() {
        inputLinks
                .values()
                .forEach(l -> l.unlink());
    }

    public void computeP() {
        if(!isActive()) return;

        Set<Activation> conflictingActs = branches
                .stream()
                .flatMap(bAct -> bAct.inputLinks.values().stream())
                .filter(l -> l.isConflict())
                .flatMap(l -> l.getInput().inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .map(l -> l.getInput())
                .collect(Collectors.toSet());

        final double[] offset = new double[] {net};
        conflictingActs
                .stream()
                .forEach(
                        cAct -> offset[0] = Math.min(offset[0], cAct.net)
                );

        final double[] norm = new double[] {Math.exp(net - offset[0])};
        conflictingActs
                .stream()
                .forEach(
                        cAct -> norm[0] += Math.exp(cAct.net - offset[0])
                );

        double p = Math.exp(net - offset[0]) / norm[0];

        if(Math.abs(p - getP()) <= TOLERANCE) return;

        Activation cAct = isFinal ? createUpdate() : this;
        cAct.net = net;
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

    public boolean assumePosRecLinks() {
        return assumePosRecLinks;
    }

    public boolean hasBranches() {
        return branches.isEmpty();
    }

    public String toString() {
        return getId() + " " +
                getNeuron().getClass().getSimpleName() + ":" + getLabel() +
                " value:" + Utils.round(value) +
                " net:" + Utils.round(net) +
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
        private Map<InputKey, Activation> inputLinks = new TreeMap<>(INPUT_COMP);
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

        public Map<InputKey, Activation> getInputLinks() {
            return this.inputLinks;
        }

        public Builder addInputLink(PatternScope ps, Activation iAct) {
            InputKey ik = new InputKey() {
                @Override
                public NeuronProvider getPInput() {
                    return iAct.getNeuronProvider();
                }

                @Override
                public PatternScope getPatternScope() {
                    return ps;
                }
            };

            inputLinks.put(ik, iAct);
            return this;
        }

        public Builder setRangeCoverage(double rangeCoverage) {
            this.rangeCoverage = rangeCoverage;
            return this;
        }
    }
}
