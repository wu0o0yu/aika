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

import network.aika.FieldObject;
import network.aika.Model;
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static network.aika.fields.ListenerFieldLink.createEventListener;


/**
 * @author Lukas Molzberger
 */
public abstract class Field implements FieldInput, FieldOutput, Writable {

    private String label;
    private FieldObject reference;

    protected double currentValue;
    protected double newValue;

    protected boolean withinUpdate;

    private Collection<AbstractFieldLink> receivers;

    protected Double tolerance;

    public Field(FieldObject reference, String label, Double tolerance) {
        this(reference, label, tolerance, false);
    }

    public Field(FieldObject reference, String label, Double tolerance, boolean weakRefs) {
        this.reference = reference;
        this.label = label;
        this.tolerance = tolerance;

        if(reference != null)
            reference.register(this);

        initIO(weakRefs);
    }

    public Field addListener(String listenerName, FieldOnTrueEvent fieldListener) {
        addOutput(createEventListener(this, listenerName, fieldListener));
        return this;
    }

    protected void initIO(boolean weakRefs) {
        receivers = new ArrayList<>();
    }

    public Field setInitialValue(double initialValue) {
        currentValue = initialValue;
        newValue = initialValue;
        return this;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public void setNewValue(double newValue) {
        this.newValue = newValue;
    }

    public void setValue(double v) {
        newValue = v;

        triggerUpdate();
    }

    @Override
    public FieldObject getReference() {
        return reference;
    }

    public void setReference(FieldObject reference) {
        this.reference = reference;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public double getCurrentValue() {
        return currentValue;
    }

    @Override
    public double getUpdatedCurrentValue() {
        return withinUpdate ?
                newValue :
                currentValue;
    }

    @Override
    public double getNewValue() {
        return newValue;
    }

    @Override
    public void copyState(Field to) {
        to.currentValue = getCurrentValue();
        to.newValue = getNewValue();
    }

    public void connectInputs(boolean initialize) {
        getInputs().forEach(fl ->
                fl.connect(initialize)
        );
    }

    public void disconnectInputs(boolean deinitialize) {
        getInputs().forEach(fl ->
                fl.disconnect(deinitialize)
        );
    }

    public Collection<AbstractFieldLink> getReceivers() {
        return receivers;
    }

    @Override
    public void addOutput(AbstractFieldLink fl) {
        this.receivers.add(fl);
    }

    @Override
    public void removeOutput(AbstractFieldLink fl) {
        this.receivers.remove(fl);
    }

    public void receiveUpdate(AbstractFieldLink fl, double u) {
        receiveUpdate(u);
    }

    public void receiveUpdate(double u) {
        assert !withinUpdate;

        newValue += u;
        triggerUpdate();
    }

    public void triggerUpdate() {
        if(Utils.belowTolerance(tolerance, newValue - currentValue))
            return;

        triggerInternal();
    }

    protected void triggerInternal() {
        withinUpdate = true;

        propagateUpdate(newValue - currentValue);
        currentValue = newValue;

        withinUpdate = false;
    }

    protected void propagateUpdate(double update) {
        AbstractFieldLink[] recs = receivers.toArray(new AbstractFieldLink[0]);

        for(int i = 0; i < recs.length; i++) {
            recs[i].receiveUpdate(update);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(currentValue);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        currentValue = in.readDouble();
    }

    @Override
    public String toString() {
        return getLabel() + ":" + getValueString();
    }

    public String getValueString() {
        return "[ov:" + Utils.round(getCurrentValue()) + " nv:" + Utils.round(getNewValue()) + "]";
    }
}
