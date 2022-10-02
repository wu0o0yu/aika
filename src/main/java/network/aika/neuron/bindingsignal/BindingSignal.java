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

import network.aika.Config;
import network.aika.Thought;
import network.aika.fields.Field;
import network.aika.fields.FieldOutput;
import network.aika.fields.QueueField;
import network.aika.fields.SlotField;
import network.aika.neuron.activation.*;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.fields.Fields.mul;
import static network.aika.neuron.bindingsignal.State.INPUT;


/**
 * @author Lukas Molzberger
 */
public class BindingSignal implements Element {

    private Map<Link, BindingSignal> parents = new TreeMap<>(
            Comparator.comparing(l -> l.getInput())
    );
    private Activation activation;
    private BindingSignal origin;
    private int depth;
    private State state;

    private QueueField onArrived;
    private FieldOutput onArrivedFired;

    private Field norm;

    public BindingSignal(PatternActivation act, State state) {
        this.origin = this;
        this.depth = 0;
        this.state = state;
        this.activation = act;

        link();
    }

    public BindingSignal(BindingSignal parent, State s, Activation act) {
        this.origin = parent.getOrigin();
        this.depth = parent.depth + 1;
        this.state = s;
        this.activation = act;

        link();
    }

    public void addParent(Link l, BindingSignal parent) {
        parents.put(l, parent);
    }

    public BindingSignal getParent(Link l) {
        return parents.get(l);
    }

    public Map<Link, BindingSignal> getParents() {
        return parents;
    }

    public void propagate(Link l) {
        Stream<Transition> transitions = l.getSynapse().getTransitions();
        transitions
                .flatMap(transition ->
                        transition.getInputTerminals()
                )
                .forEach(terminal ->
                        terminal.propagate(this, l, l.getOutput())
                );
    }

    private void initFields() {
        onArrived = new QueueField(this, "arrived", 0.0);
        onArrived.addEventListener(() ->
                getActivation().receiveBindingSignal(this)
        );

        if (!activation.getNeuron().isNetworkInput() &&
                state == INPUT &&
                activation.getLabel() == null) {
            onArrived.addEventListener(() ->
                    activation.initNeuronLabel(this)
            );
        }

        onArrivedFired = mul(
                this,
                "onFired * onArrived",
                activation.getIsFired(),
                onArrived
        );

        onArrivedFired.addEventListener(() ->
                getActivation().propagateBindingSignal(this)
        );
    }

    public Field getOnArrived() {
        return onArrived;
    }

    public FieldOutput getOnArrivedFired() {
        return onArrivedFired;
    }

    public boolean isOrigin() {
        return this == origin;
    }

    public BindingSignal getOrigin() {
        return origin;
    }

    public Activation getActivation() {
        return activation;
    }

    public int getDepth() {
        return depth;
    }

    public State getState() {
        return state;
    }

    public PatternActivation getOriginActivation() {
        return (PatternActivation) origin.getActivation();
    }

    public void link() {
        initFields();

        getActivation().registerBindingSignal(this);
        SlotField bsSlot = getActivation().getSlot(getState());
        if(bsSlot != null)
            bsSlot.connect(this);

        getOriginActivation().registerReverseBindingSignal(this);
    }

    public boolean isSelfRef(BindingSignal outputBS) {
        if(this == outputBS)
            return true;

        if(parents == null)
            return false;

        return parents.values().stream()
                .anyMatch(p ->
                        p.isSelfRef(outputBS)
                );
    }

    @Override
    public Timestamp getCreated() {
        return getOriginActivation().getCreated();
    }

    @Override
    public Timestamp getFired() {
        return getOriginActivation().getFired();
    }

    @Override
    public Thought getThought() {
        return activation.getThought();
    }

    @Override
    public Config getConfig() {
        return activation.getConfig();
    }

    @Override
    public String toString() {
        return getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ", depth:" + getDepth() + ", state:" + state;
    }
}
