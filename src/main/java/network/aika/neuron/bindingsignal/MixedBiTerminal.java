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
package network.aika.neuron.bindingsignal;

import network.aika.direction.Direction;
import network.aika.fields.SlotField;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;

import java.util.stream.Stream;

import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.bindingsignal.TransitionMode.PROPAGATE_ONLY;

/**
 * @author Lukas Molzberger
 */
public class MixedBiTerminal extends BiTerminal<VariableTerminal> {


    public MixedBiTerminal(Direction type, BiTransition transition, VariableTerminal firstTerminal, FixedTerminal secondTerminal) {
        this.type = type;
        this.transition = transition;
        this.firstTerminal = firstTerminal;
        this.secondTerminal = secondTerminal;
    }

    @Override
    public void initFixedTerminal(Synapse ts, Activation act) {
        SlotField secondSlot = secondTerminal.getSlot(act);
        Terminal.getPreconditionEvent(
                        ts,
                        act,
                        type.invert(),
                        secondSlot
                )
                .addEventListener(() ->
                        notifyInternal(ts, act, secondSlot)
                );
    }

    private void notifyInternal(Synapse ts, Activation act, SlotField secondSlot) {
        Stream<BindingSignal> bsStream = act.getBindingSignals(firstTerminal.state);
        bsStream
                .forEach(bs ->
                        ts.linkAndPropagate(
                                transition,
                                type.invert(),
                                bs,
                                secondTerminal.getBindingSignal(secondSlot)
                        )
                );
    }

    @Override
    public void notify(Synapse ts, BindingSignal bs) {
        if(transition.getMode() == PROPAGATE_ONLY)
            return;

        if(firstTerminal.getState() != bs.getState())
            return;

        SlotField secondSlot = secondTerminal.getSlot(bs.getActivation());

        if(isTrue(secondSlot))
            ts.linkAndPropagate(
                    transition,
                    type.invert(),
                    bs,
                    secondTerminal.getBindingSignal(secondSlot)
            );
    }
}
