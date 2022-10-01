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
package network.aika.neuron.conjunctive;

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.ConjunctiveActivation;
import network.aika.neuron.activation.Link;
import network.aika.utils.Utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveSynapse<S extends ConjunctiveSynapse, I extends Neuron, O extends ConjunctiveNeuron<?, OA>, L extends Link<S, IA, OA>, IA extends Activation<?>, OA extends ConjunctiveActivation> extends
        Synapse<
                S,
                I,
                O,
                L,
                IA,
                OA
                >
{
    protected ConjunctiveNeuronType type;

    private double sumOfLowerWeights;

    public ConjunctiveSynapse(ConjunctiveNeuronType type) {
        this.type = type;
    }

    public void setOutput(O output) {
        assert type == output.getType();
        super.setOutput(output);
    }

    protected double getSortingWeight() {
        return getWeight().getCurrentValue();
    }

    @Override
    public boolean propagateCheck(IA iAct) {
        return getWeight().getCurrentValue() + sumOfLowerWeights > 0.0;
    }

    @Override
    public double getSumOfLowerWeights() {
        return sumOfLowerWeights;
    }

    public void setSumOfLowerWeights(double sumOfLowerWeights) {
        if(!Utils.belowTolerance(this.sumOfLowerWeights - sumOfLowerWeights))
            setModified();

        this.sumOfLowerWeights = sumOfLowerWeights;
    }

    @Override
    public void setModified() {
        if(output != null)
            getOutput().setModified();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeInt(type.ordinal());
        out.writeDouble(sumOfLowerWeights);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        type = ConjunctiveNeuronType.values()[in.readInt()];
        sumOfLowerWeights = in.readDouble();
    }

    public void initDummyLink(BindingActivation bindingActivation) {

    }
}
