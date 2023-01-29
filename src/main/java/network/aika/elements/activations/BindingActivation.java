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
package network.aika.elements.activations;

import network.aika.Thought;
import network.aika.elements.links.Link;
import network.aika.fields.*;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.visitor.DownVisitor;

import static network.aika.utils.Utils.TOLERANCE;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    private boolean isInput;

    private Multiplication posFeedbackDummy;

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);
    }

    @Override
    protected void connectWeightUpdate() {
        updateValue = new SumField(this, "updateValue", TOLERANCE);

        super.connectWeightUpdate();
    }

    @Override
    public void patternVisitDown(DownVisitor v, Link lastLink) {
        super.patternVisitDown(v, lastLink);
        v.up(this);
    }

    @Override
    public void selfRefVisitDown(DownVisitor v, Link lastLink) {
        v.up(this);
    }

    protected void initDummyLinks() {
        neuron.getInputSynapsesAsStream()
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

    public Multiplication getPosFeedbackDummy() {
        return posFeedbackDummy;
    }

    public void setPosFeedbackDummy(Multiplication posFeedbackDummy) {
        this.posFeedbackDummy = posFeedbackDummy;
    }

    public void updateBias(double u) {
        getNet().receiveUpdate(u);
    }
}
