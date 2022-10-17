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

import network.aika.Thought;
import network.aika.fields.*;
import network.aika.neuron.Range;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.visitor.DownVisitor;


import static network.aika.fields.Fields.*;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    private boolean isInput;

    protected Field mixedNetUB;
    protected Field mixedNetLB;

    private Field isOpen;
    private Field mix;

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);
    }

    @Override
    public void patternVisitDown(DownVisitor v, Link lastLink) {
        v.up(this);
    }

    @Override
    public void posFeedVisitDown(DownVisitor v, Link lastLink) {
        v.check(lastLink, this);
    }

    @Override
    protected void initFields() {
        isOpen = new Field(this, "isOpen", 1.0);
        mix = new Field(this, "mix", 1.0);

        mixedNetUB = mix(
                this,
                "mixedNetUB",
                mix,
                netUB,
                netLB
        );

        mixedNetLB = mix(
                this,
                "mixedNetLB",
                mix,
                netLB,
                netUB
        );

        valueUB = func(
                this,
                "value = f(mixedNetUB)",
                mixedNetUB,
                x -> getActivationFunction().f(x)
        );
        valueLB = func(
                this,
                "value = f(mixedNetLB)",
                mixedNetLB,
                x -> getActivationFunction().f(x)
        );

        neuron.getInputSynapses()
                .forEach(s ->
                        s.initDummyLink(this)
                );
    }

    public boolean isInput() {
        return isInput;
    }

    public void setInput(boolean input) {
        isInput = input;
    }

    public Field getIsOpen() {
        return isOpen;
    }

    public Field getMix() {
        return mix;
    }

    public Field getMixedNetUB() {
        return mixedNetUB;
    }

    public Field getMixedNetLB() {
        return mixedNetLB;
    }

    @Override
    public Range getRange() {
        return null;
    }

    public void updateBias(double u) {
        getNetUB().receiveUpdate(u);
        getNetLB().receiveUpdate(u);
    }
}
