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


import org.aika.Iteration;
import org.aika.Writable;
import org.aika.corpus.Range;
import org.aika.lattice.InputNode;
import org.aika.Utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;

/**
 *
 * @author Lukas Molzberger
 */
public class Synapse implements Writable {

    public static final Comparator<Synapse> INPUT_SYNAPSE_COMP = new Comparator<Synapse>() {
        @Override
        public int compare(Synapse s1, Synapse s2) {
            int r = compareWeights(s1.w, s2.w, 0.00001);
            if (r != 0) return r;
            r = s1.input.compareTo(s2.input);
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


    public void link(Iteration t) {
        boolean dir = input.id < output.id;

        (dir ? input : output).lock.acquireWriteLock(t.threadId);
        (dir ? output : input).lock.acquireWriteLock(t.threadId);

        input.outputSynapses.add(this);
        output.inputSynapses.add(this);

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
    public void readFields(DataInput in, Iteration t) throws IOException {
        input = t.m.neurons.get(in.readInt());
        output = t.m.neurons.get(in.readInt());
        inputNode = (InputNode) t.m.initialNodes.get(in.readInt());

        key = Key.read(in, t);

        w = in.readDouble();
        maxLowerWeightsSum = in.readDouble();

        input.outputSynapses.add(this);
        inputNode.setSynapse(t, new InputNode.SynapseKey(key.relativeRid, output), this);
    }


    public static Synapse read(DataInput in, Iteration t) throws IOException {
        Synapse k = new Synapse();
        k.readFields(in, t);
        return k;
    }



    public enum RangeSignal {
        START,
        END,
        NONE;

        public int getSignalPos(Range r, int def) {
            switch(this) {
                case START:
                    return r.begin;
                case END:
                    return r.end;
                case NONE:
                default:
                    return def;
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


    public enum RangeVisibility {
        MATCH_INPUT,
        MAX_OUTPUT,
        NONE;

        public static int apply(int a, RangeVisibility rva, int b, RangeVisibility rvb, boolean dir) {
            if(rva == NONE) return b;
            if(rvb == NONE) return a;

            if((dir && a < b) || (!dir && a >= b)) {
                if(rva == MATCH_INPUT) {
                    return a;
                } else if(rvb == MATCH_INPUT) {
                    return b;
                }
            } else {
                if(rvb == MATCH_INPUT) {
                    return b;
                } else if(rva == MATCH_INPUT) {
                    return a;
                }
            }

            if(dir) {
                return Math.max(a, b);
            } else {
                return Math.min(a, b);
            }
        }
    }


    public static class Key implements Comparable<Key>, Writable {
        public static final Key MIN_KEY = new Key();
        public static final Key MAX_KEY = new Key();

        public boolean isNeg;
        public boolean isRecurrent;
        public Integer relativeRid;
        public Integer absoluteRid;
        public boolean matchRange;
        public RangeSignal startSignal;
        public RangeVisibility startVisibility;
        public RangeSignal endSignal;
        public RangeVisibility endVisibility;


        public Key() {}


        public Key(boolean isNeg, boolean isRecurrent, Integer relativeRid, Integer absoluteRid, boolean matchRange, RangeSignal startSignal, RangeVisibility startVisibility, RangeSignal endSignal, RangeVisibility endVisibility) {
            this.isNeg = isNeg;
            this.isRecurrent = isRecurrent;
            this.relativeRid = relativeRid;
            this.absoluteRid = absoluteRid;
            this.matchRange = matchRange;
            this.startSignal = startSignal;
            this.startVisibility = startVisibility;
            this.endSignal = endSignal;
            this.endVisibility = endVisibility;
        }


        public Key createInputNodeKey() {
            return relativeRid != null ? new Key(isNeg, isRecurrent, 0, absoluteRid, matchRange, startSignal, startVisibility, endSignal, endVisibility) : this;
        }


        @Override
        public void write(DataOutput out) throws IOException {
            out.writeBoolean(isNeg);
            out.writeBoolean(isRecurrent);
            out.writeBoolean(relativeRid != null);
            if(relativeRid != null) out.writeInt(relativeRid);
            out.writeBoolean(absoluteRid != null);
            if(absoluteRid != null) out.writeInt(absoluteRid);
            out.writeBoolean(matchRange);
            out.writeUTF(startSignal.name());
            out.writeUTF(startVisibility.name());
            out.writeUTF(endSignal.name());
            out.writeUTF(endVisibility.name());
        }


        @Override
        public void readFields(DataInput in, Iteration t) throws IOException {
            isNeg = in.readBoolean();
            isRecurrent = in.readBoolean();
            if(in.readBoolean()) relativeRid = in.readInt();
            if(in.readBoolean()) absoluteRid = in.readInt();
            matchRange = in.readBoolean();
            startSignal = RangeSignal.valueOf(in.readUTF());
            startVisibility = RangeVisibility.valueOf(in.readUTF());
            endSignal = RangeSignal.valueOf(in.readUTF());
            endVisibility = RangeVisibility.valueOf(in.readUTF());
        }


        public static Key read(DataInput in, Iteration t) throws IOException {
            Key k = new Key();
            k.readFields(in, t);
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
            r = Boolean.compare(matchRange, k.matchRange);
            if(r != 0) return r;
            r = startSignal.compareTo(k.startSignal);
            if(r != 0) return r;
            r = startVisibility.compareTo(k.startVisibility);
            if(r != 0) return r;
            r = endSignal.compareTo(k.endSignal);
            if(r != 0) return r;
            return endVisibility.compareTo(k.endVisibility);
        }
    }
}
