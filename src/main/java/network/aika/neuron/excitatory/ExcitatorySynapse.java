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
import network.aika.neuron.activation.Visitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.activation.Direction.*;

/**
 *
 * @author Lukas Molzberger
 */
public class ExcitatorySynapse<I extends Neuron<?>, O extends ExcitatoryNeuron> extends Synapse<I, O> {

    public static byte type;

    public boolean isNegative;
    public boolean isRecurrent;
    public boolean inputScope;
    public boolean isSamePattern;

    public ExcitatorySynapse() {
        super();
    }

    public ExcitatorySynapse(I input, O output, boolean isNegative, boolean isRecurrent, boolean isInputScope, boolean isSamePattern) {
        super(input, output);

        this.isNegative = isNegative;
        this.isRecurrent = isRecurrent;
        this.inputScope = isInputScope;
        this.isSamePattern = isSamePattern;
    }

    @Override
    public Visitor transition(Visitor v) {
        Visitor nv = v.copy();
        nv.incrementPathLength();

        // check related change
        if (v.downUpDir == v.startDir && v.scope == INPUT && isInputScope() && !v.related) {
            nv.related = true;
            return nv;
        }

        if(v.samePattern && isInputScope()) {
            return null;
        }

        if(v.downUpDir == INPUT && v.scope == INPUT && isInputScope()) {
            return null;
        }

        // toggle related
        if (isSamePattern()) {
            nv.related = !v.related;
            nv.samePattern = true;
        }

        // switch scope
        if (isInputScope()) {
            nv.scope = v.scope.getNext(v.downUpDir);
        }

        return nv;
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
