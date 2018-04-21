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
import network.aika.neuron.activation.Linker;
import network.aika.*;
import network.aika.Document;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.Range.Output;
import network.aika.neuron.activation.Range.Mapping;
import network.aika.training.MetaSynapse;

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


    public static final Comparator<Synapse> INPUT_SYNAPSE_COMP = (s1, s2) -> {
        int r = s1.input.compareTo(s2.input);
        if (r != 0) return r;
        return s1.rangeOutput.compareTo(s2.rangeOutput);
    };


    public static final Comparator<Synapse> OUTPUT_SYNAPSE_COMP = (s1, s2) -> {
        int r = s1.output.compareTo(s2.output);
        if (r != 0) return r;
        return s1.rangeOutput.compareTo(s2.rangeOutput);
    };


    public Neuron input;
    public Neuron output;

    public int id;
    public boolean isRecurrent;
    public Output rangeOutput;


    public Map<Synapse, Relation> relations = new TreeMap<>();
    public DistanceFunction distanceFunction = null;


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

    public boolean toBeDeleted;

    /**
     * The synapse is stored either in the input neuron or the output neuron
     * depending on whether it is a conjunctive or disjunctive synapse.
     */
    public boolean isConjunction;

    public MetaSynapse meta;

    public int createdInDoc;
    public int committedInDoc;

    public Synapse() {}


    public Synapse(int synapseId, Neuron input, Neuron output) {
        this.id = synapseId;
        this.input = input;
        this.output = output;
    }


    public Synapse(int synapseId, Neuron input, Neuron output, boolean isRecurrent, Output rangeOutput) {
        this(synapseId, input, output);
        this.isRecurrent = isRecurrent;
        this.rangeOutput = rangeOutput;
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
            }
        } else {
            if(in.outputSynapses.remove(this) != null) {
                in.setModified();
            }
        }
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
        return (v ? getNewWeight() : weight) + (absolute ? 0.0 : out.requiredSum) + (v ? out.getNewBiasSum() : out.biasSum) <= 0.0;
    }


    public void updateDelta(Document doc, double weightDelta, double biasDelta) {
        this.weightDelta += weightDelta;
        this.biasDelta += biasDelta;
        output.get().biasSumDelta += biasDelta;
        relink();
        if(doc != null) {
            doc.notifyWeightModified(this);
        }
    }


    public void update(Document doc, double weight, double bias) {
        this.weightDelta = weight - this.weight;
        double newBiasDelta = bias - this.bias;
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


    public String toString() {
        return "S OW:" + weight + " NW:" + (weight + weightDelta) + " rec:" + isRecurrent + " o:" + rangeOutput + " " +  input + "->" + output;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(input.id);
        out.writeInt(output.id);

        out.writeBoolean(isRecurrent);
        rangeOutput.write(out);

        out.writeBoolean(distanceFunction != null);
        if(distanceFunction != null) {
            out.writeUTF(distanceFunction.name());
        }

        out.writeDouble(weight);
        out.writeDouble(bias);

        out.writeBoolean(isConjunction);

        out.writeBoolean(meta != null);
        if(meta != null) {
            meta.write(out);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readInt());
        output = m.lookupNeuron(in.readInt());

        isRecurrent = in.readBoolean();
        rangeOutput = Range.Output.read(in, m);

        if(in.readBoolean()) {
            distanceFunction = DistanceFunction.valueOf(in.readUTF());
        }

        weight = in.readDouble();
        bias = in.readDouble();

        isConjunction = in.readBoolean();

        if(in.readBoolean()) {
            meta = new MetaSynapse();
            meta.readFields(in, m);
        }
    }


    public static Synapse read(DataInput in, Model m) throws IOException {
        Synapse s = new Synapse();
        s.readFields(in, m);
        return s;
    }



    public static Synapse createOrLookup(Document doc, int synapseId, boolean isRecurrent, Output rangeOutput, Neuron inputNeuron, Neuron outputNeuron) {
        Synapse synapse = outputNeuron.getSynapseById(synapseId);

        if(synapse == null) {
            synapse = new Synapse(synapseId, inputNeuron, outputNeuron, isRecurrent, rangeOutput);

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



    /**
     * The {@code Builder} class is just a helper class which is used to initialize a neuron. Most of the parameters of this class
     * will be mapped to a input synapse for this neuron.
     *
     * @author Lukas Molzberger
     */
    public static class Builder implements Comparable<Builder> {
        public boolean recurrent;
        public Neuron neuron;
        public double weight;
        public double bias;

        public Range.Relation rangeMatch = Range.Relation.NONE;
        public Output rangeOutput = Output.NONE;

        public Integer relativeRid;
        public Integer absoluteRid;

        public Integer synapseId;
        public Map<Integer, Relation> relations = new TreeMap<>();


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

        /**
         * If the absolute relational id (rid) not null, then it is required to match the rid of input activation.
         *
         * @param absoluteRid
         * @return
         */
        public Builder setAbsoluteRid(Integer absoluteRid) {
            this.absoluteRid = absoluteRid;
            return this;
        }

        /**
         * The relative relational id (rid) determines the relative position of this inputs rid with respect to
         * other inputs of this neuron.
         *
         * @param relativeRid
         * @return
         */
        public Builder setRelativeRid(Integer relativeRid) {
            this.relativeRid = relativeRid;
            return this;
        }


        /**
         * <code>setRangeMatch</code> is just a convenience function to call both
         * <code>setBeginToBeginRangeMatch</code> and <code>setEndToEndRangeMatch</code> at the same time.
         *
         * @param rr
         * @return
         */
        public Builder setRangeMatch(Range.Relation rr) {
            rangeMatch = rr;
            return this;
        }


        /**
         * <code>setRangeMatch</code> is just a convenience function to call both
         * <code>setBeginToBeginRangeMatch</code> and <code>setEndToEndRangeMatch</code> at the same time.
         *
         * @param beginToBegin
         * @param endToEnd
         * @return
         */
        public Builder setRangeMatch(Range.Operator beginToBegin, Range.Operator endToEnd) {
            rangeMatch = Range.Relation.create(beginToBegin, endToEnd);
            return this;
        }


        /**
         * <code>setRangeOutput</code> is just a convenience function to call <code>setBeginRangeOutput</code> and
         * <code>setEndRangeOutput</code> at the same time.
         *
         * @param ro
         * @return
         */
        public Builder setRangeOutput(boolean ro) {
            this.rangeOutput = ro ? Output.DIRECT : Output.NONE;
            return this;
        }

        /**
         * <code>setRangeOutput</code> is just a convenience function to call <code>setBeginRangeOutput</code> and
         * <code>setEndRangeOutput</code> at the same time.
         *
         * @param begin
         * @param end
         * @return
         */
        public Builder setRangeOutput(boolean begin, boolean end) {
            return setRangeOutput(begin ? Mapping.BEGIN : Mapping.NONE, end ? Mapping.END : Mapping.NONE);
        }


        /**
         * <code>setRangeOutput</code> is just a convenience function to call <code>setBeginRangeOutput</code> and
         * <code>setEndRangeOutput</code> at the same time.
         *
         * @param rangeOutput
         * @return
         */
        public Builder setRangeOutput(Output rangeOutput) {
            this.rangeOutput = rangeOutput;
            return this;
        }


        /**
         * Determines if this input is used to compute the range start of the output activation.
         *
         * @param begin
         * @param end
         * @return
         */
        public Builder setRangeOutput(Mapping begin, Mapping end) {
            this.rangeOutput = Output.create(begin, end);
            return this;
        }


        public Builder setSynapseId(int synapseId) {
            this.synapseId = synapseId;
            return this;
        }


        public Builder addInstanceRelation(Relation.InstanceRelation.Type type, int synapseId) {
            relations.put(synapseId, new Relation.InstanceRelation(type));
            return this;
        }


        public Builder addRangeRelation(Range.Relation relation, int synapseId) {
            relations.put(synapseId, new Relation.RangeRelation(relation));
            return this;
        }


        public Synapse getSynapse(Neuron outputNeuron) {
            return createOrLookup(null, synapseId, recurrent, rangeOutput, neuron, outputNeuron);
        }


        @Override
        public int compareTo(Builder in) {
            int r = neuron.compareTo(in.neuron);
            if(r != 0) return r;
            r = rangeMatch.compareTo(in.rangeMatch);
            if(r != 0) return r;
            r = Utils.compareInteger(relativeRid, in.relativeRid);
            if (r != 0) return r;
            r = Utils.compareInteger(absoluteRid, in.absoluteRid);
            if (r != 0) return r;
            r = rangeOutput.compareTo(in.rangeOutput);
            return r;
        }
    }
}
