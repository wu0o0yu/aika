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

import static network.aika.fields.Fields.isTrue;

/**
 * @author Lukas Molzberger
 */
public class OptionalFixedBiTerminal extends BiTerminal<FixedTerminal> {

    public OptionalFixedBiTerminal(Direction type, BiTransition transition, FixedTerminal firstTerminal, FixedTerminal secondTerminal) {
        super(type, transition, firstTerminal, secondTerminal);
    }

    @Override
    public void initFixedTerminal(Synapse ts, Activation act) {
        SlotField firstSlot = firstTerminal.getSlot(act);
        SlotField secondSlot = secondTerminal.getSlot(act);

        Direction dir = type.invert();
        Terminal.getPreconditionEvent(ts, act, dir, firstSlot)
                .addEventListener(() -> {
                    if(!secondSlot.isBound() || isTrue(secondSlot))
                        linkAndPropagate(ts, firstSlot, secondSlot, dir);
                });

        Terminal.getPreconditionEvent(ts, act, dir, secondSlot)
                .addEventListener(() -> {
                    if(!firstSlot.isBound() || isTrue(firstSlot))
                        linkAndPropagate(ts, firstSlot, secondSlot, dir);
                });
    }

    private void linkAndPropagate(Synapse ts, SlotField firstSlot, SlotField secondSlot, Direction dir) {
        ts.linkAndPropagate(
                transition,
                dir,
                firstTerminal.getBindingSignal(firstSlot),
                secondTerminal.getBindingSignal(secondSlot)
        );
    }

    @Override
    public void notify(Synapse ts, BindingSignal bs) {
        // nothing to do
    }
}
