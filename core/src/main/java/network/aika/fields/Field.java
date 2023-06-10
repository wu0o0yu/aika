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

import network.aika.Model;
import network.aika.Thought;
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static network.aika.fields.ListenerFieldLink.createEventListener;
import static network.aika.utils.Utils.doubleToString;


/**
 * @author Lukas Molzberger
 */
public abstract class Field implements FieldInput, FieldOutput, Writable {

    private static double MIN_TOLERANCE = 0.0000000001;

    private String label;
    private FieldObject reference;

    protected double value;

    private boolean withinUpdate;
    private double updatedValue;

    private Collection<AbstractFieldLink> receivers;

    protected Double tolerance;

    public Field(FieldObject reference, String label, Double tolerance) {
        this(reference, label, tolerance, false);
    }

    public Field(FieldObject reference, String label, Double tolerance, boolean weakRefs) {
        this.reference = reference;
        this.label = label;
        this.tolerance = tolerance;

        initIO(weakRefs);
    }

    public Field addListener(String listenerName, FieldOnTrueEvent fieldListener) {
        ListenerFieldLink fl = createEventListener(this, listenerName, fieldListener);
        addOutput(fl);
        fl.connect(true);

        return this;
    }

    public int getRound() {
        Thought t = reference.getThought();

        if(t == null)
            return 0;

        return t.getRound();
    }

    protected void initIO(boolean weakRefs) {
        receivers = new ArrayList<>();
    }

    public Field setInitialValue(double initialValue) {
        value = initialValue;
        return this;
    }

    public void setValue(double v) {
        triggerUpdate(0, v - value);
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
    public double getValue() {
        return value;
    }

    @Override
    public double getUpdatedValue() {
        return withinUpdate ?
                updatedValue :
                value;
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

    public void disconnectOutputs(boolean deinitialize) {
        getReceivers().forEach(fl ->
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

    public void receiveUpdate(AbstractFieldLink fl, int r, double u) {
        receiveUpdate(
                r,
                u
        );
    }

    public void receiveUpdate(int r, double u) {
        assert !withinUpdate;

        triggerUpdate(r, u);
    }


    public void triggerUpdate(int r, double u) {
        if(Utils.belowTolerance(tolerance, u))
            return;

        withinUpdate = true;

        updatedValue = value + u;
        if(updatedValue > -MIN_TOLERANCE && updatedValue < MIN_TOLERANCE) {
            updatedValue = 0.0; // TODO: Find a better solution to this hack
        }

        propagateUpdate(r, u);
        value = updatedValue;

        withinUpdate = false;
    }

    protected void propagateUpdate(int r, double update) {
        AbstractFieldLink[] recs = receivers.toArray(new AbstractFieldLink[0]);

        for(int i = 0; i < recs.length; i++) {
            recs[i].receiveUpdate(r, update);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(value);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        value = in.readDouble();
    }

    @Override
    public String toString() {
        return getLabel() + ":" + getValueString();
    }

    public String getValueString() {
        return doubleToString(getValue());
    }
}
