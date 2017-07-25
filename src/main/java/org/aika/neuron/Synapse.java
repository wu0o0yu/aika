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


import org.aika.Utils;
import org.aika.Writable;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Relation;
import org.aika.lattice.InputNode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;

/**
 *
 * @author Lukas Molzberger
 */
public class Synapse implements Writable {

    public static final Comparator<Synapse> INPUT_SYNAPSE_BY_WEIGHTS_COMP = new Comparator<Synapse>() {
        @Override
        public int compare(Synapse s1, Synapse s2) {
            int r = compareWeights(s1.w, s2.w, 0.00001);
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

    public InputNode inputNode;

    public Key key;

    public double w;

    /*
    * Max expected sum of neuron input synapses with a weight smaller or equal than this synapse. Used to
    * determine which nodes need to be generated based on this synapse.
    */
    public double maxLowerWeightsSum = Double.MAX_VALUE;


    public Synapse() {}


    public Synapse(Neuron input) {
        this.input = input;
    }


    public Synapse(Neuron input, Key key) {
        this.input = input;
        this.key = key;

        assert (w >= 0.0 && !key.isNeg) || (w <= 0.0 && key.isNeg);
    }


    public void link(Document doc) {
        boolean dir = input.id < output.id;

        (dir ? input : output).lock.acquireWriteLock(doc.threadId);
        (dir ? output : input).lock.acquireWriteLock(doc.threadId);

        input.outputSynapses.add(this);
        output.inputSynapses.add(this);
        output.inputSynapsesByWeight.add(this);

        (dir ? input : output).lock.releaseWriteLock();
        (dir ? output : input).lock.releaseWriteLock();

        if(output.m != null) {
            output.m.stat.synapses++;
        }
    }


    public static int compareWeights(Double a, Double b, double tolerance) {
        double aAbs = Math.abs(a);
        double bAbs = Math.abs(b);
        if(aAbs + tolerance < bAbs) return -1;
        if(aAbs > bAbs + tolerance) return 1;
        return 0;
    }


    public String toString() {
        return "S " + w + " " + key.relativeRid + " S:" + key.startSignal + " E:" + key.endSignal + " " +  input + "->" + output;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(input.id);
        out.writeInt(output.id);

        out.writeInt(inputNode.id);

        key.write(out);

        out.writeDouble(w);
        out.writeDouble(maxLowerWeightsSum);
    }


    @Override
    public void readFields(DataInput in, Document doc) throws IOException {
        input = doc.m.neurons.get(in.readInt());
        output = doc.m.neurons.get(in.readInt());
        inputNode = (InputNode) doc.m.initialNodes.get(in.readInt());

        key = Key.read(in, doc);

        w = in.readDouble();
        maxLowerWeightsSum = in.readDouble();

        input.outputSynapses.add(this);
        inputNode.setSynapse(doc, new InputNode.SynapseKey(key.relativeRid, output), this);
    }


    public static Synapse read(DataInput in, Document doc) throws IOException {
        Synapse k = new Synapse();
        k.readFields(in, doc);
        return k;
    }


    public enum RangeMatch {
        EQUALS,
        LESS_THAN,
        GREATER_THAN,
        NONE;

        public boolean compare(Integer a, Integer b) {
            switch(this) {
                case EQUALS:
                    return a == b;
                case LESS_THAN:
                    return a <= b;
                case GREATER_THAN:
                    return a >= b;
                default:
                    return true;
            }
        }

        public static RangeMatch invert(RangeMatch rm) {
            switch(rm) {
                case EQUALS:
                    return EQUALS;
                case LESS_THAN:
                    return GREATER_THAN;
                case GREATER_THAN:
                    return LESS_THAN;
                default:
                    return NONE;
            }
        }
    }



    public enum RangeSignal {
        START,
        END,
        NONE;

        public Integer getSignalPos(Range r) {
            switch(this) {
                case START:
                    return r.begin;
                case END:
                    return r.end;
                case NONE:
                default:
                    return null;
            }
        }

        public String toString() {
            switch (this) {
                case START:
                    return "S";
                case END:
                    return "E";
                case NONE:
                    return "N";
            }
            return "";
        }
    }



    public static class Key implements Comparable<Key>, Writable {
        public static final Key MIN_KEY = new Key();
        public static final Key MAX_KEY = new Key();

        public boolean isNeg;
        public boolean isRecurrent;
        public Integer relativeRid;
        public Integer absoluteRid;
        public RangeMatch startRangeMatch;
        public RangeMatch endRangeMatch;
        public RangeSignal startSignal;
        public RangeSignal endSignal;
        public boolean startRangeOutput;
        public boolean endRangeOutput;

        public Key() {}


        public Key(boolean isNeg, boolean isRecurrent, Integer relativeRid, Integer absoluteRid, RangeMatch startRangeMatch, RangeSignal startSignal, boolean startRangeOutput, RangeMatch endRangeMatch, RangeSignal endSignal, boolean endRangeOutput) {
            this.isNeg = isNeg;
            this.isRecurrent = isRecurrent;
            this.relativeRid = relativeRid;
            this.absoluteRid = absoluteRid;
            this.startRangeMatch = startRangeMatch;
            this.endRangeMatch = endRangeMatch;
            this.startSignal = startSignal;
            this.endSignal = endSignal;
            this.startRangeOutput = startRangeOutput;
            this.endRangeOutput = endRangeOutput;
        }


        public Key createInputNodeKey() {
            return relativeRid != null ? new Key(isNeg, isRecurrent, 0, absoluteRid, startRangeMatch, startSignal, startRangeOutput, endRangeMatch, endSignal, endRangeOutput) : this;
        }


        @Override
        public void write(DataOutput out) throws IOException {
            out.writeBoolean(isNeg);
            out.writeBoolean(isRecurrent);
            out.writeBoolean(relativeRid != null);
            if(relativeRid != null) out.writeInt(relativeRid);
            out.writeBoolean(absoluteRid != null);
            if(absoluteRid != null) out.writeInt(absoluteRid);
            out.writeUTF(startRangeMatch.name());
            out.writeUTF(endRangeMatch.name());
            out.writeUTF(startSignal.name());
            out.writeUTF(endSignal.name());
            out.writeBoolean(startRangeOutput);
            out.writeBoolean(endRangeOutput);
        }


        @Override
        public void readFields(DataInput in, Document doc) throws IOException {
            isNeg = in.readBoolean();
            isRecurrent = in.readBoolean();
            if(in.readBoolean()) relativeRid = in.readInt();
            if(in.readBoolean()) absoluteRid = in.readInt();
            startRangeMatch = RangeMatch.valueOf(in.readUTF());
            endRangeMatch = RangeMatch.valueOf(in.readUTF());
            startSignal = RangeSignal.valueOf(in.readUTF());
            endSignal = RangeSignal.valueOf(in.readUTF());
            startRangeOutput = in.readBoolean();
            endRangeOutput = in.readBoolean();
        }


        public static Key read(DataInput in, Document doc) throws IOException {
            Key k = new Key();
            k.readFields(in, doc);
            return k;
        }


        @Override
        public int compareTo(Key k) {
            if(this == MIN_KEY && k != MIN_KEY) return -1;
            else if(this != MIN_KEY && k == MIN_KEY) return 1;
            if(this == MAX_KEY && k != MAX_KEY) return 1;
            else if(this != MAX_KEY && k == MAX_KEY) return -1;

            int r = Boolean.compare(isNeg, k.isNeg);
            if(r != 0) return r;
            r = Boolean.compare(isRecurrent, k.isRecurrent);
            if(r != 0) return r;
            r = Utils.compareInteger(relativeRid, k.relativeRid);
            if(r != 0) return r;
            r = Utils.compareInteger(absoluteRid, k.absoluteRid);
            if(r != 0) return r;
            r = startRangeMatch.compareTo(k.startRangeMatch);
            if(r != 0) return r;
            r = endRangeMatch.compareTo(k.endRangeMatch);
            if(r != 0) return r;
            r = startSignal.compareTo(k.startSignal);
            if(r != 0) return r;
            r = endSignal.compareTo(k.endSignal);
            if(r != 0) return r;
            r = Boolean.compare(startRangeOutput, k.startRangeOutput);
            if(r != 0) return r;
            return Boolean.compare(endRangeOutput, k.endRangeOutput);
        }
    }
}
