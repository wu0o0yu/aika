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
import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.Document.MAX_ROUND;
import static network.aika.neuron.Synapse.State.CURRENT;

/**
 *
 * @author Lukas Molzberger
 */
public class Activation {

    public double value;
    public double net;
    public Fired fired;
    public double weight;

    private int id;
    private INeuron<?> neuron;
    private Document doc;

    public double remainingWeight;
    public double p;

    public TreeMap<Link, Link> inputLinks = new TreeMap<>(INPUT_COMP);
    public Map<Link, Link> outputLinks = new TreeMap<>(OUTPUT_COMP);

    public boolean isFinal;

    public int round; // Nur als Abbruchbedingung

    public ExcitatoryNeuron targetNeuron;

    public long visited;


    public static Comparator<Link> INPUT_COMP = (l1, l2) -> {
        int r = l1.input.fired.compareTo(l2.input.fired);
        if (r != 0) return r;
        return l1.synapse.getInput().compareTo(l2.synapse.getInput());
    };

    public static Comparator<Link> OUTPUT_COMP = (l1, l2) -> {
        int r =Synapse.OUTPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
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



    public void followDown(long v, Predicate<Activation> predicate) {
        if(visited == v) return;
        visited = v;

        followUp(v, predicate);
        inputLinks
                .values()
                .stream()
                .forEach(l -> l.input.followDown(v, predicate));
    }


    public void followUp(long v, Predicate<Activation> predicate) {
        if(visited == v) return;
        visited = v;

        if(predicate.test(this)) {
            return;
        }

        outputLinks
                .values()
                .stream()
                .forEach(l -> l.output.followUp(v, predicate));
    }


    public boolean checkAlternativeBranch(long v) {
        // Siehe Grafik 18.12.2019
        Link ld = inputLinks
                .keySet()
                .stream()
                .filter(l -> !l.isRecurrent())  // Aktuell sind die rec links an den Anfang sortiert.
                .findFirst()
                .orElse(null);

        if(ld == null) {
            return false;
        }

        return ld.input
                .getOutputLinks(ld.synapse)
                .filter(act -> act != this) // An dieser Stelle wird über die alternativen Varianten iteriert.
                .flatMap(act -> act.inputLinks.values().stream())
                .filter(l -> l.isRecurrent() && l.isNegative(CURRENT))
                .flatMap(l -> l.input.inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .map(l -> l.input)
                .anyMatch(act -> act.visited == v);
    }


    public Synapse getSynapseById(int synapseId) {
        return getNeuron().getSynapseById(synapseId);
    }


    public Stream<Activation> getOutputLinks(Synapse s) {
        return outputLinks.values().stream()
                .filter(l -> l.synapse == s)
                .map(l -> l.getOutput());
    }


    public void addLink(Activation iAct, Synapse s) {
        Link l = new Link(s, iAct, this);
        l.link();

        assert !isFinal;

        sumUpLink(iAct, s);
    }


    public void sumUpLink(Activation iAct, Synapse s) {
        double w = s.getWeight();

        net += iAct.value * w;
        if(fired == null && net > 0.0) {
            fired = neuron.incrementFired(iAct.fired);
            doc.getQueue().add(this);
        }
    }


    public void process() {
        INeuron.SynapseSummary ss = neuron.getSynapseSummary();
        value = neuron.getActivationFunction().f(net);
        weight = Math.max(0.0, Math.min(-ss.getNegRecSum(), net));

        isFinal = true;

        neuron.propagate(this);
    }


    private void compute() {

        fired = null;

        net = 0.0;
        for (Link l: inputLinks.values()) {
            Activation is = l.input;
            Synapse syn = l.getSynapse();

            sumUpLink(is, syn);
        }

    }



    public boolean isFinalActivation() {
        return false;
    }

    public boolean isActive() {
        return value > 0.0;
    }

    public boolean equals(Activation s) {
        return Math.abs(value - s.value) <= INeuron.WEIGHT_TOLERANCE;
    }


    public boolean equalsWithWeights(Activation s) {
        return equals(s) && Math.abs(weight - s.weight) <= INeuron.WEIGHT_TOLERANCE;
    }



    private void link() {

    }


    public void setWeight(double weight) {
        this.weight = weight;
    }



    public boolean checkSelfReferencing(Activation act) {
        return false;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" w:" + Utils.round(weight) + " p:" + p + " value:" + Utils.round(value));
        return sb.toString();
    }

    public double getP() {
        return 0.0;
    }

    public String toStringDetailed() {
        return null;
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


        public void addInputLink(Integer synId, Activation iAct) {
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
