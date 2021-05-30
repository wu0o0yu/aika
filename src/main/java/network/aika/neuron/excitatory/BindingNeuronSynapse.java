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
package network.aika.neuron.excitatory;

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.activation.visitor.Visitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.activation.Fired.NOT_FIRED;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class BindingNeuronSynapse<I extends Neuron<?>> extends ExcitatorySynapse<I, BindingNeuron> {

    public boolean isRecurrent;

    public BindingNeuronSynapse() {
        super();
    }

    public BindingNeuronSynapse(boolean isRecurrent) {
        this.isRecurrent = isRecurrent;
    }

    @Override
    public void updateReference(Link l) {
        if(l.getInput() == null)
            return;

        Reference iRef = l.getInput().getReference();
        if(iRef == null)
            return;

        // TODO: find a better solution.
        Synapse ts = l.getSynapse().getTemplate();
        Templates t = getModel().getTemplates();
        if(ts != t.RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE && ts != t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE) {
            l.getOutput().propagateReference(iRef);
        }
    }

    @Override
    protected boolean checkCausality(Activation fromAct, Activation toAct, Visitor v) {
        if(!isRecurrent) {
            return fromAct.getFired() != NOT_FIRED && Fired.COMPARATOR.compare(fromAct.getFired(), toAct.getFired()) <= 0;
        } else {
            return v.getSelfRef();
        }
    }

    protected void initFromTemplate(BindingNeuronSynapse s) {
        super.initFromTemplate(s);

        s.isRecurrent = isRecurrent;
    }

    @Override
    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron())
            return null;

        if(isRecurrent() && !v.getSelfRef())
            return null;

        return oAct;
    }

    @Override
    public Reference getReference(Link l) {
        return (
                isRecurrent ?
                        l.getOutput() :
                        l.getInput()
        ).getReference();
    }

    public boolean isRecurrent() {
        return isRecurrent;
    }

    public void setWeight(double weight) {
        super.setWeight(weight);
        output.getNeuron().setModified(true);
    }

    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);
        output.getNeuron().setModified(true);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeBoolean(isRecurrent);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        isRecurrent = in.readBoolean();
    }
}
