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

import network.aika.neuron.activation.Element;
import network.aika.utils.Utils;

import java.util.List;

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractFunction extends FieldNode implements FieldInput, FieldOutput {

    protected FieldLink input;
    private String label;

    public AbstractFunction(String label) {
        this.label = label;
    }

    public void addInput(FieldLink in) {
        this.input = in;
    }

    public void removeInput(FieldLink l) {
        this.input = null;
    }

    @Override
    public List<FieldLink> getInputs() {
        return List.of(input);
    }

    @Override
    public Element getReference() {
        return input.getInput().getReference();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if(input != null) {
            input.getInput().removeOutput(input, false);
            input = null;
        }
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean isInitialized() {
        return input.getInput().isInitialized();
    }

    @Override
    public double getCurrentValue() {
        return applyFunction(input.getInput().getCurrentValue());
    }

    protected abstract double applyFunction(double x);

    @Override
    public void receiveUpdate(int arg, double inputCV, double u) {
        if(isInitialized()) {
            double ownCV = applyFunction(inputCV);
            propagateUpdate(
                    ownCV,
                    computeUpdate(inputCV, ownCV, u)
            );
        }
    }

    private double computeUpdate(double inputCV, double ownCV, double u) {
        return input.getInput().isInitialized() ?
                computeNewValue(inputCV, u) - ownCV :
                computeNewValue(inputCV, u);
    }

    private double computeNewValue(double inputCV, double u) {
        return applyFunction(inputCV + u);
    }

    @Override
    public String toString() {
        return getLabel() + ":" + getValueString();
    }

    public String getValueString() {
        if(!isInitialized())
            return "--";

        return "[v:" + Utils.round(getCurrentValue()) + "]";
    }
}
