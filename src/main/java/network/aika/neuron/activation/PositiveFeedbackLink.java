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

import network.aika.fields.AbstractBiFunction;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.PositiveFeedbackSynapse;

import static network.aika.fields.Fields.mul;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackLink<IA extends Activation> extends BindingNeuronLink<PositiveFeedbackSynapse, IA> {

    private AbstractBiFunction feedbackWeightInput;
    private AbstractBiFunction feedbackBiasInput;

    public PositiveFeedbackLink(PositiveFeedbackSynapse s, BindingSignal<IA> input, BindingSignal<BindingActivation> output) {
        super(s, input, output);
    }

    @Override
    protected void initWeightInput() {
        if(isCausal())
            super.initWeightInput();

        feedbackWeightInput = mul(
                "iAct.finalValue * s.feedbackWeighted",
                input.getFinalValue(),
                synapse.getFeedbackWeight(),
                getOutput().getNet()
        );

        feedbackBiasInput = mul(
                "iAct.isFinal * s.feedbackBias",
                input.getIsFinal(),
                synapse.getFeedbackBias(),
                getOutput().getNet()
        );
    }

    @Override
    public void initWeightUpdate() {
        if(isCausal()) {
            super.initWeightUpdate();
        } else {
            mul(
                    "feedback weight update",
                    getInput().getIsFiredForWeight(),
                    getOutput().getUpdateValue(),
                    synapse.getFeedbackWeight()
            );

            mul(
                    "feedback bias update",
                    getInput().getIsFiredForBias(),
                    getOutput().getUpdateValue(),
                    synapse.getFeedbackBias()
            );
        }
    }

    protected void initBackpropGradient() {
        if(isCausal()) {
            super.initWeightUpdate();
        } else {
            backPropGradient = mul(
                    "oAct.ownOutputGradient * s.feedbackWeight",
                    output.ownOutputGradient,
                    synapse.getFeedbackWeight(),
                    input.backpropInputGradient
            );
        }
    }

    public AbstractBiFunction getFeedbackWeightInput() {
        return feedbackWeightInput;
    }

    public AbstractBiFunction getFeedbackBiasInput() {
        return feedbackBiasInput;
    }
}
