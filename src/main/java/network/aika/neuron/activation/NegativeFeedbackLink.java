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
package network.aika.neuron.activation;

import network.aika.fields.FieldLink;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.NegativeFeedbackSynapse;

import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;
import static network.aika.neuron.bindingsignal.State.INPUT;

/**
 * @author Lukas Molzberger
 */
public class NegativeFeedbackLink extends BindingNeuronLink<NegativeFeedbackSynapse, InhibitoryActivation> {


    public NegativeFeedbackLink(NegativeFeedbackSynapse s, InhibitoryActivation input, BindingActivation output) {
        super(s, input, output);
    }

    public boolean isSelfRef() {
        BindingSignal iBS = input.getBindingSignal(INPUT);
        if(iBS == null)
            return false;

        return iBS.isSelfRef(
                output.getBindingSignal(INPUT)
        );
    }

    @Override
    protected void initWeightInput() {
        if(isSelfRef())
            return;

        FieldLink fl = output.getNetLB().getInputLink(synapse.getDummyLinkLabel());
        fl.getInput().removeOutput(fl, true);
        output.getNetLB().removeInput(fl);

        weightedInputUB = mul(
                "wiUP = iAct.valueLB * s.weight",
                input.getValueLB(),
                synapse.getWeight(),
                getOutput().getNetUB()
        );
        weightedInputLB = mul(
                "wiLB = iAct.valueUB * s.weight",
                input.getValueUB(),
                synapse.getWeight(),
                getOutput().getNetLB()
        );
    }

    @Override
    protected void initOnTransparent() {
        onTransparent = threshold(
                "onTransparent",
                0.0,
                ABOVE,
                input.isFired
        );
    }

    @Override
    public void initWeightUpdate() {
        mul(
                "weight update",
                getInput().getIsFired(),
                scale("-1 * og", -1, getOutput().getUpdateValue()),
                synapse.getWeight()
        );
    }
}
