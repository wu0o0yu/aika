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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractField<R> extends FieldNode implements IField, Writable {

    private static final Logger log = LoggerFactory.getLogger(AbstractField.class);

    protected Double currentValue;
    protected Double update;
    private boolean withinUpdate;

    private R reference;
    private String label;

    public AbstractField(R reference, String label) {
        this.reference = reference;
        this.label = label;
    }

    public AbstractField(R reference, String label, double initialValue) {
        this(reference, label);

        currentValue = initialValue;
    }

    public AbstractField(R reference, String label, FieldOnTrueEvent fieldListener) {
        this(reference, label);
        addEventListener(fieldListener);
    }

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

    @Override
    public double getCurrentValue() {
        if(!isInitialized())
            throw new IllegalStateException("getCurrentValue was called on an uninitialized field");

        return currentValue;
    }

    public void setValue(double v) {
        if(isInitialized()) {
            update = v - currentValue;
            if(!checkPreCondition(currentValue, v, v - currentValue))
                return;
        } else {
            update = v;
        }

        triggerUpdate();
    }

    protected abstract boolean checkPreCondition(Double cv, double nv, double u);

    @Override
    public void receiveUpdate(int arg, double inputCV, double u) {
        receiveUpdate(u);
    }

    public void receiveUpdate(double u) {
        assert !withinUpdate;

        if(update == null)
            update = u;
        else
            update += u;

        if(!isInitialized() || checkPreCondition(
                currentValue,
                currentValue + update,
                update)) {
            triggerUpdate();
        }
    }

    public void triggerUpdate() {
        triggerInternal();
    }

    protected void triggerInternal() {
        withinUpdate = true;
        if(update != null) {
            double cv = currentValue != null ? currentValue : 0.0;
            performUpdate();
            propagateUpdate(cv, update);
            update = null;
        }
        withinUpdate = false;
    }

    @Override
    public boolean isInitialized() {
        return currentValue != null;
    }

    private void performUpdate() {
        if (isInitialized())
            currentValue += update;
        else
            currentValue = update;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(isInitialized());
        if(isInitialized())
            out.writeDouble(currentValue);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        if(in.readBoolean())
            currentValue = in.readDouble();

        update = null;
    }

    public String getValueString() {
        if(!isInitialized())
            return "--";

        return "[u:" + (update != null ? Utils.round(update) : "--") + ", " +
                "v:" + (currentValue != null ? Utils.round(currentValue) : "--") + "]";
    }

    @Override
    public String toString() {
        String refStr = getReference() != null ?
                " (Ref: " + getReference().getClass().getSimpleName() + " " + getReference() + ")" :
                "";
        return getLabel() + ":" + getValueString() + refStr;
    }
}
