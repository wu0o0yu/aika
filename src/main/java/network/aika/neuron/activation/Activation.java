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

import network.aika.Document;
import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.pattern.PatternNeuron;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.Fired.NOT_FIRED;

/**
 *
 * @author Lukas Molzberger
 */
public class Activation implements Comparable<Activation> {

    public double value;
    public double net;
    public Fired fired = NOT_FIRED;

    private int id;
    private INeuron<?> neuron;
    private Document doc;

    public double p;

    public TreeMap<Link, Link> inputLinksFiredOrder;
    public Map<Neuron, Link> inputLinks;
    public NavigableMap<Activation, Link> outputLinks;

    public boolean isFinal;


    public long visitedDown;
    public long visitedUp;

    public int round; // Nur als Abbruchbedingung
    public Activation nextRound;
    public Activation lastRound;

    private Activation(int id, INeuron<?> n) {
        this.id = id;
        this.neuron = n;
    }

    public Activation(Document doc, INeuron<?> n, Activation lastRound, int round) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = n;
        this.round = round;

        this.net = n.getTotalBias(isInitialRound(), CURRENT);

        this.lastRound = lastRound;
        if(lastRound != null) {
            lastRound.nextRound = this;
        }

        doc.addActivation(this);

        inputLinksFiredOrder = new TreeMap<>(Comparator
                .<Link, Boolean>comparing(l -> !l.isRecurrent())
                .thenComparing(l -> l.input.getFired())
                .thenComparing(l -> l.input)
        );

        inputLinks = new TreeMap<>();

        outputLinks = new TreeMap<>(Comparator
                .<Activation, Neuron>comparing(act -> act.getNeuron())
                .thenComparing(act -> act)
        );
    }


    public int getId() {
        return id;
    }

    public Document getDocument() {
        return doc;
    }


    public String getLabel() {
        return getINeuron().getLabel();
    }


    public boolean isInitialRound() {
        return round == 0;
    }

    public <N extends INeuron> N getINeuron() {
        return (N) neuron;
    }


    public Neuron getNeuron() {
        return neuron.getProvider();
    }


    public Fired getFired() {
        return fired;
    }


    public void followDown(long v, CollectResults c) {
        if(visitedDown == v) return;
        visitedDown = v;

        inputLinks
                .values()
                .stream()
                .forEach(l -> {
                    if(!(l.input.getINeuron() instanceof PatternNeuron)) {
                        l.input.followDown(v, c);
                    }
                    l.input.followUp(v, c);
                });
    }


    public void followUp(long v, CollectResults c) {
        if(visitedUp == v) return;
        visitedUp = v;

        if(isConflicting(v)) return;

        c.collect(this);

        outputLinks
                .values()
                .stream()
                .forEach(l -> l.output.followUp(v, c));
    }


    public Activation cloneAct(boolean branch) {
        Activation clonedAct = new Activation(doc, neuron, this, round + 1);

        inputLinks
                .values()
                .forEach(l -> {
                    new Link(l.synapse, l.input, clonedAct).link();
                    if(!branch) {
                        l.unlink();
                    }
                });

        return clonedAct;
    }


    public void setValue(double v) {
        this.value = v;
    }

    public void setFired(Fired fired) {
        this.fired = fired;
    }

    public boolean outputLinkExists(INeuron n) {
        return !outputLinks.subMap(
                new Activation(Integer.MIN_VALUE, n),
                new Activation(Integer.MAX_VALUE, n)
        ).isEmpty();
    }

    @Override
    public int compareTo(Activation act) {
        return Integer.compare(id, act.id);
    }

    public interface CollectResults {
        void collect(Activation act);
    }

    public boolean isConflicting(long v) {
        if(isInitialRound()) {
            return false;
        }

        return inputLinks.values().stream()
                .filter(l -> l.isConflict())
                .flatMap(l -> l.input.inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .anyMatch(l -> l.input.visitedDown != v);
    }

    public Stream<Link> getOutputLinks(Synapse s) {
        return outputLinks
                .values()
                .stream()
                .filter(l -> l.synapse == s);
    }

    public void addLink(Link l) {
        assert l.output == null;

        l.output = this;
        l.link();

        if(isFinal || (isInitialRound() && l.isRecurrent())) return;

        if(inputLinks.size() == 1 || l.input.fired.compareTo(inputLinksFiredOrder.lastKey().input.fired) > 0) {
            sumUpLink(l);
        } else {
            compute();
        }
    }

    public void sumUpLink(Link l) {
        if(l.synapse == null) return;

        double w = l.synapse.getWeight();

        net += l.input.value * w;

        checkIfFired(l);
    }

    private void compute() {
        fired = NOT_FIRED;
        net = 0.0;
        for (Link l: inputLinksFiredOrder.values()) {
            sumUpLink(l);
        }
    }

    public void checkIfFired(Link l) {
        if(fired == NOT_FIRED && net > 0.0) {
            fired = neuron.incrementFired(l.input.fired);
            doc.getQueue().add(this);
        }
    }

    public void process() {
        value = neuron.getActivationFunction().f(net);

        isFinal = true;

        doc.getLinker().linkForward(this);

        lastRound = null;
    }

    public boolean isActive() {
        return value > 0.0;
    }

    public boolean equals(Activation s) {
        return Math.abs(value - s.value) <= INeuron.WEIGHT_TOLERANCE;
    }

    public String toString() {
        return getId() + " " +
                getINeuron().getClass().getSimpleName() + ":" + getLabel() +
                " value:" + Utils.round(value) +
                " net:" + Utils.round(net) +
                " p:" + Utils.round(p) +
                " round:" + round;
    }

    public double getP() {
        return 0.0;
    }


    public static class Builder {
        public double value = 1.0;
        public int inputTimestamp;
        public int fired;
        public List<Activation> inputLinks = new ArrayList<>();


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

        public List<Activation> getInputLinks() {
            return this.inputLinks;
        }

        public Builder addInputLink(Activation iAct) {
            inputLinks.add(iAct);
            return this;
        }
    }


    public static class OscillatingActivationsException extends RuntimeException {

        private String activationsDump;

        public OscillatingActivationsException(String activationsDump) {
            super("Maximum number of rounds reached. The network might be oscillating.");

            this.activationsDump = activationsDump;
        }

        public String getActivationsDump() {
            return activationsDump;
        }
    }
}
