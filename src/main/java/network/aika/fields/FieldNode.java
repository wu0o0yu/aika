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
import network.aika.neuron.activation.Element;
import network.aika.utils.Utils;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Lukas Molzberger
 */
public abstract class FieldNode<R extends Element> implements FieldOutput {

    private String label;
    private R reference;

    private List<FieldLink> receivers = new ArrayList<>();

    public FieldNode(R reference, String label) {
        this.reference = reference;
        this.label = label;
    }

    @Override
    public R getReference() {
        return reference;
    }

    public void setReference(R reference) {
        this.reference = reference;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public abstract double getCurrentValue();

    public abstract boolean isInitialized();

    public List<FieldLink> getReceivers() {
        return receivers;
    }

    public void addInitialCurrentValue(FieldLink fl, UpdateListener listener) {
        if(isInitialized() && !Utils.belowTolerance(getCurrentValue()))
            propagateUpdate(fl, listener, 0.0, getCurrentValue());
    }

    public void removeFinalCurrentValue(FieldLink fl, UpdateListener listener) {
        if(isInitialized() && !Utils.belowTolerance(getCurrentValue()))
            propagateUpdate(fl, listener, getCurrentValue(), -getCurrentValue());
    }

    @Override
    public void addOutput(FieldLink fl, boolean propagateInitValue) {
        this.receivers.add(fl);
        if(propagateInitValue)
            addInitialCurrentValue(fl, fl.getOutput());
    }

    @Override
    public void removeOutput(FieldLink fl, boolean propagateFinalValue) {
        if(propagateFinalValue)
            removeFinalCurrentValue(fl, fl.getOutput());
        this.receivers.remove(fl);
    }

    protected void propagateUpdate(double cv, double update) {
        int i = 0;
        while(i < receivers.size()) {
            FieldLink fl = receivers.get(i++);
            fl.getOutput().receiveUpdate(fl, cv, update);
        }
    }

    protected void propagateUpdate(FieldLink fl, UpdateListener listener, double cv, double update) {
        listener.receiveUpdate(fl, cv, update);
    }

    @Override
    public void disconnect() {
        receivers.stream()
                .filter(l -> l.getOutput() instanceof FieldInput)
                .forEach(l -> ((FieldInput) l.getOutput()).removeInput(l));
        receivers.clear();
    }
}
