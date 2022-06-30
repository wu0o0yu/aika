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
import network.aika.fields.Field;
import network.aika.fields.Fields;
import network.aika.neuron.Range;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.disjunctive.BindingCategoryNeuron;
import network.aika.neuron.disjunctive.InhibitoryNeuron;

import static network.aika.neuron.bindingsignal.State.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class BindingCategoryActivation extends DisjunctiveActivation<BindingCategoryNeuron> {

    protected Field inputBSEvent = new Field(this, "inputBSEvent");


    public BindingCategoryActivation(int id, Thought t, BindingCategoryNeuron neuron) {
        super(id, t, neuron);
    }

    public void receiveBindingSignal(BindingSignal bs) {
        if(bs.getState() == INPUT)
            Fields.connect(bs.getOnArrived(), inputBSEvent, false);

        super.receiveBindingSignal(bs);
    }

    public Field getFixedBSEvent(State s) {
        if(s == INPUT)
            return inputBSEvent;

        return super.getFixedBSEvent(s);
    }

    @Override
    public Range getRange() {
        BindingSignal bs = getPrimaryBranchBindingSignal();
        if(bs == null)
            return null;

        return bs.getOriginActivation()
                .getRange();
    }

    private BindingSignal getPrimaryBranchBindingSignal() {
        return getBindingSignals()
                .filter(bs -> bs.getState() == INPUT)
                .filter(bs -> bs.getDepth() == 1)
                .findFirst()
                .orElse(null);
    }
}