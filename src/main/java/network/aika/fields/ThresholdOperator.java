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
public class ThresholdOperator extends FieldListener implements FieldOutput {

    private double threshold;
    private FieldOutput input;
    private String label;

    public ThresholdOperator(String label, double threshold, FieldOutput in) {
        this.label = label;
        this.threshold = threshold;
        this.input = in;
        this.input.addFieldListener(label, (l, u) ->
                triggerUpdate()
        );
    }

    public ThresholdOperator(String label, double threshold, FieldOutput in, FieldInput... out) {
        this(label, threshold, in);

        for (FieldInput o : out)
            addFieldListener(label, (l, u) ->
                    o.setAndTriggerUpdate(getNewValue())
            );
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
        return checkThreshold(input.getCurrentValue());
    }

    @Override
    public double getNewValue() {
        return checkThreshold(input.getNewValue());
    }

    @Override
    public double getUpdate() {
        return input.isInitialized() ?
                getNewValue() - getCurrentValue() :
                getNewValue();
    }

    private double checkThreshold(double x) {
        return x > threshold ? 1.0 : 0.0;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean updateAvailable() {
        return input.updateAvailable();
    }

    @Override
    public boolean isInitialized() {
        return input.isInitialized();
    }

    @Override
    public void propagateInitialValue() {
        input.propagateInitialValue();
    }

    @Override
    public String toString() {
        if(!isInitialized())
            return "--";

        return "[v:" + Utils.round(getCurrentValue()) + "]";
    }
}
