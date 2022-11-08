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

import network.aika.fields.FieldOutput;
import network.aika.fields.MinMax;
import network.aika.fields.MinMaxField;
import network.aika.fields.Multiplication;
import network.aika.neuron.conjunctive.NegativeFeedbackSynapse;
import network.aika.neuron.visitor.UpVisitor;

import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;

/**
 * @author Lukas Molzberger
 */
public class NegativeFeedbackLink extends FeedbackLink<NegativeFeedbackSynapse, InhibitoryActivation> {

    MinMaxField maxInputUB;
    MinMaxField maxInputLB;

    public NegativeFeedbackLink(NegativeFeedbackSynapse s, InhibitoryActivation input, BindingActivation output) {
        super(s, input, output);

        input.getInputLinks().forEach(l ->
                input.connectFields((InhibitoryLink) l, this)
        );
    }

    @Override
    protected void initWeightInputUB() {
        maxInputLB = new MinMaxField(this, MinMax.MIN, "minLB");

        Multiplication weightedUB = mul(
                this,
                "!isOpen * x * weight",
                invert("!isOpen", output.getIsOpen()),
                initWeightedInput(false)
        );

        connect(weightedUB, getOutput().getNet(true));
    }

    @Override
    protected void initWeightInputLB() {
        maxInputUB = new MinMaxField(this, MinMax.MAX, "maxUB");

        weightedInputLB = initWeightedInput(true);

        connect(weightedInputLB, getOutput().getNet(false));
    }

    @Override
    public FieldOutput getInputValue(boolean upperBound) {
        return upperBound ?
                maxInputUB :
                maxInputLB;
    }

    @Override
    public void bindingVisitUp(UpVisitor v) {
        // don't allow negative feedback links to create new links; i.d. do nothing
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
    public void connectWeightUpdate() {
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

    public MinMaxField getMaxInputUB() {
        return maxInputUB;
    }

    public MinMaxField getMaxInputLB() {
        return maxInputLB;
    }
}
