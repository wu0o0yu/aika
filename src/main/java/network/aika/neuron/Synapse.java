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
import network.aika.Writable;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.Synapse.State.CURRENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<I extends Neuron, O extends Neuron> implements Writable, InputKey, OutputKey {

    public static double TOLERANCE = 0.0000001;

    protected boolean isRecurrent;
    protected boolean isNegative;
    protected boolean isPropagate;
    PatternScope patternScope;

    protected NeuronProvider input;
    protected NeuronProvider output;

    private double weight;
    private double weightDelta;

    public Synapse() {
    }

    public Synapse(NeuronProvider input, NeuronProvider output) {
        this.input = input;
        this.output = output;
    }

    public void setInput(NeuronProvider input) {
        this.input = input;
    }

    public void setOutput(NeuronProvider output) {
        this.output = output;
    }

    public abstract byte getType();

    public PatternScope getPatternScope() {
        return patternScope;
    }

    public void setPatternScope(PatternScope patternScope) {
        this.patternScope = patternScope;
    }

    public void setRecurrent(boolean recurrent) {
        isRecurrent = recurrent;
    }

    public boolean isRecurrent() {
        return isRecurrent;
    }

    public void setNegative(boolean negative) {
        isNegative = negative;
    }

    public boolean isNegative() {
        return isNegative;
    }

    public void setPropagate(boolean propagate) {
        isPropagate = propagate;
    }

    public boolean isPropagate() {
        return isPropagate;
    }

    protected abstract void link(Neuron in, Neuron out);

    protected abstract void unlink(Neuron in, Neuron out);

    public NeuronProvider getPInput() {
        return input;
    }

    public NeuronProvider getPOutput() {
        return output;
    }

    public I getInput() {
        return (I) input.get();
    }

    public O getOutput() {
        return (O) output.get();
    }

    public Neuron getNeuron(Direction dir) {
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
        Neuron in = input.get();
        Neuron out = output.get();

        link(in, out);
    }

    public void unlink() {
        Neuron in = input.get();
        Neuron out = output.get();

        unlink(in, out);
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

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void updateDelta(double weightDelta) {
        this.weightDelta += weightDelta;
    }

    public void update(double weight) {
        this.weightDelta = weight - this.weight;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());

        out.writeLong(input.getId());
        out.writeLong(output.getId());

        out.writeDouble(weight);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readLong());
        output = m.lookupNeuron(in.readLong());

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

        protected NeuronProvider inputNeuron;
        protected double weight;
        protected boolean propagate;
        protected PatternScope patternScope;
        protected boolean isRecurrent;
        protected boolean isNegative;

        /**
         * Determines the input neuron.
         *
         * @param inputNeuron
         * @return
         */
        public Builder setNeuron(NeuronProvider inputNeuron) {
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
        public Builder setNeuron(Neuron<?> neuron) {
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

        public Builder setPatternScope(PatternScope ps) {
            patternScope = ps;
            return this;
        }

        public Builder setRecurrent(boolean recurrent) {
            isRecurrent = recurrent;
            return this;
        }

        public Builder setNegative(boolean negative) {
            isNegative = negative;
            return this;
        }

        public Synapse getSynapse(NeuronProvider outputNeuron) {
            Synapse s = createSynapse(outputNeuron);
            s.setPatternScope(patternScope);
            s.setRecurrent(isRecurrent);
            s.setNegative(isNegative);
            s.setPropagate(propagate);
            return s;
        }

        public abstract Synapse createSynapse(NeuronProvider outputNeuron);
    }
}
