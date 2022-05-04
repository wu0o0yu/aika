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
package network.aika.fields;

import network.aika.direction.Direction;
import network.aika.neuron.Synapse;
import network.aika.neuron.bindingsignal.BiTransition;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.Transition;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Lukas Molzberger
 */
public abstract class FieldNode implements FieldOutput {

    private List<FieldLink> receivers = new ArrayList<>();

    public abstract double getCurrentValue();

    public abstract boolean isInitialized();

    public void addInitialCurrentValue(int arg, UpdateListener listener) {
        if(isInitialized())
            propagateUpdate(arg, listener, getCurrentValue());
    }

    public void removeFinalCurrentValue(int arg, UpdateListener listener) {
        if(isInitialized())
            propagateUpdate(arg, listener, -getCurrentValue());
    }

    @Override
    public void addOutput(FieldLink l, boolean propagateInitValue) {
        this.receivers.add(l);
        if(propagateInitValue)
            addInitialCurrentValue(l.getArgument(), l.getOutput());
    }

    @Override
    public void removeOutput(FieldLink l, boolean propagateFinalValue) {
        if(propagateFinalValue)
            removeFinalCurrentValue(l.getArgument(), l.getOutput());
        this.receivers.remove(l);
    }

    @Override
    public void addEventListener(FieldOnTrueEvent eventListener) {
        addOutput(
                new FieldLink(
                        null,
                        0,
                        (arg, u) -> {
                            if (u > 0.0)
                                eventListener.onTrue();
                        }
                ),
                true
        );
    }

    @Override
    public void addLinkingEventListener(BindingSignal bs, Synapse ts, Direction dir, Transition t) {
        addOutput(
                new FieldLink(
                        null,
                        0,
                        (arg, u) -> {
                            if (u > 0.0)
                                ts.link(dir, bs, t);
                        }
                ),
                true
        );
    }

    @Override
    public void addBiTransitionEventListener(BindingSignal bs, Direction dir, BiTransition t) {
        addOutput(
                new FieldLink(
                        null,
                        0,
                        (arg, u) -> {
                            if (u > 0.0)
                                t.register(bs, dir);
                        }
                ),
                true
        );
    }


    protected void propagateUpdate(double update) {
        int i = 0;
        while(i < receivers.size()) {
            FieldLink l = receivers.get(i++);
            l.getOutput().receiveUpdate(l.getArgument(), update);
        }
    }

    protected void propagateUpdate(int arg, UpdateListener listener, double update) {
        listener.receiveUpdate(arg, update);
    }

    @Override
    public void disconnect() {
        receivers.stream()
                .filter(l -> l.getOutput() instanceof FieldInput)
                .forEach(l -> ((FieldInput) l.getOutput()).removeInput(l));
        receivers.clear();
    }
}
