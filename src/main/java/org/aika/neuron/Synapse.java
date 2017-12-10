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
package org.aika.neuron;


import org.aika.*;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.lattice.InputNode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

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

    public static final Comparator<Synapse> INPUT_SYNAPSE_BY_WEIGHTS_COMP = new Comparator<Synapse>() {
        @Override
        public int compare(Synapse s1, Synapse s2) {
            int r = compareWeights(s1.weight, s2.weight, 0.00001);
            if (r != 0) return r;
            r = s1.input.compareTo(s2.input);
            if (r != 0) return r;
            return s1.key.compareTo(s2.key);
        }
    };


    public static final Comparator<Synapse> INPUT_SYNAPSE_COMP = new Comparator<Synapse>() {
        @Override
        public int compare(Synapse s1, Synapse s2) {
            int r = s1.input.compareTo(s2.input);
            if (r != 0) return r;
            return s1.key.compareTo(s2.key);
        }
    };


    public static final Comparator<Synapse> OUTPUT_SYNAPSE_COMP = new Comparator<Synapse>() {
        @Override
        public int compare(Synapse s1, Synapse s2) {
            int r = s1.output.compareTo(s2.output);
            if (r != 0) return r;
            return s1.key.compareTo(s2.key);
        }
    };

    public Neuron input;
    public Neuron output;

    public Provider<InputNode> inputNode;

    public Key key;

    /**
     * The weight of this synapse.
     */
    public float weight;

    /**
     * The new weight of this synapse. The converter will use it to compute few internal
     * parameters and then transfer it to the weight variable.
     */
    public float newWeight;

    /**
     * The synapse is stored either in the input neuron or the output neuron
     * depending on whether it is a conjunctive or disjunctive synapse.
     */
    public boolean isConjunction;

    public Synapse() {}


    public Synapse(Model m, Neuron input, Neuron output) {
        this.input = input;
        this.output = output;
    }


    public Synapse(Model m, Neuron input, Neuron output, Key key) {
        this(m, input, output);
        this.key = lookupKey(key);
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
        out.provider.lock.releaseWriteLock();

        if(isConjunction(false)) {
            if(out.inputSynapses.remove(this) != null) {
                out.setModified();
            }
        } else {
            if(in.outputSynapses.remove(this) != null) {
                in.setModified();
            }
        }
        if(isConjunction(true)) {
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


    public boolean isConjunction(boolean v) {
        INeuron out = output.get();
        return (v ? newWeight : weight) + out.bias + out.posRecSum - out.negRecSum - out.negDirSum <= 0.0;
    }


    public static int compareWeights(Float a, Float b, double tolerance) {
        double aAbs = Math.abs(a);
        double bAbs = Math.abs(b);
        if(aAbs + tolerance < bAbs) return -1;
        if(aAbs > bAbs + tolerance) return 1;
        return 0;
    }


    public boolean isNegative() {
        return weight <= 0.0;
    }


    public String toString() {
        return "S " + weight + " " + key.relativeRid + " S:" + key.startRangeMapping + " E:" + key.endRangeMapping + " " +  input + "->" + output;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(input.id);
        out.writeInt(output.id);
        out.writeInt(inputNode.id);

        key.write(out);

        out.writeFloat(weight);

        out.writeBoolean(isConjunction);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readInt());
        output = m.lookupNeuron(in.readInt());
        inputNode = m.lookupNodeProvider(in.readInt());

        key = lookupKey(Key.read(in, m));

        newWeight = weight = in.readFloat();

        isConjunction = in.readBoolean();
    }


    public static Synapse read(DataInput in, Model m) throws IOException {
        Synapse s = new Synapse();
        s.readFields(in, m);
        return s;
    }


    static Map<Key, Key> keyMap = new TreeMap<>();

    public static Key lookupKey(Key k) {
        Key rk = keyMap.get(k);
        if(rk == null) {
            keyMap.put(k, k);
            rk = k;
        }
        return rk;
    }



    public static class Key implements Comparable<Key>, Writable {
        public static final Key MIN_KEY = new Key();
        public static final Key MAX_KEY = new Key();

        public boolean isRecurrent;
        public Integer relativeRid;
        public Integer absoluteRid;
        public Operator startRangeMatch;
        public Operator endRangeMatch;
        public Mapping startRangeMapping;
        public Mapping endRangeMapping;
        public boolean startRangeOutput;
        public boolean endRangeOutput;

        public Key() {}


        public Key(boolean isRecurrent, Integer relativeRid, Integer absoluteRid, Operator startRangeMatch, Mapping startRangeMapping, boolean startRangeOutput, Operator endRangeMatch, Mapping endRangeMapping, boolean endRangeOutput) {
            this.isRecurrent = isRecurrent;
            this.relativeRid = relativeRid;
            this.absoluteRid = absoluteRid;
            this.startRangeMatch = startRangeMatch;
            this.endRangeMatch = endRangeMatch;
            this.startRangeMapping = startRangeMapping;
            this.endRangeMapping = endRangeMapping;
            this.startRangeOutput = startRangeOutput;
            this.endRangeOutput = endRangeOutput;
        }


        public Key createInputNodeKey() {
            return relativeRid != null ?
                    new Key(
                            isRecurrent,
                            0,
                            absoluteRid,
                            startRangeMatch,
                            startRangeMapping,
                            startRangeOutput,
                            endRangeMatch,
                            endRangeMapping,
                            endRangeOutput
                    ) : this;
        }


        @Override
        public void write(DataOutput out) throws IOException {
            out.writeBoolean(isRecurrent);
            out.writeBoolean(relativeRid != null);
            if(relativeRid != null) out.writeByte(relativeRid);
            out.writeBoolean(absoluteRid != null);
            if(absoluteRid != null) out.writeByte(absoluteRid);
            out.writeByte(startRangeMatch.getId());
            out.writeByte(endRangeMatch.getId());
            out.writeByte(startRangeMapping.getId());
            out.writeByte(endRangeMapping.getId());
            out.writeBoolean(startRangeOutput);
            out.writeBoolean(endRangeOutput);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            isRecurrent = in.readBoolean();
            if(in.readBoolean()) relativeRid = (int) in.readByte();
            if(in.readBoolean()) absoluteRid = (int) in.readByte();
            startRangeMatch = Operator.getById(in.readByte());
            endRangeMatch = Operator.getById(in.readByte());
            startRangeMapping = Mapping.getById(in.readByte());
            endRangeMapping = Mapping.getById(in.readByte());
            startRangeOutput = in.readBoolean();
            endRangeOutput = in.readBoolean();
        }


        public static Key read(DataInput in, Model m) throws IOException {
            Key k = new Key();
            k.readFields(in, m);
            return k;
        }


        @Override
        public int compareTo(Key k) {
            if(this == k) return 0;

            if(this == MIN_KEY && k != MIN_KEY) return -1;
            else if(this != MIN_KEY && k == MIN_KEY) return 1;
            if(this == MAX_KEY && k != MAX_KEY) return 1;
            else if(this != MAX_KEY && k == MAX_KEY) return -1;

            int r = Boolean.compare(isRecurrent, k.isRecurrent);
            if(r != 0) return r;
            r = Utils.compareInteger(relativeRid, k.relativeRid);
            if(r != 0) return r;
            r = Utils.compareInteger(absoluteRid, k.absoluteRid);
            if(r != 0) return r;
            r = startRangeMatch.compareTo(k.startRangeMatch);
            if(r != 0) return r;
            r = endRangeMatch.compareTo(k.endRangeMatch);
            if(r != 0) return r;
            r = startRangeMapping.compareTo(k.startRangeMapping);
            if(r != 0) return r;
            r = endRangeMapping.compareTo(k.endRangeMapping);
            if(r != 0) return r;
            r = Boolean.compare(startRangeOutput, k.startRangeOutput);
            if(r != 0) return r;
            return Boolean.compare(endRangeOutput, k.endRangeOutput);
        }
    }
}
