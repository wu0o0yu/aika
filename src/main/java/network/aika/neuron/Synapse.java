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
import network.aika.neuron.relation.Relation;
import network.aika.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

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
public class Synapse implements Writable {

    public static final int OUTPUT = -1;
    public static final int VARIABLE = -2;

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



    public Neuron input;
    public Neuron output;

    public Integer id;

    public boolean isRecurrent;
    public boolean identity;

    public Map<Integer, Relation> relations = new TreeMap<>();

    public DistanceFunction distanceFunction = null;

    public Writable extension;


    public boolean inactive;

    /**
     * The weight of this synapse.
     */
    public double weight;

    /**
     * The weight delta of this synapse. The converter will use it to compute few internal
     * parameters and then createOrLookup the weight variable.
     */
    public double weightDelta;


    public double bias;

    public double biasDelta;

    public double limit;

    public double limitDelta;

    public boolean toBeDeleted;

    /**
     * The synapse is stored either in the input neuron or the output neuron
     * depending on whether it is a conjunctive or disjunctive synapse.
     */
    public boolean isConjunction;

    public int createdInDoc;
    public int committedInDoc;

    public Synapse() {
        relations = new TreeMap<>();
    }


    public Synapse(Neuron input, Neuron output, Integer id) {
        this.id = id;
        this.input = input;
        this.output = output;

        if(output.model.getSynapseExtensionFactory() != null) {
            extension = output.model.getSynapseExtensionFactory().createObject();
        }
    }


    public void link() {
        INeuron in = input.get();
        INeuron out = output.get();

        boolean dir = in.provider.id < out.provider.id;

        (dir ? in : out).lock.acquireWriteLock();
        (dir ? out : in).lock.acquireWriteLock();

        in.provider.lock.acquireWriteLock();
        in.provider.inMemoryOutputSynapses.put(this, this);
        in.provider.lock.releaseWriteLock();

        out.provider.lock.acquireWriteLock();
        out.provider.inMemoryInputSynapses.put(this, this);
        out.provider.inputSynapsesById.put(id, this);
        out.provider.lock.releaseWriteLock();

        removeLinkInternal(in, out);

        if(isConjunction(true, false)) {
            out.inputSynapses.put(this, this);
            isConjunction = true;
            out.setModified();
        } else {
            in.outputSynapses.put(this, this);
            isConjunction = false;
            in.setModified();
        }

        out.registerSynapseId(id);

        if(in.isPassiveInputNeuron()) {
            out.registerPassiveInputSynapse(this);
        }

        (dir ? in : out).lock.releaseWriteLock();
        (dir ? out : in).lock.releaseWriteLock();
    }


    public void relink() {
        boolean newIsConjunction = isConjunction(true, false);
        if(newIsConjunction != isConjunction) {
            INeuron in = input.get();
            INeuron out = output.get();

            boolean dir = in.provider.id < out.provider.id;
            (dir ? in : out).lock.acquireWriteLock();
            (dir ? out : in).lock.acquireWriteLock();

            if (newIsConjunction) {
                out.inputSynapses.put(this, this);
                isConjunction = true;
                out.setModified();
            } else {
                in.outputSynapses.put(this, this);
                isConjunction = false;
                in.setModified();
            }

            (dir ? in : out).lock.releaseWriteLock();
            (dir ? out : in).lock.releaseWriteLock();
        }
    }


    public void unlink() {
        INeuron in = input.get();
        INeuron out = output.get();

        boolean dir = in.provider.id < out.provider.id;

        (dir ? in : out).lock.acquireWriteLock();
        (dir ? out : in).lock.acquireWriteLock();

        in.provider.lock.acquireWriteLock();
        in.provider.inMemoryOutputSynapses.remove(this);
        in.provider.lock.releaseWriteLock();

        out.provider.lock.acquireWriteLock();
        out.provider.inMemoryInputSynapses.remove(this);
        out.provider.inputSynapsesById.remove(id);
        out.provider.lock.releaseWriteLock();

        removeLinkInternal(in, out);

        (dir ? in : out).lock.releaseWriteLock();
        (dir ? out : in).lock.releaseWriteLock();
    }


