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
import java.util.function.Consumer;

import static network.aika.Document.MAX_ROUND;

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

    private Activation parent;
    private Set<Activation> children = new TreeSet<>();

    public Decision decision;

    public double remainingWeight;
    public int cacheFactor = 1;
    public double p;

    public TreeMap<Link, Link> inputLinks = new TreeMap<>(INPUT_COMP);
    public Map<Link, Link> outputLinks = new TreeMap<>(OUTPUT_COMP);

    boolean isFinal;

    public int round;

    public ExcitatoryNeuron targetNeuron;


    public static Comparator<Link> INPUT_COMP = (l1, l2) -> {
        int r = l1.input.fired.compareTo(l2.input.fired);
        if (r != 0) return r;
        return Synapse.INPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
    };

    public static Comparator<Link> OUTPUT_COMP = (l1, l2) -> {
        int r =Synapse.OUTPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.output.getId(), l2.output.getId());
    };


    public Activation(Document doc, INeuron<?> n, double value, Fired fired) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = n;

        this.value = value;
        this.fired = fired;

        isFinal = true;
    }


    public Activation(Activation parent) {
        this.doc = parent.doc;
        this.neuron = parent.neuron;
        this.parent = parent;
//        this.round = newSearchNode() ? 0 : parent.round + 1;

        if(round > MAX_ROUND) {
            throw new Activation.OscillatingActivationsException(doc.activationsToString());
        }

        if(parent != null) {
            parent.children.add(this);
        }
    }


    public int getId() {
        return id;
    }

    public Document getDocument() {
        return doc;
    }

    public int getThreadId() {
        return doc.getThreadId();
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


    public Synapse getSynapseById(int synapseId) {
        return getNeuron().getSynapseById(synapseId);
    }


    public void process() {


    }


    private void compute() {
        INeuron.SynapseSummary ss = neuron.getSynapseSummary();

        fired = null;

        net = 0.0;
        for (Link l: inputLinks.values()) {
            Activation is = l.input;
            Synapse syn = l.getSynapse();

            double w = syn.getWeight();

            net += is.value * w;

            if(fired == null && net > 0.0) {
                fired = neuron.incrementFired(is.fired);
            }
        }

        value = neuron.getActivationFunction().f(net);
        weight = Math.max(0.0, Math.min(-ss.getNegRecSum(), net));
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


    public void traverse(Consumer<Activation> f) {
        f.accept(this);
        for(Activation c: children) {
            c.traverse(f);
        }
    }


    public void cleanup() {
        parent.children.remove(this);
        parent = null;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" d:"  + decision + " cacheFactor:" + cacheFactor + " w:" + Utils.round(weight) + " p:" + p + " value:" + Utils.round(value));
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
        public Double targetValue;
        public int inputTimestamp;
        public int fired;
        public Map<Integer, Activation> inputLinks = new TreeMap<>();


        public Builder setValue(double value) {
            this.value = value;
            return this;
        }


        public Builder setTargetValue(Double targetValue) {
            this.targetValue = targetValue;
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


        public Map<Integer, Activation> getInputLinks() {
            return this.inputLinks;
        }


        public void addInputLink(Integer synId, Activation iAct) {
            inputLinks.put(synId, iAct);
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
