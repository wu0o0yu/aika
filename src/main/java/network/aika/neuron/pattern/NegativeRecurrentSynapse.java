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
package network.aika.neuron.pattern;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.Sign;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

/**
 *
 * @author Lukas Molzberger
 */
public class NegativeRecurrentSynapse extends ExcitatorySynapse<InhibitoryNeuron, PatternPartNeuron> {

    public static byte type;

    public NegativeRecurrentSynapse() {
        super();
    }

    public NegativeRecurrentSynapse(Neuron input, Neuron output, int lastCount) {
        super(input, output, false, lastCount);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    public void updateCountValue(Activation iAct, Activation oAct) {
        if(!needsCountUpdate) {
            return;
        }
        needsCountUpdate = false;

        double p = (iAct != null ? iAct.getP() : 1.0) * (oAct != null ? oAct.getP() : 1.0);

/*        if(TNeuron.checkSelfReferencing(oo, io)) {
            countValueIPosOPos += (Sign.POS.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        } else {
            countValueINegOPos += (Sign.NEG.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        }*/
        countValueIPosONeg += (Sign.POS.getX(iAct) * Sign.NEG.getX(oAct) * p);
        countValueINegONeg += (Sign.NEG.getX(iAct) * Sign.NEG.getX(oAct) * p);

        needsFrequencyUpdate = true;
    }

    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            NegativeRecurrentSynapse s = (NegativeRecurrentSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output) -> new NegativeRecurrentSynapse(input, output, output.getModel().charCounter);
        }
    }
}
