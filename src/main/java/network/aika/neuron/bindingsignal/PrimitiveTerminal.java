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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

/**
 * @author Lukas Molzberger
 */
public abstract class PrimitiveTerminal implements Terminal {

    protected BiTerminal parent = null;
    protected PrimitiveTransition transition;
    protected State state;
    protected Direction type;

    public PrimitiveTerminal(State state) {
        this.state = state;
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
    public void setType(Direction type) {
        this.type = type;
    }

    @Override
    public Direction getType() {
        return type;
    }

    public void setTransition(PrimitiveTransition transition) {
        this.transition = transition;
    }

    public PrimitiveTransition getTransition() {
        return transition;
    }

    @Override
    public Stream<PrimitiveTerminal> getPrimitiveTerminals() {
        return Stream.of(this);
    }

    public abstract BindingSignal getBindingSignal(FieldOutput bsEvent);

    @Override
    public void propagate(BindingSignal bs, Link l, Activation act) {
        if(bs.getState() != state)
            return;

        if(!transition.isPropagate())
            return;

        bs.propagate(this, l, act);
    }

    @Override
    public boolean matchesState(State s) {
        return s == state;
    }
}
