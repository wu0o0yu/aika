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
import static network.aika.neuron.activation.Timestamp.NOT_SET_AFTER;
import static network.aika.neuron.bindingsignal.State.*;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    private FieldOutput branchProbability;
    private Multiplication norm;
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

        norm = mul("inputNorm * sameNorm", ConstantField.ONE, ConstantField.ONE);

        // (1 - (isFinal * (1 - bp))) : apply branch probability only when isFinal.
        branchProbability = invert(
                "branch-probability",
                mul(
                        "isFinal * (1 - bp)",
                        isFinal,
                        invert(
                                "(1 - bp)",
                                div(
                                        "exp(net) / norm",
                                        pow(expNet, 2.0),
                                        norm
                                )
                        )
                )
        );

        if(!isInput()) {
            func(
                    "f(bp * net)",
                    mul(
                            "bp * net",
                            branchProbability,
                            net
                    ),
                    x -> getNeuron().getActivationFunction().f(x),
                    value
            );
        }
    }

    @Override
    public void connectNorm(Field n, State s) {
        Integer arg = switch (s) {
            case INPUT -> 0;
            case SAME -> 1;
            default -> null;
        };

        connect(n, arg, norm);
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
                .filter(bs -> NOT_SET_AFTER.compare(bs.getOriginActivation().getFired(), fired) < 0)
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

    public FieldOutput getBranchProbability() {
        return branchProbability;
    }

    public void disconnect() {
        super.disconnect();

        FieldOutput[] fields = new FieldOutput[]{
                branchProbability,
                expNet
        };
        for(FieldOutput f: fields) {
            if(f == null)
                continue;
            f.disconnect();
        }
    }
}
