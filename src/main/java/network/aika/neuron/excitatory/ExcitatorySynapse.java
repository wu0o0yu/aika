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

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatorySynapse<I extends Neuron<?>, O extends ExcitatoryNeuron> extends Synapse<I, O> {

    public static byte type;

    public ExcitatorySynapse() {
        super();
    }

    public ExcitatorySynapse(I input, O output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public boolean followSelfRef() {
        return false;
    }

    @Override
    public boolean checkRequiredSelfRef(boolean isSelfRef) {
        return true;
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
