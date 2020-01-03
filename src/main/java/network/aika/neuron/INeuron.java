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
package network.aika.neuron;


import network.aika.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static network.aika.neuron.Synapse.State.CURRENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class INeuron<S extends Synapse> extends AbstractNode<Neuron> implements Comparable<INeuron> {

    private static final Logger log = LoggerFactory.getLogger(INeuron.class);

    public static double WEIGHT_TOLERANCE = 0.001;


    public static final INeuron MIN_NEURON = new ExcitatoryNeuron();
    public static final INeuron MAX_NEURON = new ExcitatoryNeuron();

    private String label;

    private volatile double bias;
    private volatile double biasDelta;

    private volatile int synapseIdCounter = 0;


    TreeMap<Neuron, Synapse> outputSynapses = new TreeMap<>();
    Set<Neuron> propagateTargets = new TreeSet<>();


    ReadWriteLock lock = new ReadWriteLock();


    protected INeuron() {
    }


    public INeuron(Neuron p) {
        provider = p;
    }


    public INeuron(Model m, String label) {
        this.label = label;

        provider = new Neuron(m, this);

        setModified();
    }


    public abstract Fired incrementFired(Fired f);


    public abstract boolean isWeak(Synapse synapse, Synapse.State state);


    public Integer getId() {
        return provider.getId();
    }

    public String getLabel() {
        return label;
    }


    public Collection<Synapse> getOutputSynapses() {
        return outputSynapses.values();
    }


    public abstract ActivationFunction getActivationFunction();



    public Model getModel() {
        return provider.getModel();
    }



    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param input
     */
    public Activation addInput(Document doc, Activation.Builder input) {
        Fired f = new Fired(input.inputTimestamp, input.fired);

        Activation act = new Activation(doc, this, input.value, f);
        act.isFinal = true;

        for(Activation iAct: input.getInputLinks()) {
            act.addLink(new Link(null, iAct, act));
        }

        propagate(act);

        doc.getQueue().process();

        return act;
    }


    public abstract void addInputSynapse(S s);

    public abstract void addOutputSynapse(Synapse synapse);

    public abstract void commit(Collection<? extends Synapse> modifiedSynapses);


    public void commitBias() {
        bias += biasDelta;
        biasDelta = 0.0;
    }


    public synchronized int getNewSynapseId() {
        setModified();
        return synapseIdCounter++;
    }


    public synchronized void registerSynapseId(Integer synId) {
        if(synId >= synapseIdCounter) {
            setModified();
            synapseIdCounter = synId + 1;
        }
    }


    public void addPropagateTarget(Neuron target) {
        propagateTargets.add(target);
    }


    public void removePropagateTarget(Neuron target) {
        propagateTargets.remove(target);
    }


    public void propagate(Activation act) {
        propagateTargets.forEach(n -> n.get());

        propagateTargets
                .stream()
                .map(n -> provider.activeOutputSynapses.get(n))
                .forEach(s -> s.getOutput().propagate(act, s));
    }


    protected void propagate(Activation iAct, Synapse s) {
        Document doc = iAct.getDocument();
        Activation oAct = new Activation(doc, this, iAct.round);

        doc.getLinker().add(new Link(s, iAct, oAct));

        doc.getLinker().process();
    }


    public int compareTo(INeuron n) {
        if (this == n) return 0;
        if (this == MIN_NEURON) return -1;
        if (n == MIN_NEURON) return 1;
        if (this == MAX_NEURON) return 1;
        if (n == MAX_NEURON) return -1;

        return Integer.compare(getId(), n.getId());
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(getType());

        out.writeBoolean(label != null);
        if(label != null) {
            out.writeUTF(label);
        }

        out.writeDouble(bias);

        out.writeInt(synapseIdCounter);

        for (Synapse s : outputSynapses.values()) {
            if (s.getOutput() != null) {
                out.writeBoolean(true);
                getModel().writeSynapse(s, out);
            }
        }
        out.writeBoolean(false);
    }


    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        if(in.readBoolean()) {
            label = in.readUTF();
        }

        bias = in.readDouble();

        synapseIdCounter = in.readInt();

        while (in.readBoolean()) {
            Synapse syn = m.readSynapse(in);
            outputSynapses.put(syn.getPOutput(), syn);
        }
    }


    @Override
    public void suspend() {
        for (Synapse s : outputSynapses.values()) {
            s.getPOutput().removeActiveInputSynapse(s);
        }
    }


    @Override
    public void reactivate() {
    }


    public void setBias(double b) {
        biasDelta = b - bias;
    }


    public void updateBiasDelta(double biasDelta) {
        this.biasDelta += biasDelta;
    }


    public abstract double getTotalBias(Synapse.State state);


    public double getBias() {
        return bias;
    }


    protected double getBias(Synapse.State state) {
        return state == CURRENT ? bias : bias + biasDelta;
    }


    public abstract String getType();


    public String toString() {
        return label;
    }


    protected String toDetailedString() {
        return typeToString() + " " + label + " B:" + Utils.round(bias);
    }


    public abstract String typeToString();


    public String toStringWithSynapses() {
        SortedSet<Synapse> is = new TreeSet<>(Comparator.comparing(Synapse::getPInput));

        is.addAll(getProvider().getActiveInputSynapses());

        StringBuilder sb = new StringBuilder();
        sb.append(toDetailedString());
        sb.append("\n");
        for (Synapse s : is) {
            sb.append("  ");
            sb.append(s.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public abstract void removeInputSynapse(S s);

    public abstract void removeOutputSynapse(Synapse s);
}
