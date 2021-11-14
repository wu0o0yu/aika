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
package network.aika.neuron.activation.fields;

import network.aika.Model;
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Lukas Molzberger
 */
public class Field implements FieldInput, FieldOutput, Writable {

    private double oldValue = 0.0;
    private Double update;

    private FieldUpdateEvent fieldListener;

    public Field() {
    }

    public Field(FieldUpdateEvent fieldListener) {
        this.fieldListener = fieldListener;
    }

    public Field(double v) {
        this.oldValue = v;
        if(!Utils.belowTolerance(v))
            update = v;
    }

    public Field(double v, FieldUpdateEvent fieldListener) {
        this(v);
        this.fieldListener = fieldListener;
    }

    public double getOldValue() {
        return oldValue;
    }

    @Override
    public double getNewValue() {
        if(updateAvailable())
            return oldValue + update;
        else
            return oldValue;
    }

    public void set(double v) {
        if(Utils.belowTolerance( v - oldValue))
            return;

        update = v - oldValue;
    }


    public void add(double u) {
        if(Utils.belowTolerance(u))
            return;

        update += u;
    }


    public void triggerUpdate() {
        triggerInternal();
    }

    protected void triggerInternal() {
        if (fieldListener == null)
            return;

        fieldListener.updated(update, getNewValue());
    }

    public boolean updateAvailable() {
        return update != null;
    }

    public double getUpdate() {
        return update;
    }

    public void acknowledgePropagated() {
        oldValue += update;
        update = null;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(oldValue);
        out.writeBoolean(update != null);
        if(update != null)
            out.writeDouble(update);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        oldValue = in.readDouble();
        update = null;
        if(in.readBoolean())
            update = in.readDouble();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[d:");
        if(update != null)
            sb.append(Utils.round(update));
        else sb.append("X");
        sb.append(",v:");
        sb.append(Utils.round(oldValue));
        sb.append("]");
        return sb.toString();
    }
}
