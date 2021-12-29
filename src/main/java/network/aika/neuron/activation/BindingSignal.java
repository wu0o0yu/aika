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

import java.util.Collection;
import java.util.stream.Stream;

import static network.aika.direction.Direction.OUTPUT;

/**
 * @author Lukas Molzberger
 */
public class BindingSignal {

    private Activation activation;
    private BindingSignal origin;
    private byte scope;
    private byte depth;

    public BindingSignal(Activation act, byte scope) {
        this.origin = this;
        this.activation = act;
        this.scope = scope;
        this.depth = 0;
    }

    public BindingSignal(BindingSignal parent, Activation activation, byte scope) {
        this.origin = parent.getOrigin();
        this.activation = activation;
        this.scope = scope;
        this.depth = (byte) (getDepth() + 1);
    }

    public static Stream<BindingSignal> propagateBindingSignals(Link l, Collection<BindingSignal> bindingSignals) {
        return bindingSignals.stream()
                .map(iBS -> l.getSynapse().propagateBindingSignal(l, iBS))
                .filter(oBS -> oBS != null)
                .filter(oBS -> !l.getOutput().checkIfBindingSignalExists(oBS));
    }

    public void link() {
        activation.bindingSignals.put(getOriginActivation(), this);
        getOriginActivation().registerBindingSignal(activation, this);
    }

    public BindingSignal getOrigin() {
        return origin;
    }

    public Activation<?> getOriginActivation() {
        return origin.getActivation();
    }

    public Activation getActivation() {
        return activation;
    }

    public byte getScope() {
        return scope;
    }

    public byte getDepth() {
        return depth;
    }

    public String toString() {
        return "[" + getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ",s:" + scope + ",d:" + depth + "]";
    }
}
