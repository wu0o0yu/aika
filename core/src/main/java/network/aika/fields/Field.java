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
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.fields.ListenerFieldLink.createEventListener;
import static network.aika.utils.Utils.doubleToString;


/**
 * @author Lukas Molzberger
 */
public abstract class Field implements FieldInput, FieldOutput, Writable {

    public static int MAX_ROUNDS = 20;

    private static double MIN_TOLERANCE = 0.0000000001;

    private String label;
    private FieldObject reference;

    protected int lastRound = 0;
    protected double[] value = new double[MAX_ROUNDS];

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

    protected void initIO(boolean weakRefs) {
        receivers = new ArrayList<>();
    }

    public Field setInitialValue(double initialValue) {
        value[0] = initialValue;
        return this;
    }

    public void setValue(int r, double v) {
        triggerUpdate(r, v - value[r]);
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
    public double getValue(int r) {
        if(r < 0)
            return 1.0;

        if(r == MAX_VALUE)
            r = lastRound;

        return value[r];
    }

    @Override
    public double getUpdatedValue(int r) {
        if (r < 0)
            return 1.0;

        if (r == MAX_VALUE)
            r = lastRound;

        return withinUpdate ?
                updatedValue :
                value[r];
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

        lastRound = r;

        withinUpdate = true;

        updatedValue = value[r] + u;
        if(updatedValue > -MIN_TOLERANCE && updatedValue < MIN_TOLERANCE) {
            updatedValue = 0.0; // TODO: Find a better solution to this hack
        }

        propagateUpdate(r, u);
        value[r] = updatedValue;

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
        out.writeInt(value.length);
        for(int i = 0; i < value.length; i++)
            out.writeDouble(value[i]);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        int l = in.readInt();
        for(int i = 0; i < l; i++)
            value[i] = in.readDouble();
    }

    @Override
    public String toString() {
        return getLabel() + ":" + getValueString();
    }

    public String getValueString() {
        return doubleToString(getValue(MAX_VALUE));
    }
}
