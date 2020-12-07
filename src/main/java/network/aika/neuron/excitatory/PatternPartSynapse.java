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
import network.aika.neuron.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.PrimaryInhibitorySynapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.activation.Direction.*;
import static network.aika.neuron.phase.activation.ActivationPhase.TEMPLATE;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternPartSynapse<I extends Neuron<?>> extends ExcitatorySynapse<I, PatternPartNeuron> {

    public static byte type;

    public boolean isNegative;

    public boolean isRecurrent;
    public boolean inputScope;
    public boolean isSamePattern;

    public PatternPartSynapse() {
        super();
    }

    public PatternPartSynapse(I input, PatternPartNeuron output, Synapse template) {
        super(input, output, template);
    }

    public PatternPartSynapse(I input, PatternPartNeuron output, Synapse template, boolean isNegative, boolean isRecurrent, boolean inputScope, boolean isSamePattern) {
        super(input, output, template);
        this.isNegative = isNegative;
        this.isRecurrent = isRecurrent;
        this.inputScope = inputScope;
        this.isSamePattern = isSamePattern;
    }

    @Override
    public boolean checkTemplate(Activation iAct, Activation oAct, Visitor v) {
        return true;
    }

    @Override
    public boolean checkInduction(Link l) {
        return true;
    }

    @Override
    protected boolean checkOnCreate(Activation fromAct, Activation toAct, Visitor v) {
        return fromAct.getPhase() != TEMPLATE || !isRecurrent || v.getSelfRef();
    }

    @Override
    public PatternPartSynapse instantiateTemplate(I input, PatternPartNeuron output) {
        if(!input.getTemplates().contains(getInput())) {
            return null;
        }
        return new PatternPartSynapse(input, output, this, isNegative, isRecurrent, inputScope, isSamePattern);
    }

    @Override
    public Activation getOutputActivationToLink(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron() ||
                (isRecurrent() && !v.getSelfRef())) {
            return null;
        }

        if (isNegative() && !v.getSelfRef()) {
            oAct = oAct.createBranch(this);
        }
        return oAct;
    }

    @Override
    public void transition(Visitor v, Activation fromAct, Activation toAct, boolean create) {
        if (v.startDir == INPUT && output.getNeuron().isInputNeuron() && output.getNeuron() == v.origin.getNeuron()) { //X
            return;
        }

        if (v.scope == INPUT && isInputScope() && !v.related) {
            Visitor nv = v.prepareNextStep();
            nv.incrementPathLength();

            nv.related = true;

            next(fromAct, toAct, nv, create);

            if(v.downUpDir == INPUT) {
                return;
            }
        }

        Visitor nv = v.prepareNextStep();
        nv.incrementPathLength();

        if(v.samePattern && isInputScope()) {
            return;
        }

        if(v.downUpDir == INPUT && v.scope == INPUT && isInputScope()) {
            return;
        }

        // toggle related
        if (isSamePattern()) {
            nv.related = !v.related;
            nv.samePattern = true;
        }

        // switch scope
        if (isInputScope()) {
            nv.scope = v.scope.getNext(v.downUpDir);

            if (!nv.related && nv.scope == SAME) {
                return;
            }
        }

        next(fromAct, toAct, nv, create);
    }

    @Override
    public Reference getReference(Link l) {
        return (isRecurrent ? l.getOutput() : l.getInput())
                .getReference();
    }

    @Override
    public byte getType() {
        return type;
    }

    public boolean isNegative() {
        return isNegative;
    }

    public boolean isRecurrent() {
        return isRecurrent;
    }

    public boolean isInputScope() {
        return inputScope;
    }

    public boolean isSamePattern() {
        return isSamePattern;
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

        out.writeBoolean(isNegative);
        out.writeBoolean(isRecurrent);
        out.writeBoolean(inputScope);
        out.writeBoolean(isSamePattern);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        isNegative = in.readBoolean();
        isRecurrent = in.readBoolean();
        inputScope = in.readBoolean();
        isSamePattern = in.readBoolean();
    }
}
