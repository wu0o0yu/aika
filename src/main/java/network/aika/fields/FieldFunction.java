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

import java.util.function.DoubleFunction;

/**
 * @author Lukas Molzberger
 */
public class FieldFunction extends FieldListener implements FieldOutput {

    private FieldOutput input;
    private DoubleFunction<Double> function;
    private String label;

    public FieldFunction(String label, FieldOutput in, DoubleFunction<Double> f) {
        this.input = in;
        this.function = f;
        this.label = label;

        this.input.addFieldListener(label, (l, u) ->
                triggerUpdate()
        );
    }

    public FieldFunction(String label, FieldOutput in, DoubleFunction<Double> f, FieldInput... out) {
        this(label, in, f);

        for (FieldInput o : out)
            addFieldListener(label, (l, u) ->
                    o.addAndTriggerUpdate(u)
            );
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
    public void propagateInitialValue() {
        if(isInitialized())
            propagateUpdate(getCurrentValue());
    }

    private void triggerUpdate() {
        if (!updateAvailable())
            return;

        propagateUpdate(
                getUpdate()
        );
    }

    @Override
    public double getCurrentValue() {
        return function.apply(input.getCurrentValue());
    }

    @Override
    public double getNewValue() {
        return function.apply(input.getNewValue());
    }

    @Override
    public boolean updateAvailable() {
        return input.updateAvailable();
    }

    @Override
    public double getUpdate() {
        return input.isInitialized() ?
                getNewValue() - getCurrentValue() :
                getNewValue();
    }

    @Override
    public String toString() {
        if(!isInitialized())
            return "--";

        return "[v:" + Utils.round(getCurrentValue()) + "]";
    }
}
