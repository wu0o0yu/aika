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
package network.aika.neuron.disjunctive;

import network.aika.Model;
import network.aika.neuron.SampleSpace;
import network.aika.neuron.activation.*;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.visitor.linking.LinkingOperator;
import network.aika.neuron.visitor.linking.inhibitory.InhibitoryDownVisitor;
import network.aika.neuron.visitor.linking.pattern.PatternDownVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends DisjunctiveSynapse<
        InhibitorySynapse,
        BindingNeuron,
        InhibitoryNeuron,
        InhibitoryLink,
        BindingActivation,
        InhibitoryActivation
        >
{

    private InhibSynType type;

    public InhibitorySynapse() {
    }

    public InhibitorySynapse(InhibSynType type) {
        this.type = type;
    }

    public InhibSynType getType() {
        return type;
    }


    @Override
    public void startVisitor(LinkingOperator c, Activation bs) {
        new InhibitoryDownVisitor(bs.getThought(), c)
                .start(bs);
    }

    @Override
    public InhibitorySynapse instantiateTemplate(BindingNeuron input, InhibitoryNeuron output) {
        InhibitorySynapse s = new InhibitorySynapse(type);
        s.initFromTemplate(input, output, this);
        return s;
    }

    @Override
    public InhibitoryLink createLink(BindingActivation input, InhibitoryActivation output) {
        return new InhibitoryLink(this, input, output);
    }


    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeInt(type.ordinal());
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        type = InhibSynType.values()[in.readInt()];
    }
}
