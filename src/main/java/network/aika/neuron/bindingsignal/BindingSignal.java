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
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * @author Lukas Molzberger
 */
public abstract class BindingSignal<B extends BindingSignal> {

    protected BindingSignal parent;
    protected Activation activation;
    protected B origin;
    protected byte depth;

    public static Stream<BindingSignal> transitionBindingSignals(Link l, Collection<BindingSignal> bindingSignals) {
        return bindingSignals.stream()
                .map(iBS -> iBS.propagate(l))
                .filter(oBS -> oBS != null);
    }

    public abstract boolean checkPropagate();

    protected abstract BindingSignal propagate(Link l);

    public abstract boolean checkRelatedBindingSignal(Synapse s, BindingSignal outputBS, Activation iAct, Activation oAct);

    public abstract boolean exists();

    public abstract void link();

    public B getOrigin() {
        return origin;
    }

    public abstract Activation<?> getOriginActivation();

    public Activation<?> getActivation() {
        return activation;
    }

    public byte getDepth() {
        return depth;
    }
}
