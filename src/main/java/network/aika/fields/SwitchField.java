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

import java.util.function.BooleanSupplier;

/**
 * @author Lukas Molzberger
 */
public class SwitchField extends AbstractBiFunction implements FieldInput {

    private BooleanSupplier test;

    public SwitchField(String label, FieldInterface in1, boolean register1, FieldInterface in2, boolean register2, BooleanSupplier test) {
        super(label, in1, register1, in2, register2);
        this.test = test;
    }

    @Override
    public void propagateInitialValue() {
        propagateUpdate(getCurrentValue());
    }

    private FieldInterface getField() {
        if(test.getAsBoolean()) {
            return (FieldInterface) in2;
        } else {
            return (FieldInterface) in1;
        }
    }

    private FieldInterface getOtherField() {
        if(test.getAsBoolean()) {
            return (FieldInterface) in1;
        } else {
            return (FieldInterface) in2;
        }
    }

    @Override
    public boolean set(double v) {
        return getField().set(v);
    }

    @Override
    public boolean add(double u) {
        return getField().add(u);
    }

    @Override
    public void triggerUpdate() {
        getField().triggerUpdate();
    }

    @Override
    public double getCurrentValue() {
        return FieldOutput.getCurrentValue(in1) + FieldOutput.getCurrentValue(in2);
    }

    @Override
    public double getNewValue() {
        return getField().getNewValue() + FieldOutput.getCurrentValue(getOtherField());
    }

    @Override
    public boolean updateAvailable() {
        return getField().updateAvailable();
    }

    @Override
    public double getUpdate() {
        return FieldOutput.getUpdate(getField());
    }

    @Override
    public String toString() {
        if(!isInitialized())
            return "--";

        return "[v:" + getCurrentValue() + "]";
    }
}
