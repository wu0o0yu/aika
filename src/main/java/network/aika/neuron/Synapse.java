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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<I extends Neuron<?>, O extends Neuron<?>> implements Writable {

    public static double TOLERANCE = 0.0000001;

    protected boolean isNegative;
    protected boolean isPropagate;

    protected NeuronProvider input;
    protected NeuronProvider output;

    private double weight;

    public Synapse() {
    }

    public Synapse(I input, O output) {
        this.input = input.getProvider();
        this.output = output.getProvider();
    }

    public void setInput(I input) {
        this.input = input.getProvider();
    }

    public void setOutput(O output) {
        this.output = output.getProvider();
    }

    public abstract byte getType();

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
        return (I) input.getNeuron();
    }

    public O getOutput() {
        return (O) output.getNeuron();
    }

    public double getWeight() {
        return weight;
    }

    public void link() {
        Neuron in = input.getNeuron();
        Neuron out = output.getNeuron();

        link(in, out);
    }

    public void unlink() {
        Neuron in = input.getNeuron();
        Neuron out = output.getNeuron();

        unlink(in, out);
    }

    public boolean isZero() {
        return Math.abs(weight) < TOLERANCE;
    }

/*
    public boolean isWeak(State state) {
        return output.get().isWeak(this, state);
    }
*/
    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void updateWeight(double weightDelta) {
        this.weight += weightDelta;
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
        return "S " + getClass().getSimpleName() + "  w:" + Utils.round(getWeight()) + " " + input + "->" + output + " (neg:" + isNegative() + ", prop:" + isPropagate() + ")";
    }
}
