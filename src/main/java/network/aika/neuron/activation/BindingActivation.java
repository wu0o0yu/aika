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

import static network.aika.fields.ConstantField.ONE;
import static network.aika.fields.Fields.*;
import static network.aika.neuron.activation.Timestamp.FIRED_COMPARATOR;
import static network.aika.neuron.bindingsignal.State.*;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    private AbstractBiFunction bpWeightedNet;

    private FieldOutput bpSame;
    private FieldOutput bpInput;

    private FieldFunction expNet;

    private boolean isInput;

    protected SlotField inputBSSlot = new SlotField(this, "inputBSSlot");
    protected SlotField relatedSameBSSlot = new SlotField(this, "relatedSameBSSlot");


    protected BindingActivation(int id, BindingNeuron n) {
        super(id, n);
    }

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);

        expNet = exp(net);

        bpWeightedNet = mul(
                "bp * net",
                ONE,
                net
        );

        isFinal.addEventListener(() ->
            connect(
                    mul("bpInput * bpSame", bpInput, bpSame),
                    1,
                    bpWeightedNet,
                    true
            )
        );

        if(!isInput()) {
            func(
                    "f(bp * net)",
                    bpWeightedNet,
                    x -> getNeuron().getActivationFunction().f(x),
                    value
            );
        }
    }

    @Override
    public void connectNorm(Field n, State s) {
        connect(expNet, n);

        switch (s) {
            case INPUT:
                bpInput = div("bpInput", expNet, n);
            case SAME:
                bpSame = div("bpSame", expNet, n);
            default:
        }
    }

    public SlotField getSlot(State s) {
        return switch(s) {
            case INPUT -> inputBSSlot;
            case RELATED_SAME -> relatedSameBSSlot;
            default -> super.getSlot(s);
        };
    }

    @Override
    protected void initFields() {
        // Override parent
    }

    public boolean isInput() {
        return isInput;
    }

    public void setInput(boolean input) {
        isInput = input;
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
        getNet().receiveUpdate(u);
    }

    public FieldFunction getExpNet() {
        return expNet;
    }

    public AbstractBiFunction getBpWeightedNet() {
        return bpWeightedNet;
    }

    public FieldOutput getBpSame() {
        return bpSame;
    }

    public FieldOutput getBpInput() {
        return bpInput;
    }
    public void disconnect() {
        super.disconnect();

        FieldOutput[] fields = new FieldOutput[]{
                bpWeightedNet,
                bpSame,
                bpInput,
                expNet
        };
        for(FieldOutput f: fields) {
            if(f == null)
                continue;
            f.disconnect();
        }
    }
}
