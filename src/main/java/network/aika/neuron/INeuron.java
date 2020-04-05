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
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.linker.Linker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.function.Function;

import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class INeuron<S extends Synapse> extends AbstractNode<Neuron> implements Comparable<INeuron> {

    private static final Logger log = LoggerFactory.getLogger(INeuron.class);

    private String label;

    private volatile double bias;
    private volatile double biasDelta;

    protected TreeMap<Neuron, Synapse> outputSynapses = new TreeMap<>();
    protected Set<Neuron> propagateTargets = new TreeSet<>();

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

    public abstract ActivationFunction getActivationFunction();

    public abstract Fired incrementFired(Fired f);

    public abstract boolean isWeak(Synapse synapse, Synapse.State state);

    public abstract void addInputSynapse(S s);

    public abstract void removeInputSynapse(S s);

    public abstract void removeOutputSynapse(Synapse s);

    public abstract void addOutputSynapse(Synapse synapse);

    public abstract void commit(Collection<? extends Synapse> modifiedSynapses);

    public abstract byte getType();

    public Integer getId() {
        return provider.getId();
    }

    public String getLabel() {
        return label;
    }

    public Collection<? extends Synapse> getSynapses(Direction dir) {
        return dir == INPUT ? getProvider().getActiveInputSynapses() : getProvider().getActiveOutputSynapses();
    }

    public Collection<Synapse> getOutputSynapses() {
        return outputSynapses.values();
    }

    public Model getModel() {
        return provider.getModel();
    }

    public Set<Neuron> getPropagationTargets() {
        return propagateTargets;
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param input
     */
    public Activation addInput(Document doc, Activation.Builder input) {
        Activation act = new Activation(doc, this, false, null, 0);

        for(Activation iAct: input.getInputLinks()) {
            Synapse s = getProvider().getInputSynapse(iAct.getNeuron());
            act.addLink(new Link(s, iAct, null), false);
        }

        act.setValue(input.value);
        act.setFired(new Fired(input.inputTimestamp, input.fired));
        act.setRangeCoverage(input.rangeCoverage);

        act.isFinal = true;

        doc.getLinker().linkForward(act, false);
        doc.getQueue().process(false);

        return act;
    }

    public void commitBias() {
        bias += biasDelta;
        biasDelta = 0.0;
    }

    public void addPropagateTarget(Neuron target) {
        propagateTargets.add(target);
    }

    public void removePropagateTarget(Neuron target) {
        propagateTargets.remove(target);
    }

    public void setBias(double b) {
        biasDelta = b - bias;
    }

    public void updateBiasDelta(double biasDelta) {
        this.biasDelta += biasDelta;
    }

    public abstract double getTotalBias(boolean initialRound, Synapse.State state);

    public double getBias() {
        return bias;
    }

    protected double getBias(Synapse.State state) {
        return state == CURRENT ? bias : bias + biasDelta;
    }

    public double getCost(Sign s) {
        return 0.0;
    }

    public double computeGradient(Link l, int depth, Function<Link, Double> f) {
        return 0.0;
    }

    public abstract double propagateRangeCoverage(Activation iAct);

    public abstract void collectLinkingCandidates(Activation act, Direction dir, Linker.CollectResults c);

    public int compareTo(INeuron n) {
        if (this == n) return 0;
        return Integer.compare(getId(), n.getId());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());

        out.writeBoolean(label != null);
        if(label != null) {
            out.writeUTF(label);
        }

        out.writeDouble(bias);

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

        while (in.readBoolean()) {
            Synapse syn = m.readSynapse(in);
            outputSynapses.put(syn.getPOutput(), syn);
        }
    }

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


    public void collectPPSameInputLinkingCandidatesUp(Activation act, Linker.CollectResults c) {
    }

    public void collectPPRelatedInputRPLinkingCandidatesDown(Activation act, Linker.CollectResults c) {
    }
}
