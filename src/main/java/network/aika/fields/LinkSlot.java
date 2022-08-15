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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Element;
import network.aika.utils.Utils;

import java.util.*;

/**
 * @author Lukas Molzberger
 */
public class LinkSlot extends FieldNode<Activation> implements FieldInput, FieldOutput {

    protected Map<FieldLink, Double> inputs = new TreeMap<>(Comparator.comparingInt(fl -> fl.getArgument()));

    private FieldLink defaultInput;
    private FieldLink selectedInput;

    public LinkSlot(Activation ref, String label) {
        super(ref, label);
    }

    public FieldLink getDefaultInput() {
        return defaultInput;
    }

    public void setDefaultInput(FieldLink defaultInput) {
        this.defaultInput = defaultInput;
    }

    @Override
    public Collection<FieldLink> getInputs() {
        return inputs.keySet();
    }

    @Override
    public int getNextArg() {
        return inputs.size();
    }

    @Override
    public void addInput(FieldLink fl) {
        FieldOutput input = fl.getInput();

        inputs.put(fl, input.isInitialized() ? input.getCurrentValue() : null);
    }

    @Override
    public void removeInput(FieldLink l) {
        inputs.remove(l);
    }

    @Override
    public void disconnect() {
        super.disconnect();
        inputs.keySet()
                .forEach(fl -> fl.getInput().removeOutput(fl, false));
        inputs.clear();
    }

    public void receiveUpdate(FieldLink fl, double inputCV, double u) {
        inputs.put(fl, Double.valueOf(inputCV + u));

        if(isInitialized(fl)) {
            double ownCV = getCurrentValue(fl, inputCV);
            propagateUpdate(
                    ownCV,
                    computeUpdate(fl, inputCV, ownCV, u)
            );
        }
        selectedInput = getMaxInput();
    }

    private boolean isInitialized(FieldLink fl) {
        return fl.getInput().isInitialized();
    }

    private double getValue(FieldLink fl) {
        if(fl == null)
            return 0.0;

        Double v = inputs.get(fl);
        if(v == null)
            return 0.0;

        return v;
    }

    protected double getCurrentValue(FieldLink fl, double inputCV) {
        FieldLink maxInput = getMaxInput();
        if(maxInput == null)
            return 0.0;

        return getValue(maxInput);
    }

    protected double computeUpdate(FieldLink fl, double inputCV, double ownCV, double u) {
        FieldLink maxInput = getMaxInput();
        if(maxInput == null || maxInput != fl)
            return 0.0;

        double newValue = getValue(maxInput);
        double oldValue = getValue(selectedInput);

        return newValue - oldValue;
    }

    private FieldLink getMaxInput() {
        return inputs.entrySet().stream()
                .filter(e -> e.getKey() != defaultInput)
                .filter(e -> e.getValue() != null)
                .max(Comparator.comparingDouble(e -> e.getValue()))
                .map(e -> e.getKey())
                .orElse(null);
    }

    @Override
    public double getCurrentValue() {
        return 0;
    }

    @Override
    public boolean isInitialized() {
        return selectedInput != null ?
                isInitialized(selectedInput) :
                false;
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
