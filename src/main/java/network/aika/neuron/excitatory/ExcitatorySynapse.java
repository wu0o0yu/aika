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

import network.aika.neuron.*;
import network.aika.neuron.activation.Visitor;

import static network.aika.neuron.activation.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class ExcitatorySynapse<I extends Neuron<?>, O extends ExcitatoryNeuron> extends Synapse<I, O> {

    public static byte type;

    public boolean isNegative;
    public boolean isRecurrent;
    public boolean inputScope;
    public boolean isRelated;

    public ExcitatorySynapse() {
        super();
    }

    public ExcitatorySynapse(I input, O output, boolean isNegative, boolean isRecurrent, boolean isInputScope, boolean isRelated) {
        super(input, output);

        this.isNegative = isNegative;
        this.isRecurrent = isRecurrent;
        this.inputScope = isInputScope;
        this.isRelated = isRelated;
    }

    @Override
    public Visitor transition(Visitor v) {
        Visitor nc = new Visitor(v, true);
        if(v.downUpDir == INPUT) {
            if(isInputScope()) {
                if (v.input) {
                    nc.related = true;
                } else {
                    nc.input = true;
                }
            }
        } else {
            if(isInputScope()) {
                nc.input = false;
            }
        }
        return nc;
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

    public boolean isRelated() {
        return isRelated;
    }

    public void setWeight(double weight) {
        super.setWeight(weight);
        output.getNeuron().setModified(true);
    }

    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);
        output.getNeuron().setModified(true);
    }
}
