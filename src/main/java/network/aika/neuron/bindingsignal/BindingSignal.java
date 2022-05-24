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
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Timestamp;

import java.util.stream.Stream;

import static network.aika.fields.Fields.mul;


/**
 * @author Lukas Molzberger
 */
public class BindingSignal<A extends Activation> implements Element {

    private BindingSignal parent;
    private A activation;
    private Link link;
    private SingleTransition transition;
    private BindingSignal origin;
    private int depth;
    private State state;

    private Field onArrived;
    private FieldOutput onArrivedFired;
    private FieldOutput onArrivedFinal;
    private FieldOutput onArrivedFiredFinal;

    public BindingSignal(A act, State state) {
        this.origin = this;
        this.depth = 0;
        this.state = state;

        init(act);
    }

    private BindingSignal(BindingSignal parent) {
        this.parent = parent;
        this.origin = parent.getOrigin();
        this.depth = parent.depth + 1;
    }

    private BindingSignal(BindingSignal parent, State state) {
        this(parent);
        this.state = state;
    }

    public BindingSignal(BindingSignal parent, SingleTransition t) {
        this(parent);

        this.transition = t;
        this.state = t.next(Direction.OUTPUT);
    }

    public void init(A act) {
        this.activation = act;
        onArrived = new QueueField(this, "arrived", 0.0);

        initFields();
//        initBSListeners();

        activation.receiveBindingSignal(this);
    }

    public SingleTransition transition(Synapse s) {
        Stream<Transition> transitions = s.getTransitions();
        return transitions
                .flatMap(t -> t.getBSPropagateTransitions())
                .findFirst()
                .orElse(null);
    }

    public void propagate(Link l) {
        SingleTransition t = transition(l.getSynapse());
        if(t == null)
            return;

        BindingSignal toBS = next(t);
        toBS.setLink(l);

        Activation oAct = l.getOutput();
        toBS.init(oAct);
        oAct.addBindingSignal(toBS);
    }

    public BindingSignal propagate(Synapse s) {
        return next(transition(s));
    }

    public BindingSignal next(SingleTransition t) {
        return t != null ?
                new BindingSignal(this, t) :
                null;
    }

    public void setLink(Link l) {
        this.link = l;
    }

    private void initFields() {
        if (!activation.getNeuron().isNetworkInput()) {

            if(state == State.INPUT && activation.getLabel() == null) {
                onArrived.addEventListener(() ->
                        activation.getNeuron().setLabel(
                                activation.getConfig().getLabel(this)
                        )
                );
            }
        }

        onArrivedFired = mul(
                "onFired * onArrived",
                activation.getIsFired(),
                onArrived
        );

        onArrivedFinal = mul(
                "onFinal * onArrived",
                activation.getIsFinal(),
                onArrived
        );

        onArrivedFiredFinal = mul(
                "onFired * onArrived * isFinal",
                onArrivedFired,
                activation.getIsFinal()
        );

        onArrivedFired.addEventListener(() ->
                getActivation().propagateBindingSignal(this)
        );
    }
/*
   wird durch fixed und dyn. BS abgelöst
    private void initBSListeners() {
        Neuron<?, ?> n = activation.getNeuron();

        boolean templateEnabled = activation.getConfig().isTemplatesEnabled();
        for(Direction dir: DIRECTIONS)
            n.getTargetSynapses(dir, templateEnabled)
                    .forEach(s ->
                            s.registerLinkingEvents(this, dir)
                    );
    }
*/
    public Field getOnArrived() {
        return onArrived;
    }

    public FieldOutput getEvent(boolean isFired, boolean isFinal) {
        if(isFired && isFinal)
            return onArrivedFiredFinal;

        if(isFired)
            return onArrivedFired;

        if(isFinal)
            return onArrivedFinal;

        return onArrived;
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

    public SingleTransition getTransition() {
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

    public boolean isSelfRef(BindingSignal outputBS) {
        if(this == outputBS)
            return true;

        if(parent == null)
            return false;

        return parent.isSelfRef(outputBS);
    }

    public String toString() {
        return getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ", depth:" + getDepth() + ", state:" + state;
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
}
