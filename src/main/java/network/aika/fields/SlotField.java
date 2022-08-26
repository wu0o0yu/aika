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
import network.aika.utils.Utils;

import java.util.List;

import static network.aika.fields.FieldLink.link;

/**
 * @author Lukas Molzberger
 */
public class SlotField extends Field<Activation> {

    private FieldLink input;

    public SlotField(Activation reference, String label) {
        super(reference, label);
    }

    public void connect(BindingSignal bs) {
        if(!isConnected())
            FieldLink.connect(bs.getOnArrived(), this);
    }

    public boolean isConnected() {
        return input != null;
    }

    @Override
    public List<FieldLink> getInputs() {
        return List.of(input);
    }

    @Override
    public int getNextArg() {
        return 0;
    }

    public BindingSignal getFixedBindingSignal() {
        if(input == null)
            return null;

        return getBindingSignal(input);
    }

    private BindingSignal getBindingSignal(FieldLink l) {
        Field onArrived = (Field) l.getInput();
        return (BindingSignal) onArrived.getReference();
    }

    @Override
    public void addInput(FieldLink l) {
        assert input == null ||
                getBindingSignal(input).getOriginActivation() == getBindingSignal(l).getOriginActivation();
        input = l;
    }

    @Override
    public void removeInput(FieldLink l) {
        assert input == l;
        input = null;
    }

    public boolean isBound() {
        return input != null;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        input.getInput().removeOutput(input);
        input = null;
    }
}
