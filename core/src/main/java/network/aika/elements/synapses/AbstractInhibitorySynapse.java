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
package network.aika.elements.synapses;

import network.aika.Model;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.InhibitoryActivation;
import network.aika.elements.links.AbstractInhibitoryLink;
import network.aika.elements.neurons.InhibitoryNeuron;
import network.aika.elements.neurons.Neuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class AbstractInhibitorySynapse<S extends AbstractInhibitorySynapse, I extends Neuron, L extends AbstractInhibitoryLink<S, IA>, IA extends Activation<?>> extends DisjunctiveSynapse<
        S,
        I,
        InhibitoryNeuron,
        L,
        IA,
        InhibitoryActivation
        >
{
    private Scope type;

    private AbstractInhibitorySynapse() {
        super(null);
    }

    public AbstractInhibitorySynapse(Scope type) {
        this();
        this.type = type;
    }

    public Scope getType() {
        return type;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeBoolean(type != null);
        out.writeInt(type.ordinal());
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        if(in.readBoolean())
            type = Scope.values()[in.readInt()];
    }
}
