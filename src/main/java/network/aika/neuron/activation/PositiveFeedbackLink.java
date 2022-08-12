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
import network.aika.neuron.conjunctive.PositiveFeedbackSynapse;

import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackLink<IA extends Activation<?>> extends BindingNeuronLink<PositiveFeedbackSynapse, IA> {

    public PositiveFeedbackLink(PositiveFeedbackSynapse s, IA input, BindingActivation output) {
        super(s, input, output);
    }

    @Override
    protected void initOnTransparent() {
        onTransparent = threshold(
                "onTransparent",
                0.0,
                ABOVE,
                synapse.getWeight()
        );
    }

    @Override
    protected void initWeightInput() {
        FieldLink fl = output.getNetUB().getInputLink(synapse);
        if (fl != null) {
            fl.getInput().removeOutput(fl, true);
            output.getNetUB().removeInput(fl);
        }

        super.initWeightInput();
    }
}
