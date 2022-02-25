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
public abstract class BiFunction extends FieldListener implements FieldOutput {
    protected FieldOutput in1;
    protected FieldOutput in2;

    protected int currentArgument = -1;
    private String label;

    public BiFunction(String label, FieldOutput in1, boolean register1, FieldOutput in2, boolean register2) {
        this.label = label;
        this.in1 = in1;
        this.in2 = in2;

        if (register1)
            in1.addFieldListener(label + "-arg(1)", (l, u) ->
                    triggerUpdate(1)
            );

        if (register2)
            in2.addFieldListener(label + "-arg(2)", (l, u) ->
                    triggerUpdate(2)
            );
    }

    public BiFunction(String label, FieldOutput in1, boolean register1, FieldOutput in2, boolean register2, FieldInput out) {
        this(label, in1, register1, in2, register2);

        addFieldListener(out.getLabel(), (l, u) ->
                out.addAndTriggerUpdate(u)
        );
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void triggerUpdate(int arg) {
        currentArgument = arg;
        if (!updateAvailable())
            return;

        propagateUpdate(
                getUpdate()
        );
        currentArgument = -1;
    }

    @Override
    public boolean updateAvailable() {
        switch (currentArgument) {
            case 1:
                return !Utils.belowTolerance(FieldOutput.getCurrentValue(in2)) && in1.updateAvailable();
            case 2:
                return !Utils.belowTolerance(FieldOutput.getCurrentValue(in1)) && in2.updateAvailable();
            default:
                throw new IllegalArgumentException();
        }
    }

    public String toString() {
        return "[v:" + getCurrentValue() + "]";
    }
}
