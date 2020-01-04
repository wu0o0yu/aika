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
import network.aika.neuron.excitatory.ExcitatoryNeuron;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.Synapse.State.CURRENT;

/**
 *
 * @author Lukas Molzberger
 */
public class Activation {

    public double value;
    public double net;
    public Fired fired;

    private int id;
    private INeuron<?> neuron;
    private Document doc;

    public double p;

    public TreeMap<Link, Link> inputLinks = new TreeMap<>(INPUT_COMP);
    public Map<Link, Link> outputLinks = new TreeMap<>(OUTPUT_COMP);

    public boolean isFinal;

    public int round; // Nur als Abbruchbedingung

    public ExcitatoryNeuron targetNeuron;

    public long visitedDown;
    public long visitedUp;


    public static Comparator<Link> INPUT_COMP =
            Comparator.
                    <Link, Fired>comparing(l -> l.getInput().getFired())
                    .thenComparing(l -> l.getSynapse().getInput());


    public static Comparator<Link> OUTPUT_COMP = (l1, l2) -> {
        int r = Synapse.OUTPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.output.getId(), l2.output.getId());
    };


    public Activation(Document doc, INeuron<?> n, int round) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = n;
        this.round = round;

        this.net = n.getTotalBias(CURRENT);
        this.fired = null;

        doc.addActivation(this);
    }


    public Activation(Document doc, INeuron<?> n, double value, Fired fired) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = n;

        this.value = value;
        this.fired = fired;

        doc.addActivation(this);
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

        followUp(v, false, c);
        visitedDown = v;

        inputLinks
                .values()
                .stream()
                .forEach(l -> l.input.followDown(v, c));
    }


    public void followUp(long v, boolean isConflict, CollectResults c) {
        if(visitedDown == v || visitedUp == v) return;
        visitedUp = v;

        boolean ic = isConflict || !checkBranch(v, this);

        if(c.collect(this, ic)) {
            return;
        }

        outputLinks
                .values()
                .stream()
                .forEach(l -> l.output.followUp(v, ic, c));
    }


    public Activation cloneAct() {
        Activation clonedAct = new Activation(doc, neuron, round);

        clonedAct.value = value;
        clonedAct.net = net;
        clonedAct.fired = fired;
        clonedAct.p = p;
        clonedAct.isFinal = isFinal;
        clonedAct.targetNeuron = targetNeuron;
        clonedAct.inputLinks.putAll(inputLinks);

        return clonedAct;
    }


    public interface CollectResults {
        boolean collect(Activation act, boolean isConflict);
    }


    public boolean checkBranch(long v, Activation act) {
        return !act.inputLinks.values().stream()
                .filter(l -> l.isRecurrent() && l.isNegative(CURRENT))
                .flatMap(l -> l.input.inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .anyMatch(l -> l.input.visitedDown != v);
    }


    public Stream<Link> getOutputLinks(Synapse s) {
        return outputLinks.values().stream()
                .filter(l -> l.synapse == s);
    }


    public void addLink(Link l) {
        l.link();

        assert !isFinal;

        sumUpLink(l);
    }


    public void sumUpLink(Link l) {
        double w = l.synapse.getWeight();

        net += l.input.value * w;
        if(fired == null && net > 0.0) {
            fired = neuron.incrementFired(l.input.fired);
            doc.getQueue().add(this);
        }
    }


    public void process() {
        value = neuron.getActivationFunction().f(net);

        isFinal = true;

        neuron.propagate(this);
    }


    private void compute() {
        fired = null;

        net = 0.0;
        for (Link l: inputLinks.values()) {
            sumUpLink(l);
        }
    }


    public boolean isActive() {
        return value > 0.0;
    }

    public boolean equals(Activation s) {
        return Math.abs(value - s.value) <= INeuron.WEIGHT_TOLERANCE;
    }


    public String toString() {
        return getINeuron().getType() + ":" + getLabel() +
                " value:" + Utils.round(value) +
                " net:" + Utils.round(net) +
                " p:" + Utils.round(p);
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


        public void addInputLink(Activation iAct) {
            inputLinks.add(iAct);
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
