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
package network.aika.fields;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.utils.Utils;

/**
 * @author Lukas Molzberger
 */
public class SlotField extends AbstractField<Activation> {

    private FieldLink input;

    public SlotField(Activation reference, String label) {
        super(reference, label);
    }

    public void connect(BindingSignal bs) {
        Fields.connect(bs.getOnArrived(), this, false);
    }

    public BindingSignal getFixedBindingSignal() {
        if(input == null)
            return null;

        Field onArrived = (Field) input.getInput();
        return (BindingSignal) onArrived.getReference();
    }

    @Override
    protected boolean checkPreCondition(Double cv, double nv, double u) {
        return !Utils.belowTolerance(u);
    }

    @Override
    public void addInput(FieldLink l) {
        assert input == null;
        input = l;
    }

    @Override
    public void removeInput(FieldLink l) {
        assert input == l;
        input = null;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        input.getInput().removeOutput(input, false);
        input = null;
    }
}
