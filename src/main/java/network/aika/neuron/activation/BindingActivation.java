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
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.conjunctive.BindingNeuron;

import java.util.*;

import static network.aika.fields.Fields.*;
import static network.aika.neuron.activation.Timestamp.FIRED_COMPARATOR;
import static network.aika.neuron.bindingsignal.State.*;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    private boolean isInput;

    protected SlotField inputBSSlot = new SlotField(this, "inputBSSlot");
    protected SlotField relatedSameBSSlot = new SlotField(this, "relatedSameBSSlot");

    protected Field mixedNetUB;
    protected Field mixedNetLB;

    private Field isOpen;
    private Field mix;

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);
    }

    @Override
    public BindingSignal getAbstractBindingSignal() {
        return getBindingSignal(ABSTRACT);
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

    @Override
    public SlotField getSlot(State s) {
        return switch(s) {
            case INPUT -> inputBSSlot;
            case RELATED_SAME -> relatedSameBSSlot;
            default -> super.getSlot(s);
        };
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
        BindingSignal bs = getPrimaryPatternBindingSignal();
        if(bs == null)
            return null;

        return bs.getOriginActivation()
                .getRange();
    }

    private BindingSignal getPrimaryPatternBindingSignal() {
        return getPatternBindingSignals().values().stream()
                .filter(bs -> FIRED_COMPARATOR.compare(bs.getOriginActivation().getFired(), fired) < 0)
                .filter(bs -> bs.getState() == SAME || bs.getState() == INPUT || bs.getState() == RELATED_SAME)
                .min(Comparator.comparing(bs -> bs.getState().ordinal()))
                .orElse(null);
    }

    public void updateBias(double u) {
        getNetUB().receiveUpdate(u);
        getNetLB().receiveUpdate(u);
    }
}
