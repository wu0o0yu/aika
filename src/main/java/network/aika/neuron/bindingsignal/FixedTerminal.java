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
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;

import static network.aika.neuron.bindingsignal.TransitionMode.PROPAGATE_ONLY;


/**
 * @author Lukas Molzberger
 */
public class FixedTerminal extends PrimitiveTerminal {

    public FixedTerminal(State state, Direction type, Class<? extends Neuron> neuronClazz) {
        super(state, type, neuronClazz);
    }

    public static FixedTerminal fixed(State s, Direction type, Class<? extends Neuron> neuronClazz) {
        return new FixedTerminal(s, type, neuronClazz);
    }

    @Override
    public void initFixedTerminal(PrimitiveTransition t, Synapse ts, Activation act) {
        if(t.getMode() == PROPAGATE_ONLY)
            return;

        FieldOutput bsEvent = getSlot(act);
        if(bsEvent == null)
            return;

        Direction dir = type;
        Terminal.getPreconditionEvent(ts, act, dir, bsEvent)
                .addEventListener(() ->
                        ts.linkAndPropagate(
                                t,
                                dir,
                                getBindingSignal(bsEvent)
                        )
                );
    }

    @Override
    public void notify(PrimitiveTransition t, Synapse ts, BindingSignal bs) {
        // nothing to do here
    }



    public BindingSignal getBindingSignal(Activation act) {
        return getBindingSignal(getSlot(act));
    }

    public BindingSignal getBindingSignal(FieldOutput bsEvent) {
        return getBindingSignal((SlotField) bsEvent);
    }

    public BindingSignal getBindingSignal(SlotField bsEvent) {
        if(bsEvent == null)
            return null;

        return bsEvent.getFixedBindingSignal();
    }

    public String toString() {
        return "fixed(" + type + ":" + state + ")";
    }
}
