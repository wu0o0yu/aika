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

    public static int FIRST_ROUND = 0;
    public static int MAX_ROUNDS = 20;

    private static double MIN_TOLERANCE = 0.0000000001;

    private String label;
    private FieldObject reference;

    protected int lastRound = -1;
    protected Double[] value;

    private boolean withinUpdate;
    private double updatedValue;

    private Collection<AbstractFieldLink> receivers;

    protected Double tolerance;

    public Field(FieldObject reference, String label, Double tolerance) {
        this(reference, label, MAX_ROUNDS, tolerance, false);
    }

    public Field(FieldObject reference, String label, int maxRounds, Double tolerance, boolean weakRefs) {
        this.reference = reference;
        this.label = label;
        this.tolerance = tolerance;

        value = new Double[maxRounds];

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

    public Field setInitialValue(int r, double initialValue) {
        value[r] = initialValue;
        lastRound = Math.max(r, lastRound);
        return this;
    }

    public void setValue(int r, double v) {
        Double ov = value[r];
        triggerUpdate(r, ov != null ? v - value[r] : v);
    }

    @Override
    public FieldObject getReference() {
        return reference;
    }

    public void setReference(FieldObject reference) {
        this.reference = reference;
    }

    @Override
    public int getLastRound() {
        return lastRound;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Double getLastValue() {
        return getValue(lastRound);
    }

    @Override
    public Double getValue(int r) {
        r = checkRound(r);

        if(r < 0 || r > lastRound)
            return null;

        return value[r];
    }

    @Override
    public double getValue(int r, double defaultValue) {
        r = checkRound(r);

        if(r < 0 || r > lastRound || value[r] == null)
            return defaultValue;

        return value[r];
    }

    @Override
    public Double getUpdatedValue(int r) {
        r = checkRound(r);

        if(r < 0 || r > lastRound)
            return null;

        return withinUpdate ?
                updatedValue :
                value[r];
    }

    @Override
    public Double getUpdatedLastValue() {
        return getUpdatedValue(lastRound);
    }

    private int checkRound(int r) {
        if(r == MAX_VALUE)
            r = lastRound;

        r = Math.min(r, lastRound);
        return r;
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
        if(r > 0 && Utils.belowTolerance(tolerance, u))
            return;

        withinUpdate = true;

        updatedValue = getValue(r, 0.0) + u;
        lastRound = r;
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
        return doubleToString(getLastValue());
    }
}
