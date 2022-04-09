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
import network.aika.fields.Field;
import network.aika.fields.FieldOutput;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.steps.activation.Linking;

import static network.aika.fields.Fields.mul;
import static network.aika.steps.LinkingOrder.POST_FIRED;
import static network.aika.steps.LinkingOrder.PRE_FIRED;


/**
 * @author Lukas Molzberger
 */
public class BindingSignal<A extends Activation> {

    private BindingSignal parent;
    private A activation;
    private Link link;
    private Transition transition;
    private BindingSignal origin;
    private int depth;
    private State state;
    private boolean propagateAllowed = true;

    private Field onArrived = new Field(this, "onArrived");
    private FieldOutput onArrivedFired;
    FieldOutput onArrivedBound;
    FieldOutput onArrivedBoundFired;

    public BindingSignal(A act, State state) {
        this.origin = this;
        this.activation = act;
        this.depth = 0;
        this.state = state;

        initFields();
    }

    public BindingSignal(BindingSignal parent, State state) {
        this.parent = parent;
        this.origin = parent.getOrigin();
        this.depth = parent.depth + 1;
        this.state = state;
    }

    private void initFields() {
        onArrived.addEventListener(() -> {
            if (!activation.getNeuron().isNetworkInput()) {
                Linking.add(activation, this, PRE_FIRED);
            }
        });

        onArrivedFired = mul(
                "onFired * onArrived",
                activation.getIsFired(),
                onArrived
        );

        onArrivedFired.addEventListener(() ->
                Linking.add(activation, this, POST_FIRED)
        );

        activation.initBSFields(this);
    }

    public Field getOnArrived() {
        return onArrived;
    }

    public FieldOutput getOnArrivedFired() {
        return onArrivedFired;
    }

    public FieldOutput getOnArrivedBound() {
        return onArrivedBound;
    }

    public void setOnArrivedBound(FieldOutput onArrivedBound) {
        this.onArrivedBound = onArrivedBound;
    }

    public FieldOutput getOnArrivedBoundFired() {
        return onArrivedBoundFired;
    }

    public void setOnArrivedBoundFired(FieldOutput f) {
        onArrivedBoundFired = f;
    }

    public boolean isOrigin() {
        return this == origin;
    }

    public BindingSignal getOrigin() {
        return origin;
    }

    public A getActivation() {
        return activation;
    }

    public Link getLink() {
        return link;
    }

    public Transition getTransition() {
        return transition;
    }

    public int getDepth() {
        return depth;
    }

    public static boolean originEquals(BindingSignal bsA, BindingSignal bsB) {
        return bsA != null && bsB != null && bsA.getOrigin() == bsB.getOrigin();
    }

    public State getState() {
        return state;
    }

    public Activation getOriginActivation() {
        return origin.getActivation();
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

    public BindingSignal<A> clone(A act) {
        BindingSignal<A> c = new BindingSignal(parent, state);
        c.activation = act;
        c.link = link;
        c.initFields();
        return c;
    }

    public BindingSignal<A> propagate(Link<?, ?, A> l) {
        Transition t = l.getSynapse().getTransition(
                this,
                Direction.OUTPUT,
                true
        );
        if(t == null)
            return null;

        BindingSignal nextBS = t.next(this);

        nextBS.activation = l.getOutput();
        nextBS.link = l;
        nextBS.transition = t;
        nextBS.propagateAllowed = t.getPropagate() > 1;
        nextBS.initFields();

        return nextBS;
    }

    public boolean isPropagateAllowed() {
        return propagateAllowed;
    }

    public boolean match(BindingSignal<A> oBS) {
        return state == oBS.state;
    }

    public String toString() {
        return getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ", depth:" + getDepth() + ", state:" + state;
    }

}
