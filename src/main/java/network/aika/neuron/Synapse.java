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
import network.aika.Thought;
import network.aika.Writable;
import network.aika.neuron.activation.Direction;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;

import static network.aika.neuron.Synapse.State.CURRENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<I extends INeuron, O extends INeuron> implements Writable, InputKey, OutputKey {

    public static double TOLERANCE = 0.0000001;

    public static Comparator<Synapse> INPUT_COMPARATOR = Comparator.comparing(s -> s.getInput());

    protected Neuron input;
    protected Neuron output;

    private double weight;
    private double weightDelta;

    public Synapse() {
    }

    public Synapse(Neuron input, Neuron output) {
        this.input = input;
        this.output = output;
    }

    public void setInput(Neuron input) {
        this.input = input;
    }

    public void setOutput(Neuron output) {
        this.output = output;
    }

    public abstract void init(PatternScope patternScope, Boolean isRecurrent, Boolean isNegative, boolean propagate);

    public abstract byte getType();

    public abstract boolean isPropagate();

    public abstract boolean isRecurrent();

    public abstract boolean isNegative();

    public abstract PatternScope getPatternScope();

    protected abstract void addLinkInternal(INeuron in, INeuron out);

    protected abstract void removeLinkInternal(INeuron in, INeuron out);

    public Neuron getPInput() {
        return input;
    }

    public Neuron getPOutput() {
        return output;
    }

    public I getInput() {
        return (I) input.get();
    }

    public O getOutput() {
        return (O) output.get();
    }

    public INeuron getNeuron(Direction dir) {
        switch(dir) {
            case INPUT:
                return getInput();
            case OUTPUT:
                return getOutput();
        }

        return null;
    }

    public double getWeight() {
        return weight;
    }

    public double getNewWeight() {
        return weight + weightDelta;
    }

    public double getWeight(State s) {
        return s == CURRENT ? weight : getNewWeight();
    }

    public double getWeightDelta() {
        return weightDelta;
    }

    public void link() {
        INeuron in = input.get();
        INeuron out = output.get();

        boolean dir = in.getId() < out.getId();

        (dir ? in : out).lock.acquireWriteLock();
        (dir ? out : in).lock.acquireWriteLock();

        addLinkInternal(in, out);

        (dir ? in : out).lock.releaseWriteLock();
        (dir ? out : in).lock.releaseWriteLock();
    }

    public void unlink() {
        INeuron in = input.get();
        INeuron out = output.get();

        boolean dir = input.getId() < out.getId();

        (dir ? in : out).lock.acquireWriteLock();
        (dir ? out : in).lock.acquireWriteLock();

        removeLinkInternal(in, out);

        (dir ? in : out).lock.releaseWriteLock();
        (dir ? out : in).lock.releaseWriteLock();
    }

    public void commit() {
        weight += weightDelta;
        weightDelta = 0.0;
    }

    public boolean isZero() {
        return Math.abs(weight) < TOLERANCE;
    }

    public enum State {
        NEXT,
        CURRENT
    }

    public boolean isWeak(State state) {
        return output.get().isWeak(this, state);
    }

    public void updateDelta(Thought doc, double weightDelta) {
        this.weightDelta += weightDelta;

        if(doc != null) {
            doc.notifyWeightModified(this);
        }
    }

    public void update(Thought t, double weight) {
        this.weightDelta = weight - this.weight;

        if(t != null) {
            t.notifyWeightModified(this);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());

        out.writeInt(input.getId());
        out.writeInt(output.getId());

        out.writeDouble(weight);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readInt());
        output = m.lookupNeuron(in.readInt());

        weight = in.readDouble();
    }

    public String toString() {
        return "S W:" + Utils.round(getNewWeight()) + " " + input + "->" + output;
    }

    /**
     * The {@code Builder} class is just a helper class which is used to initialize a neuron. Most of the parameters of this class
     * will be mapped to a input synapse for this neuron.
     *
     * @author Lukas Molzberger
     */
    public static abstract class Builder {

        protected Neuron inputNeuron;
        protected double weight;
        protected boolean propagate;

        /**
         * Determines the input neuron.
         *
         * @param inputNeuron
         * @return
         */
        public Builder setNeuron(Neuron inputNeuron) {
            assert inputNeuron != null;
            this.inputNeuron = inputNeuron;
            return this;
        }

        /**
         * Determines the input neuron.
         *
         * @param neuron
         * @return
         */
        public Builder setNeuron(INeuron<?> neuron) {
            assert neuron != null;
            this.inputNeuron = neuron.getProvider();
            return this;
        }

        public Builder setPropagate(boolean propagate) {
            this.propagate = propagate;
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

        public abstract Synapse getSynapse(Neuron outputNeuron);
    }
}
