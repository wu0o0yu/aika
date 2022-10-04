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
import network.aika.neuron.activation.Activation;

import java.util.stream.Stream;

/**
 * @author Lukas Molzberger
 */
public abstract class PrimitiveTerminal extends Terminal<PrimitiveTransition> {

    protected BiTerminal parent = null;
    protected State state;
    protected Direction type;

    private Class<? extends Neuron> neuronClazz;

    public PrimitiveTerminal(State state, Direction type, Class<? extends Neuron> neuronClazz) {
        this.state = state;
        this.type = type;
        this.neuronClazz = neuronClazz;
    }

    public SlotField getSlot(Activation act) {
        return act != null ?
                act.getSlot(state) :
                null;
    }

    public Class<? extends Neuron> getNeuronClazz() {
        return neuronClazz;
    }

    public BiTerminal getParent() {
        return parent;
    }

    public void setParent(BiTerminal parent) {
        this.parent = parent;
    }

    public State getState() {
        return state;
    }

    @Override
    public Direction getType() {
        return type;
    }

    @Override
    public Stream<PrimitiveTerminal> getPrimitiveTerminals() {
        return Stream.of(this);
    }

    public abstract BindingSignal getBindingSignal(FieldOutput bsEvent);

    @Override
    public boolean matchesState(State s) {
        return s == state;
    }
}
