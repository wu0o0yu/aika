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


import network.aika.utils.Utils;

/**
 * @author Lukas Molzberger
 */
public class InvertFunction extends FieldListener implements FieldOutput {

    FieldOutput input;
    private String label;

    public InvertFunction(String label, FieldOutput in) {
        this.label = label;
        this.input = in;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean isInitialized() {
        return input.isInitialized();
    }

    @Override
    public void propagateInitialValue(FieldUpdateEvent listener) {
        if(isInitialized())
            propagateUpdate(listener, getCurrentValue());
    }

    @Override
    public double getCurrentValue() {
        return 1.0 - input.getCurrentValue();
    }

    @Override
    public double getNewValue() {
        return 1.0 - input.getNewValue();
    }

    @Override
    public boolean updateAvailable() {
        return input.updateAvailable();
    }

    @Override
    public double getUpdate() {
        return getNewValue() - getCurrentValue();
    }

    @Override
    public String toString() {
        if(!isInitialized())
            return "--";

        return "[v:" + Utils.round(getCurrentValue()) + "]";
    }
}