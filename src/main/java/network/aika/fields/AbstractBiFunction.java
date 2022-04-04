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
public abstract class AbstractBiFunction extends FieldListener implements FieldOutput {
    protected FieldOutput in1;
    protected FieldOutput in2;

    private String label;

    public AbstractBiFunction(String label, FieldOutput in1, FieldOutput in2) {
        this.label = label;
        this.in1 = in1;
        this.in2 = in2;
    }

    protected void registerInputListener() {
        in1.addFieldListener("arg 1", (l, u) ->
                triggerUpdate(1, u)
        );

        in2.addFieldListener("arg 2", (l, u) ->
                triggerUpdate(2, u)
        );
    }

    @Override
    public boolean isInitialized() {
        return in1 != null && in1.isInitialized() &&
                in2 != null && in2.isInitialized();
    }

    @Override
    public void propagateInitialValue(FieldUpdateEvent listener) {
        if(isInitialized())
            propagateUpdate(listener, getCurrentValue());
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void triggerUpdate(int arg, double u) {
        if(isInitialized())
            propagateUpdate(computeUpdate(arg, u));
    }

    protected abstract double computeUpdate(int arg, double u);

    public String toString() {
        if(!isInitialized())
            return "--";

        return "[v:" + Utils.round(getCurrentValue()) + "]";
    }
}
