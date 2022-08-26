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

import network.aika.neuron.Synapse;

import java.util.Comparator;

/**
 * @author Lukas Molzberger
 */
public class LinkSlot extends Field<Synapse> implements FieldInput, FieldOutput {

    private FieldLink defaultInput;
    private FieldLink selectedInput;

    public LinkSlot(Synapse ref, String label) {
        super(ref, label);
    }

    public FieldLink getDefaultInput() {
        return defaultInput;
    }

    public FieldLink getSelectedInput() {
        return selectedInput;
    }

    public void setDefaultInput(FieldLink defaultInput) {
        this.defaultInput = defaultInput;
    }

    public void receiveUpdate(FieldLink fl, double u) {
        computeUpdate(fl);
        triggerUpdate();
    }

    private void computeUpdate(FieldLink fl) {
        double inputNV = fl.getInput().getNewValue();

        if(selectedInput == null || selectedInput == defaultInput) {
            selectedInput = fl;
            newValue = inputNV;
            return;
        }

        double outputOV = selectedInput.getCurrentInputValue();

        FieldLink maxFL = inputNV >= outputOV ?
            fl :
            getInputs().stream()
                    .filter(in -> in != defaultInput)
                    .max(Comparator.comparingDouble(in -> in.getCurrentInputValue()))
                    .orElse(null);

        selectedInput = maxFL;
        newValue = maxFL == fl ?
                inputNV :
                maxFL.getCurrentInputValue();
    }
}
