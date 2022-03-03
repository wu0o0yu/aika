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

/**
 * @author Lukas Molzberger
 */
public class ConstantDoubleField extends FieldListener implements DoubleFieldOutput {

    public static final ConstantDoubleField ZERO = new ConstantDoubleField("ZERO", 0.0);
    public static final ConstantDoubleField ONE = new ConstantDoubleField("ONE", 1.0);

    private final double value;
    private boolean initialized = false;
    private String label;

    public ConstantDoubleField(String label, double value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void propagateInitialValue() {
        propagateUpdate(getCurrentValue());
    }

    @Override
    public double getCurrentValue() {
        return value;
    }

    @Override
    public double getNewValue() {
        return value;
    }

    @Override
    public boolean updateAvailable() {
        return !initialized;
    }

    @Override
    public double getUpdate() {
        return 0;
    }
}
