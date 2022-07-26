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


import network.aika.callbacks.UpdateListener;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Lukas Molzberger
 */
public abstract class FieldNode implements FieldOutput {

    private List<FieldLink> receivers = new ArrayList<>();

    public abstract double getCurrentValue();

    public abstract boolean isInitialized();

    public List<FieldLink> getReceivers() {
        return receivers;
    }

    public void addInitialCurrentValue(int arg, UpdateListener listener) {
        if(isInitialized())
            propagateUpdate(arg, listener, 0.0, getCurrentValue());
    }

    public void removeFinalCurrentValue(int arg, UpdateListener listener) {
        if(isInitialized())
            propagateUpdate(arg, listener, getCurrentValue(), -getCurrentValue());
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
        addUpdateListener((arg, cv, u) -> {
                    if (u > 0.0)
                        eventListener.onTrue();
                }
        );
    }

    @Override
    public void addUpdateListener(UpdateListener updateListener) {
        addOutput(
                new FieldLink(
                        null,
                        0,
                        updateListener
                ),
                true
        );
    }

    protected void propagateUpdate(double cv, double update) {
        int i = 0;
        while(i < receivers.size()) {
            FieldLink l = receivers.get(i++);
            l.getOutput().receiveUpdate(l.getArgument(), cv, update);
        }
    }

    protected void propagateUpdate(int arg, UpdateListener listener, double cv, double update) {
        listener.receiveUpdate(arg, cv, update);
    }

    @Override
    public void disconnect() {
        receivers.stream()
                .filter(l -> l.getOutput() instanceof FieldInput)
                .forEach(l -> ((FieldInput) l.getOutput()).removeInput(l));
        receivers.clear();
    }
}
