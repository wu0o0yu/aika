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
public abstract class AbstractBiFunction extends FieldNode implements FieldInput, FieldOutput {
    protected FieldLink in1;
    protected FieldLink in2;

    private String label;

    public AbstractBiFunction(String label) {
        this.label = label;
    }

    public void addInput(FieldLink l) {
        if(l.getArgument() == 1) {
            in1 = l;
        } else {
            in2 = l;
        }
    }

    @Override
    public void removeInput(FieldLink l) {
        if(l.getArgument() == 1) {
            in1 = null;
        } else {
            in2 = null;
        }
    }

    private boolean isInitialized(int arg) {
        FieldLink in = arg == 1 ? in2 : in1;

        return in != null && in.getInput().isInitialized();
    }

    @Override
    public boolean isInitialized() {
        return in1 != null && in1.getInput().isInitialized() &&
                in2 != null && in2.getInput().isInitialized();
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void receiveUpdate(int arg, double u) {
        if(isInitialized(arg))
            propagateUpdate(computeUpdate(arg, u));
    }

    protected abstract double computeUpdate(int arg, double u);

    public String toString() {
        if(!isInitialized())
            return "--";

        return "[v:" + Utils.round(getCurrentValue()) + "]";
    }
}
