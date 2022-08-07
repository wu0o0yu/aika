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
import network.aika.direction.Direction;
import network.aika.fields.Field;
import network.aika.fields.FieldOutput;
import network.aika.fields.QueueField;
import network.aika.fields.SlotField;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;

import java.util.stream.Stream;

import static network.aika.fields.Fields.mul;
import static network.aika.neuron.bindingsignal.BSKey.createKey;
import static network.aika.neuron.bindingsignal.State.INPUT;


/**
 * @author Lukas Molzberger
 */
public class BindingSignal implements Element {

    private BindingSignal parent;
    private Activation activation;
    private Link link;
    private PrimitiveTransition transition;
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
    }

    private BindingSignal(BindingSignal parent) {
        this.parent = parent;
        this.origin = parent.getOrigin();
        this.depth = parent.depth + 1;
    }

    public BindingSignal(BindingSignal parent, State s) {
        this(parent);

        this.state = s;
    }

    public BindingSignal(BindingSignal parent, PrimitiveTerminal fromTerminal) {
        this(parent);

        Direction toDirection = fromTerminal.getType().invert();
        this.transition = fromTerminal.getTransition();
        this.state = toDirection.getTerminal(transition).getState();
    }

    public void init(Activation act) {
        this.activation = act;
        onArrived = new QueueField(this, "arrived", 0.0);
        onArrived.addEventListener(() ->
                activation.receiveBindingSignal(this)
        );

        initFields();
    }

    public void propagate(Link l) {
        propagate(l.getSynapse())
                .forEach(toBS -> {
                            toBS.setLink(l);
                            l.getOutput().addBindingSignal(toBS);
                        }
                );
    }

    public Stream<BindingSignal> propagate(Synapse s) {
        if(depth >= 3)
            return Stream.empty();

        Stream<Transition> transitions = s.getTransitions();
        return transitions
                .flatMap(transition ->
                        transition.getInputTerminals()
                )
                .flatMap(terminal ->
                        terminal.propagate(this)
                );
    }

    public BindingSignal next(PrimitiveTerminal t) {
        return t != null ?
                new BindingSignal(this, t) :
                null;
    }

    public void setLink(Link l) {
        this.link = l;
    }

    private void initFields() {
        if (!activation.getNeuron().isNetworkInput()) {
            if(state == INPUT && activation.getLabel() == null) {
                onArrived.addEventListener(() ->
                        activation.initNeuronLabel(this)
                );
            }
        }

        onArrivedFired = mul(
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

    public Link getLink() {
        return link;
    }

    public PrimitiveTransition getTransition() {
        return transition;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isNetworkInput() {
        return activation != null && activation.isNetworkInput();
    }

    public Field getNorm() {
        return norm;
    }

    public void setNorm(Field norm) {
        this.norm = norm;
    }

    public static boolean originEquals(BindingSignal bsA, BindingSignal bsB) {
        return bsA != null && bsB != null && bsA.getOrigin() == bsB.getOrigin();
    }

    public State getState() {
        return state;
    }

    public PatternActivation getOriginActivation() {
        return (PatternActivation) origin.getActivation();
    }

    public void link() {
        getActivation().registerBindingSignal(this);
        SlotField bsSlot = getActivation().getSlot(getState());
        if(bsSlot != null)
            bsSlot.connect(this);

        getOriginActivation().registerReverseBindingSignal(this);
    }

    public boolean shorterBSExists() {
        BindingSignal existingBS = getActivation().getBindingSignal(createKey(this));
        if(existingBS == null)
            return false;

        return existingBS.getState() == state &&
                existingBS.depth <= depth;
    }

    public boolean isSelfRef(BindingSignal outputBS) {
        if(this == outputBS)
            return true;

        if(parent == null)
            return false;

        return parent.isSelfRef(outputBS);
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
