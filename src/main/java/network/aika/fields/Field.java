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
import network.aika.neuron.activation.BindingActivation;
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
public class Field extends FieldListener implements FieldInterface, Writable {

    private static final Logger log = LoggerFactory.getLogger(Field.class);

    private Double currentValue;
    private Double update;
    private boolean allowUpdate;
    private Object refObj;
    private String label;

    private PropagatePreCondition propagatePreCondition;

    public Field(Object refObj, String label) {
        this.refObj = refObj;
        this.label = label;
        this.propagatePreCondition = (cv, nv, u) -> !Utils.belowTolerance(u);
    }

    public Field(Object refObj, String label, FieldUpdateEvent fieldListener) {
        this(refObj, label);
        addFieldListener(label, fieldListener);
    }

    public Object getRefObj() {
        return refObj;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public PropagatePreCondition getPropagatePreCondition() {
        return propagatePreCondition;
    }

    public void setPropagatePreCondition(PropagatePreCondition propagatePreCondition) {
        this.propagatePreCondition = propagatePreCondition;
    }

    @Override
    public void propagateInitialValue(FieldUpdateEvent listener) {
        propagateUpdate(listener, getCurrentValue());
    }

    @Override
    public double getCurrentValue() {
        if(!isInitialized())
            throw new IllegalStateException("getCurrentValue was called on an uninitialized field");

        return currentValue;
    }

    @Override
    public double getNewValue() {
        if(!allowUpdate)
            throw new IllegalStateException("getNewValue was called outside the listener");

        if(updateAvailable())
            return isInitialized() ? currentValue + update : update;
        else {
            return getCurrentValue();
        }
    }

    @Override
    public boolean set(double v) {
        if(isInitialized()) {
            update = v - currentValue;
            return propagatePreCondition.check(currentValue, v, v - currentValue);
        } else {
            update = v;
            return true;
        }
    }

    @Override
    public boolean add(double u) {
        if(update == null)
            update = u;
        else
            update += u;

        return !isInitialized() || propagatePreCondition.check(
                currentValue,
                currentValue + update,
                update);
    }

    @Override
    public void triggerUpdate() {
        triggerInternal();
    }

    protected void triggerInternal() {
        allowUpdate = true;
        if(updateAvailable())
            propagateUpdate(update);
        acknowledgePropagated();
        allowUpdate = false;
    }

    @Override
    public boolean updateAvailable() {
        return update != null;
    }

    @Override
    public double getUpdate() {
        if(!allowUpdate)
            throw new IllegalStateException("getNewValue was called outside the listener");

        return update;
    }

    @Override
    public boolean isInitialized() {
        return currentValue != null;
    }

    private void acknowledgePropagated() {
        if (update == null)
            return;

        assert allowUpdate;
        if (isInitialized())
            currentValue += update;
        else
            currentValue = update;

        update = null;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(currentValue);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        currentValue = in.readDouble();
        update = null;
    }

    @Override
    public String toString() {
        if(!isInitialized())
            return "--";

        return "[u:" + (update != null ? Utils.round(update) : "--") + ", " +
                "v:" + (currentValue != null ? Utils.round(currentValue) : "--") + "]";
    }
}
