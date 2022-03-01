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
import network.aika.neuron.activation.Link;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Molzberger
 */
public class Field extends FieldListener implements FieldInput, FieldOutput, Writable {

    private static final Logger log = LoggerFactory.getLogger(Field.class);

    private double currentValue = 0.0;
    private Double update;
    private boolean allowUpdate;
    private String label;

    private PropagatePreCondition propagatePreCondition;

    public Field(String label) {
        this.label = label;
        this.propagatePreCondition = (cv, nv, u) -> !Utils.belowTolerance(u);
    }

    public Field(String label, FieldUpdateEvent fieldListener) {
        this(label);
        addFieldListener(label, fieldListener);
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
    public void propagateInitialValue() {
        propagateUpdate(getCurrentValue());
    }

    @Override
    public double getCurrentValue() {
        return currentValue;
    }

    @Override
    public double getNewValue() {
        if(!allowUpdate)
            throw new IllegalStateException("getNewValue was called outside the listener");

        if(updateAvailable())
            return currentValue + update;
        else
            return currentValue;
    }

    @Override
    public boolean set(double v) {
        update = v - currentValue;

        return propagatePreCondition.check(currentValue, v, v - currentValue);
    }

    @Override
    public boolean add(double u) {
        if(update == null)
            update = u;
        else
            update += u;

        return propagatePreCondition.check(currentValue, currentValue + update, update);
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

    private void acknowledgePropagated() {
        if(update == null)
            return;

        assert allowUpdate;
        currentValue += update;
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

    public String toString() {
        return "[u:" + (update != null ? Utils.round(update) : "--") + ", v:" + Utils.round(currentValue) + "]";
    }
}
