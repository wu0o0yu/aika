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
import network.aika.fields.FieldOutput;
import network.aika.fields.SlotField;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;

import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public class FixedBiTerminal extends BiTerminal<FixedTerminal> {

    public FixedBiTerminal(Direction type, BiTransition transition, FixedTerminal firstTerminal, FixedTerminal secondTerminal) {
        this.type = type;
        this.transition = transition;
        this.firstTerminal = firstTerminal;
        this.secondTerminal = secondTerminal;
    }

    @Override
    public void initFixedTerminal(Synapse ts, Activation act) {
        SlotField firstSlot = firstTerminal.getSlot(act);
        SlotField secondSlot = secondTerminal.getSlot(act);

        FieldOutput inputEvent = mul(
                "fixed bi-terminal event",
                firstSlot,
                secondSlot
        );

        Direction dir = type.invert();
        Terminal.getPreconditionEvent(ts, act, dir, inputEvent)
                .addEventListener(() ->
                        ts.linkAndPropagate(
                                transition,
                                dir,
                                firstTerminal.getBindingSignal(firstSlot),
                                secondTerminal.getBindingSignal(secondSlot)
                        )
                );
    }

    @Override
    public void notify(Synapse ts, BindingSignal bs) {
        // nothing to do
    }
}
