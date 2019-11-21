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
import network.aika.Document;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.link.Link;
import network.aika.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.Synapse.State.NEXT;

/**
 * The {@code Synapse} class connects two neurons with each other. When propagating an activation signal, the
 * weight of the synapse is multiplied with the activation value of the input neurons activation. The result is then added
 * to the output neurons weighted sum to compute the output activation value. In contrast to a conventional neural network
 * synapses in Aika do not just propagate the activation value from one neuron to the next, but also structural information
 * like the text range and the relational id (e.g. word position) and also the interpretation to which the input
 * activation belongs. To determine in which way the structural information should be propagated synapses in Aika possess
 * a few more properties.
 *
 * <p>The properties {@code relativeRid} and {@code absoluteRid} determine either the relative difference between
 * the {@code rid} of the input activation and the rid of the output activation or require a fixed rid as input.
 *
 * <p>The properties range match, range mapping and range output manipulate the ranges. The range match determines whether
 * the input range begin or end is required to be equal, greater than or less than the range of the output activation.
 * The range mapping can be used to map for example an input range end to an output range begin. Usually this simply maps
 * begin to begin and end to end. The range output property is a boolean flag which determines whether the input range
 * should be propagated to the output activation.
 *
 * <p>Furthermore, the property {@code isRecurrent} specifies if this synapse is a recurrent feedback link. Recurrent
 * feedback links can be either negative or positive depending on the weight of the synapse. Recurrent feedback links
 * kind of allow to use future information as inputs of a current neuron. Aika allows this by making assumptions about
 * the recurrent input neuron. The class {@code SearchNode} modifies these assumptions until the best interpretation
 * for this document is found.
 *
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse implements Writable {

    public static final int OUTPUT = -1;

    public static double TOLERANCE = 0.0000001;


    public static final Comparator<Synapse> INPUT_SYNAPSE_COMP = (s1, s2) -> {
        int r = s1.input.compareTo(s2.input);
        if (r != 0) return r;
        return Integer.compare(s1.id, s2.id);
    };


    public static final Comparator<Synapse> OUTPUT_SYNAPSE_COMP = (s1, s2) -> {
        int r = s1.output.compareTo(s2.output);
        if (r != 0) return r;
        return Integer.compare(s1.id, s2.id);
    };

    private Neuron input;
    private Neuron output;

    private Integer id;

    private boolean inactive;
    private boolean inactiveNew;

    private double weight;
    private double weightDelta;

    private double limit = 1.0;
    private double limitDelta;


    public Synapse() {
    }


    public Synapse(Neuron input, Neuron output, Integer id) {
        this.id = id;
        this.input = input;
        this.output = output;
    }


    public abstract boolean storeOnInputSide();

    public abstract boolean storeOOutputSide();


    public Neuron getInput() {
        return input;
    }

    public Neuron getOutput() {
        return output;
    }

    public Integer getId() {
        return id;
    }


    public Collection<Activation> getActivations(Activation outputAct) {
        return outputAct.getInputLinks()
                .filter(l -> l.getSynapse() == this)
                .map(l -> l.getInput())
                .collect(Collectors.toList());
    }


    public boolean isInactive() {
        return inactive;
    }

    public boolean isInactive(State s) {
        return s == CURRENT ? inactive : inactiveNew;
    }

    public void setInactive(State s, boolean inactive) {
        if(s == CURRENT) {
            this.inactive = inactive;
        } else if(s == NEXT) {
            this.inactiveNew = inactive;
        }
    }

    public double getWeight() {
        return weight;
    }

    public double getLimit() {
        return limit;
    }

    public double getNewWeight() {
        return weight + weightDelta;
    }

    public double getNewLimit() {
        return limit + limitDelta;
    }

    public double getWeight(State s) {
        return s == CURRENT ? weight : getNewWeight();
    }

    public double getLimit(State s) {
        return s == CURRENT ? limit : getNewLimit();
    }

    public double getWeightDelta() {
        return weightDelta;
    }

    public double getLimitDelta() {
        return limitDelta;
    }

    public void link() {
        INeuron in = input.get();
        INeuron out = output.get();

        boolean dir = in.getId() < out.getId();

        (dir ? in : out).lock.acquireWriteLock();
        (dir ? out : in).lock.acquireWriteLock();

        input.lock.acquireWriteLock();
        input.activeOutputSynapses.put(this, this);
        input.lock.releaseWriteLock();

        output.lock.acquireWriteLock();
        output.activeInputSynapses.put(this, this);
        output.inputSynapsesById.put(id, this);
        output.lock.releaseWriteLock();

        removeLinkInternal(in, out);

        if(storeOnInputSide()) {
            out.inputSynapses.put(this, this);
            out.setModified();
        }

        if(storeOOutputSide()) {
            in.outputSynapses.put(this, this);
            in.setModified();
        }

        out.registerSynapseId(id);

        (dir ? in : out).lock.releaseWriteLock();
        (dir ? out : in).lock.releaseWriteLock();
    }


    public void unlink() {
        INeuron in = input.get();
        INeuron out = output.get();

        boolean dir = input.getId() < out.getId();

        (dir ? in : out).lock.acquireWriteLock();
        (dir ? out : in).lock.acquireWriteLock();

        input.lock.acquireWriteLock();
        input.activeOutputSynapses.remove(this);
        input.lock.releaseWriteLock();

        output.lock.acquireWriteLock();
        output.activeInputSynapses.remove(this);
        output.inputSynapsesById.remove(id);
        output.lock.releaseWriteLock();

        removeLinkInternal(in, out);

        (dir ? in : out).lock.releaseWriteLock();
        (dir ? out : in).lock.releaseWriteLock();
    }


    private void removeLinkInternal(INeuron in, INeuron out) {
        if(storeOnInputSide()) {
            if(out.inputSynapses.remove(this) != null) {
                out.setModified();
            }
        }
        if(storeOOutputSide()) {
            if(in.outputSynapses.remove(this) != null) {
                in.setModified();
            }
        }
    }


    public double getMaxInputValue() {
        return limit * weight;
    }


    public boolean exists() {
        if(input.get().outputSynapses.containsKey(this)) return true;
        if(output.get().inputSynapses.containsKey(this)) return true;
        return false;
    }


    public void commit() {
        weight += weightDelta;
        weightDelta = 0.0;

        limit += limitDelta;
        limitDelta = 0.0;

        inactive = inactiveNew;
    }


    public boolean isZero() {
        return Math.abs(weight) < TOLERANCE;
    }

    public double computeRelationWeights(Link l) {
        return 0;
    }

    public double computeMaxRelationWeights() {
        return 0;
    }


    public enum State {
        NEXT,
        CURRENT
    }


    public boolean isWeak(State state) {
        return output.get().isWeak(this, state);
    }


    public void updateDelta(Document doc, double weightDelta, double limitDelta) {
        this.weightDelta += weightDelta;
        this.limitDelta += limitDelta;

        if(doc != null) {
            doc.notifyWeightModified(this);
        }
    }


    public void update(Document doc, double weight, double limit) {
        this.weightDelta = weight - this.weight;
        this.limitDelta = limit - this.limit;

        if(doc != null) {
            doc.notifyWeightModified(this);
        }
    }


    public boolean isNegative(State s) {
        return getWeight(s) < 0.0;
    }


    public boolean isRecurrent() {
        return input.isRecurrent(isNegative(CURRENT));
    }


    public String toString() {
        return "S ID:" + id + " NW:" + Utils.round(getNewWeight()) + " " + input + "->" + output;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(id);

        out.writeInt(input.getId());
        out.writeInt(output.getId());

        out.writeDouble(weight);
        out.writeDouble(limit);

        out.writeBoolean(inactive);
    }



    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        id = in.readInt();

        input = m.lookupNeuron(in.readInt());
        output = m.lookupNeuron(in.readInt());

        weight = in.readDouble();
        limit = in.readDouble();

        inactive = in.readBoolean();
    }



    public static Synapse createOrReplace(Document doc, Integer synapseId, Neuron inputNeuron, Neuron outputNeuron, SynapseFactory synapseFactory) {
        outputNeuron.get(doc);
        inputNeuron.get(doc);
        outputNeuron.lock.acquireWriteLock();
        Synapse synapse = outputNeuron.inputSynapsesById.get(synapseId);
        if(synapse != null) {
            synapse.unlink();
        }
        outputNeuron.lock.releaseWriteLock();

        if(synapse == null) {
            synapse = synapseFactory.createSynapse(inputNeuron, outputNeuron, synapseId);
        } else {
            synapse.input = inputNeuron;
            synapse.output = outputNeuron;
        }

        if (synapseId == null) {
            synapse.id = outputNeuron.get(doc).getNewSynapseId();
        }

        synapse.link();

        return synapse;
    }



    /**
     * The {@code Builder} class is just a helper class which is used to initialize a neuron. Most of the parameters of this class
     * will be mapped to a input synapse for this neuron.
     *
     * @author Lukas Molzberger
     */
    public static abstract class Builder implements Neuron.Builder {

        private Neuron neuron;
        double weight;
        double limit = 1.0;
        private Integer synapseId;


        /**
         * Determines the input neuron.
         *
         * @param neuron
         * @return
         */
        public Builder setNeuron(Neuron neuron) {
            assert neuron != null;
            this.neuron = neuron;
            return this;
        }

        /**
         * Determines the input neuron.
         *
         * @param neuron
         * @return
         */
        public Builder setNeuron(INeuron<?, ?> neuron) {
            assert neuron != null;
            this.neuron = neuron.getProvider();
            return this;
        }


        /**
         * The synapse weight of this input.
         *
         * @param weight
         * @return
         */
        public Builder setWeight(double weight) {
            this.weight = weight;
            return this;
        }


        public Builder setLimit(double limit) {
            this.limit = limit;
            return this;
        }


        public Builder setSynapseId(int synapseId) {
            assert synapseId >= 0;
            this.synapseId = synapseId;
            return this;
        }


        @Override
        public void registerSynapseIds(Neuron n) {
            n.registerSynapseId(synapseId);
        }


        public Synapse getSynapse(Neuron outputNeuron) {
            Synapse s = createOrReplace(null, synapseId, neuron, outputNeuron, getSynapseFactory());
            return s;
        }


        protected abstract SynapseFactory getSynapseFactory();
    }

    public interface SynapseFactory {
        Synapse createSynapse(Neuron input, Neuron output, Integer id);
    }
}
