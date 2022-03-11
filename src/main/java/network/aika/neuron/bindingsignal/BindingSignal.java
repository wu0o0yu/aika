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
import network.aika.fields.BooleanFieldOutput;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * @author Lukas Molzberger
 */
public class BindingSignal<O extends Activation> {

    private BindingSignal<O> parent;
    private Activation activation;
    private Link link;
    private BindingSignal<O> origin;
    private byte depth;
    private State state;

    private BooleanFieldOutput onArrived;


    public BindingSignal(O act, State state) {
        this.origin = this;
        this.activation = act;
        this.depth = 0;
        this.state = state;
    }

    public BindingSignal(BindingSignal<O> parent, State state) {
        this.parent = parent;
        this.origin = parent.getOrigin();
        this.depth = (byte) (getDepth() + 1);
        this.state = state;
    }

    public static Stream<BindingSignal> propagateBindingSignals(Link l, Collection<BindingSignal> bindingSignals) {
        return bindingSignals.stream()
                .map(iBS -> iBS.propagate(l))
                .filter(oBS -> oBS != null);
    }

    public boolean isOrigin() {
        return this == origin;
    }

    public BindingSignal<O> getOrigin() {
        return origin;
    }

    public Activation getActivation() {
        return activation;
    }

    public byte getDepth() {
        return depth;
    }

    public static boolean originEquals(BindingSignal bsA, BindingSignal bsB) {
        return bsA != null && bsB != null && bsA.getOrigin() == bsB.getOrigin();
    }

    public State getState() {
        return state;
    }

    public O getOriginActivation() {
        return (O) origin.getActivation();
    }

    public void link() {
        getActivation().registerBindingSignal(this);
        getOriginActivation().registerReverseBindingSignal(getActivation(), this);
    }

    public boolean exists() {
        BindingSignal existingBS = getActivation().getBindingSignal(getOriginActivation());
        if(existingBS == null)
            return false;

        return existingBS.getState() == state;
    }

    public BindingSignal<O> clone(O act) {
        BindingSignal<O> c = new BindingSignal(parent, state);
        c.activation = act;
        c.link = link;
        return c;
    }

    public boolean checkPropagate() {
        return getActivation().checkPropagateBindingSignal(this);
    }

    protected BindingSignal<O> propagate(Link l) {
        BindingSignal<O> nextBS = l.getSynapse().transition(this, Direction.OUTPUT, true);
        if(nextBS != null) {
            nextBS.activation = l.getOutput();
            nextBS.link = l;
        }

        return nextBS;
    }

    public boolean match(BindingSignal<O> oBS) {
        return state == oBS.state;
    }

    public String toString() {
        return getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ", depth:" + getDepth() + ", state:" + state;
    }

    public Link getLink() {
        return link;
    }
}
