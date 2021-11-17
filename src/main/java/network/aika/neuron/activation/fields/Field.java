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

    public void setFieldListener(FieldUpdateEvent fieldListener) {
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

    public boolean set(double v) {
        if(Utils.belowTolerance( v - oldValue))
            return false;

        update = v - oldValue;

        return true;
    }


    public boolean add(double u) {
        if(Utils.belowTolerance(u))
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

        fieldListener.updated();
    }

    public boolean updateAvailable() {
        return update != null;
    }

    public double getUpdate() {
        return update;
    }

    public void acknowledgePropagated() {
        if(update == null)
            return;

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
        sb.append("[u:");
        if(update != null)
            sb.append(Utils.round(update));
        else sb.append("-");
        sb.append(",v:");
        sb.append(Utils.round(oldValue));
        sb.append("]");
        return sb.toString();
    }
}
