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
public class Field implements FieldInput, FieldOutput, Writable {

    private static final Logger log = LoggerFactory.getLogger(Field.class);


    private double currentValue = 0.0;
    private Double update;
    private boolean allowUpdate;

    private PropagatePreCondition propagatePreCondition;
    private FieldUpdateEvent fieldListener;

    public Field() {
        this.propagatePreCondition = (cv, nv, u) -> !Utils.belowTolerance(u);
    }

    public Field(FieldUpdateEvent fieldListener) {
        this();
        this.fieldListener = fieldListener;
    }

    public PropagatePreCondition getPropagatePreCondition() {
        return propagatePreCondition;
    }

    public void setPropagatePreCondition(PropagatePreCondition propagatePreCondition) {
        this.propagatePreCondition = propagatePreCondition;
    }

    public FieldUpdateEvent getFieldListener() {
        return fieldListener;
    }

    public void setFieldListener(FieldUpdateEvent fieldListener) {
        this.fieldListener = fieldListener;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    @Override
    public double getNewValue(boolean ack) {
        double r;
        if(updateAvailable())
            r = currentValue + update;
        else
            r = currentValue;

        if(ack)
            acknowledgePropagated();

        return r;
    }

    public void setInitialValue(double v) {
        currentValue = v;
        update = null;
    }

    public boolean set(double v) {
        if(!propagatePreCondition.check(currentValue, v, v - currentValue))
            return false;

        update = v - currentValue;

        return true;
    }

    public boolean add(double u) {
        if(!propagatePreCondition.check(currentValue, currentValue + u, u))
            return false;

        if(update == null)
            update = u;
        else
            update += u;

        return true;
    }


    public void triggerUpdate() {
        triggerInternal();
    }

    protected void triggerInternal() {
        if (fieldListener == null)
            return;

        allowUpdate = true;
        fieldListener.updated(update);
        acknowledgePropagated();

        allowUpdate = false;
    }


    public boolean updateAvailable() {
        return updateAvailable(1);
    }

    public boolean updateAvailable(int updateArg) {
        return update != null;
    }

    public double getUpdate(int updateArg, boolean ack) {

        if(!allowUpdate)
            log.warn("field is not allowed to retrieve update value");

        double r = update;
        if(ack)
            acknowledgePropagated();

        return r;
    }

    public void acknowledgePropagated() {
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
        StringBuilder sb = new StringBuilder();
        sb.append("[u:");
        if(update != null)
            sb.append(Utils.round(update));
        else sb.append("-");
        sb.append(",v:");
        sb.append(Utils.round(currentValue));
        sb.append("]");
        return sb.toString();
    }
}