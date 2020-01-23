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
import network.aika.neuron.Sign;
import network.aika.neuron.TNeuron;
import network.aika.neuron.activation.Activation;

/**
 *
 * @author Lukas Molzberger
 */
public class NegExcitatorySynapse extends ExcitatorySynapse {

    public static byte type;


    public NegExcitatorySynapse() {
        super();
    }

    public NegExcitatorySynapse(Neuron input, Neuron output) {
        super(input, output, true, false);
    }

    public NegExcitatorySynapse(Neuron input, Neuron output, int lastCount) {
        super(input, output, true, false, lastCount);
    }


    @Override
    public byte getType() {
        return type;
    }


    public void updateCountValue(Activation io, Activation oo) {
        double inputValue = io != null ? io.value : 0.0;
        double outputValue = oo != null ? oo.value : 0.0;

        if(!needsCountUpdate) {
            return;
        }
        needsCountUpdate = false;

        double optionProp = (io != null ? io.getP() : 1.0) * (oo != null ? oo.getP() : 1.0);

/*        if(TNeuron.checkSelfReferencing(oo, io)) {
            countValueIPosOPos += (Sign.POS.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        } else {
            countValueINegOPos += (Sign.NEG.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        }*/
        countValueIPosONeg += (Sign.POS.getX(inputValue) * Sign.NEG.getX(outputValue) * optionProp);
        countValueINegONeg += (Sign.NEG.getX(inputValue) * Sign.NEG.getX(outputValue) * optionProp);

        needsFrequencyUpdate = true;
    }


    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            NegExcitatorySynapse s = (NegExcitatorySynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output) -> new NegExcitatorySynapse(input, output, output.getModel().charCounter);
        }
    }
}
