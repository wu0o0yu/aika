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

import network.aika.fields.Field;
import network.aika.neuron.disjunctive.InhibitorySynapse;
import network.aika.steps.link.LinkingIn;

import static network.aika.fields.Fields.add;
import static network.aika.fields.Fields.func;

/**
 * @author Lukas Molzberger
 */
public class InhibitoryLink extends DisjunctiveLink<InhibitorySynapse, BindingActivation, InhibitoryActivation> {

    protected Field valueUB;
    protected Field valueLB;

    protected Field netUB;
    protected Field netLB;

    public InhibitoryLink(InhibitorySynapse s, BindingActivation input, InhibitoryActivation output) {
        super(s, input, output);

        initFields();
    }

    protected void initFields() {
        netUB = add(
                this,
                "netUB",
                output.getNeuron().getBias(),
                input.getValue(true)
        );

        netLB = add(
                this,
                "netLB",
                output.getNeuron().getBias(),
                input.getValue(false)
        );

        valueUB = func(
                this,
                "value = f(netUB)",
                netUB,
                x -> output.getActivationFunction().f(x)
        );

        valueLB = func(
                this,
                "value = f(netLB)",
                netLB,
                x -> output.getActivationFunction().f(x)
        );

        output.getOutputLinks().forEach(l ->
                output.connectFields(this, (NegativeFeedbackLink) l)
        );
    }

    public Field getValueUB() {
        return valueUB;
    }

    public Field getValueLB() {
        return valueLB;
    }

    public Field getNetUB() {
        return netUB;
    }

    public Field getNetLB() {
        return netLB;
    }
}
