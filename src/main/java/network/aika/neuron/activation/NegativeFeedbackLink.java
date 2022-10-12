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

import network.aika.direction.Direction;
import network.aika.neuron.conjunctive.NegativeFeedbackSynapse;
import network.aika.neuron.linking.Visitor;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;

/**
 * @author Lukas Molzberger
 */
public class NegativeFeedbackLink extends BindingNeuronLink<NegativeFeedbackSynapse, InhibitoryActivation> {


    public NegativeFeedbackLink(NegativeFeedbackSynapse s, InhibitoryActivation input, BindingActivation output) {
        super(s, input, output);
    }

    public void trackBindingSignal(Visitor v, Predicate<Activation> p) {
        if(visited == v.getV())
            return;
        visited = v.getV();

        followBindingSignal(v, p);
    }



    @Override
    protected void initWeightInput() {
        if(isSelfRef())
            return;

        weightedInputUB = initWeightedInput(false);
        weightedInputLB = initWeightedInput(true);

        connect(weightedInputUB, input.getId(), getOutput().lookupLinkSlot(synapse, true));
        connect(weightedInputLB, input.getId(), getOutput().lookupLinkSlot(synapse, false));
    }

    private boolean isSelfRef() {
        return input.isSelfRef(output);
    }

    @Override
    protected void initOnTransparent() {
        onTransparent = threshold(
                this,
                "onTransparent",
                0.0,
                ABOVE,
                input.isFired
        );
    }

    @Override
    public void initWeightUpdate() {
        mul(
                this,
                "weight update",
                getInput().getIsFired(),
                scale(
                        this,
                        "-1 * og",
                        -1,
                        getOutput().getUpdateValue()
                ),
                synapse.getWeight()
        );
    }
}
