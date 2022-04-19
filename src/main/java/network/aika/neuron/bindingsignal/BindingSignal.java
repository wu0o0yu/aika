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
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Fields.mul;


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
    private FieldOutput onArrivedFinal;
    private FieldOutput onArrivedFiredFinal;
    FieldOutput onArrivedBound;
    FieldOutput onArrivedBoundFired;
    FieldOutput onArrivedBoundFiredFinal;

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

    public BindingSignal(BindingSignal parent, Transition t) {
        this(parent);

        this.transition = t;
        this.propagateAllowed = t.getPropagate() > 1;
        this.state = t.next(Direction.OUTPUT);
    }

    public void init(A act) {
        this.activation = act;
        initFields();
        initLinkingEvents();
    }

    public BindingSignal<A> clone(A act) {
        BindingSignal clonedBS = new BindingSignal(parent, state);
        clonedBS.init(act);
        clonedBS.setLink(link); // TODO: wrong link
        return clonedBS;
    }

    public BindingSignal propagate(Synapse s) {
        Transition t = s.getTransition(
                this,
                Direction.OUTPUT,
                true
        );
        if(t == null)
            return null;

        return new BindingSignal(this, t);
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

        activation.initBSFields(this);
    }

    private void initLinkingEvents() {
        Neuron<?, ?> n = activation.getNeuron();

        boolean templateEnabled = activation.getConfig().isTemplatesEnabled();
        n.getTargetSynapses(INPUT, templateEnabled)
                .forEach(s -> s.addInputLinkingEvents(this));

        n.getTargetSynapses(OUTPUT, templateEnabled)
                .forEach(s -> s.addOutputLinkingEvents(this));
    }


    public Field getOnArrived() {
        return onArrived;
    }

    public FieldOutput getOnArrivedFired() {
        return onArrivedFired;
    }

    public FieldOutput getOnArrivedFinal() {
        return onArrivedFinal;
    }

    public FieldOutput getOnArrivedFiredFinal() {
        return onArrivedFiredFinal;
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

    public FieldOutput getOnArrivedBoundFiredFinal() {
        return onArrivedBoundFiredFinal;
    }

    public void setOnArrivedBoundFiredFinal(FieldOutput f) {
        onArrivedBoundFiredFinal = f;
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

    public void setLink(Link l) {
        this.link = l;
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

    public Stream<BindingSignal<?>> getRelatedBindingSignal(Synapse targetSynapse, Neuron toNeuron) {
        Activation originAct = getOriginActivation();
        Stream<BindingSignal<?>> relatedBindingSignals = originAct.getReverseBindingSignals(toNeuron);

        if(targetSynapse.allowLooseLinking()) {
            relatedBindingSignals = Stream.concat(
                    relatedBindingSignals,
                    originAct.getThought().getLooselyRelatedBindingSignals(this, targetSynapse.getLooseLinkingRange(), toNeuron)
            );
        }

        return relatedBindingSignals;
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

    public boolean isPropagateAllowed() {
        return propagateAllowed;
    }

    public String toString() {
        return getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ", depth:" + getDepth() + ", state:" + state;
    }
}