    private void removeLinkInternal(INeuron in, INeuron out) {
        if(isConjunction(false, false)) {
            if(out.inputSynapses.remove(this) != null) {
                out.setModified();
                out.synapseIdCounter--;
            }
        } else {
            if(in.outputSynapses.remove(this) != null) {
                in.setModified();
                out.synapseIdCounter--;
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


    /**
     *
     * @param v
     * @param absolute if true, a disjunctive synapse needs to be able to activate the neuron by itself.
     * @return
     */
    public boolean isConjunction(boolean v, boolean absolute) {
        INeuron out = output.get();
        return (v ? (limit + limitDelta) * getNewWeight() : limit * weight) + (absolute ? 0.0 : out.requiredSum) + (v ? out.getNewBiasSum() : out.biasSum) <= 0.0;
    }


    public void updateDelta(Document doc, double weightDelta, double biasDelta, double limitDelta) {
        this.weightDelta += weightDelta;
        this.biasDelta += biasDelta;
        this.limitDelta += limitDelta;
        output.get().biasSumDelta += biasDelta;
        relink();
        if(doc != null) {
            doc.notifyWeightModified(this);
        }
    }


    public void update(Document doc, double weight, double bias, double limit) {
        this.weightDelta = weight - this.weight;
        double newBiasDelta = bias - this.bias;
        this.limitDelta = limit - this.limit;
        output.get().biasSumDelta += newBiasDelta - biasDelta;
        biasDelta = newBiasDelta;

        relink();
        if(doc != null) {
            doc.notifyWeightModified(this);
        }
    }


    public boolean isNegative() {
        return weight < 0.0;
    }


    public Relation getRelationById(Integer id) {
        return relations.get(id);
    }


    public String toString() {
        return "S NW:" + getNewWeight() + " NB:" + getNewBias() + " rec:" + isRecurrent + " " +  input + "->" + output;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(id);

        out.writeBoolean(isRecurrent);
        out.writeBoolean(identity);

        out.writeInt(input.id);
        out.writeInt(output.id);

        out.writeInt(relations.size());
        for(Map.Entry<Integer, Relation> me: relations.entrySet()) {
            out.writeInt(me.getKey());

            me.getValue().write(out);
        }

        out.writeBoolean(distanceFunction != null);
        if(distanceFunction != null) {
            out.writeUTF(distanceFunction.name());
        }

        out.writeDouble(weight);
        out.writeDouble(bias);
        out.writeDouble(limit);

        out.writeBoolean(isConjunction);

        out.writeBoolean(extension != null);
        if(extension != null) {
            extension.write(out);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        id = in.readInt();

        isRecurrent = in.readBoolean();
        identity = in.readBoolean();

        input = m.lookupNeuron(in.readInt());
        output = m.lookupNeuron(in.readInt());

        int l = in.readInt();
        for(int i = 0; i < l; i++) {
            Integer relId = in.readInt();
            relations.put(relId, Relation.read(in, m));
        }

        if(in.readBoolean()) {
            distanceFunction = DistanceFunction.valueOf(in.readUTF());
        }

        weight = in.readDouble();
        bias = in.readDouble();
        limit = in.readDouble();

        isConjunction = in.readBoolean();

        if(in.readBoolean()) {
            extension = m.getSynapseExtensionFactory().createObject();
            extension.readFields(in, m);
        }
    }


    public static Synapse read(DataInput in, Model m) throws IOException {
        Synapse s = new Synapse();
        s.readFields(in, m);
        return s;
    }



    public static Synapse createOrLookup(Document doc, Integer synapseId, Neuron inputNeuron, Neuron outputNeuron) {
        outputNeuron.get(doc);
        inputNeuron.get(doc);
        outputNeuron.lock.acquireWriteLock();
        Synapse synapse = null;
        if(synapseId != null) {
            synapse = outputNeuron.inputSynapsesById.get(synapseId);
        } else {
            Map.Entry<Synapse, Synapse> me = outputNeuron.inMemoryInputSynapses.subMap(
                    new Synapse(inputNeuron, outputNeuron, Integer.MIN_VALUE), true,
                    new Synapse(inputNeuron, outputNeuron, Integer.MAX_VALUE), true
            ).firstEntry();
            if(me != null) {
                synapse = me.getKey();
            }
        }
        outputNeuron.lock.releaseWriteLock();

        if(synapse == null) {
            synapse = new Synapse(inputNeuron, outputNeuron, synapseId);

            if(synapseId == null) {
                synapse.id = outputNeuron.get(doc).getNewSynapseId();
            }

            synapse.link();
            if(doc != null) {
                synapse.createdInDoc = doc.id;
            }
        }
        return synapse;
    }


    public double getNewWeight() {
        return weight + weightDelta;
    }


    public double getNewBias() {
        return bias + biasDelta;
    }


    public boolean[] linksOutput() {
        boolean[] result = new boolean[] {false, false};
        for(Map.Entry<Integer, Relation> me: relations.entrySet()) {
            Relation rel = me.getValue();
            if(me.getKey() == OUTPUT) {
                if (rel.linksOutputBegin()) {
                    result[0] = true;
                }
                if (rel.linksOutputEnd()) {
                    result[1] = true;
                }
            }
        }
        return result;
    }


    public boolean linksAnyOutput() {
        boolean[] lo = linksOutput();
        return lo[0] || lo[1];
    }


    /**
     * The {@code Builder} class is just a helper class which is used to initialize a neuron. Most of the parameters of this class
     * will be mapped to a input synapse for this neuron.
     *
     * @author Lukas Molzberger
     */
    public static class Builder implements Neuron.Builder {

        public boolean recurrent;
        public Neuron neuron;
        public double weight;
        public double bias;
        public double limit = 1.0;

        public DistanceFunction distanceFunction;

        public boolean identity;

        public Integer synapseId;


        /**
         * The property <code>recurrent</code> specifies if input is a recurrent feedback link. Recurrent
         * feedback links can be either negative or positive depending on the weight of the synapse. Recurrent feedback links
         * kind of allow to use future information as inputs of a current neuron. Aika allows this by making assumptions about
         * the recurrent input neuron. The class <code>SearchNode</code> modifies these assumptions until the best interpretation
         * for this document is found.
         *
         * @param recurrent
         * @return
         */
        public Builder setRecurrent(boolean recurrent) {
            this.recurrent = recurrent;
            return this;
        }


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
         * The synapse weight of this input.
         *
         * @param weight
         * @return
         */
        public Builder setWeight(double weight) {
            this.weight = weight;
            return this;
        }

        /**
         * The bias of this input that will later on be added to the neurons bias.
         *
         * @param bias
         * @return
         */
        public Builder setBias(double bias) {
            this.bias = bias;
            return this;
        }


        public Builder setLimit(double limit) {
            this.limit = limit;
            return this;
        }


        public Builder setDistanceFunction(DistanceFunction distFunc) {
            this.distanceFunction = distFunc;
            return this;
        }




        public Builder setIdentity(boolean identity) {
            this.identity = identity;
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
            Synapse s = createOrLookup(null, synapseId, neuron, outputNeuron);

            s.isRecurrent = recurrent;
            s.identity = identity;
            s.distanceFunction = distanceFunction;

            return s;
        }
    }
}
