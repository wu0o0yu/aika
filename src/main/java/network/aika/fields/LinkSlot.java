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

import static network.aika.fields.LinkSlotMode.MAX;

/**
 * @author Lukas Molzberger
 */
public class LinkSlot extends Field<Synapse> implements FieldInput, FieldOutput {

    LinkSlotMode mode;
    private FieldLink selectedInput;

    public LinkSlot(Synapse ref, LinkSlotMode m, String label) {
        super(ref, label);
        mode = m;
    }

    public FieldLink getSelectedInput() {
        return selectedInput;
    }

    public LinkSlotMode getMode() {
        return mode;
    }

    public void receiveUpdate(FieldLink fl, double u) {
        computeUpdate(fl);
        triggerUpdate();
    }

    private void computeUpdate(FieldLink fl) {
        double inputNV = fl.getInput().getNewValue();

        if(selectedInput == null) {
            selectedInput = fl;
            newValue = inputNV;
            return;
        }

        double outputOV = selectedInput.getCurrentInputValue();

        FieldLink mFL = compare(inputNV, outputOV) ?
            fl :
            getInputs().stream()
                    .max(getComparator())
                    .orElse(null);

        selectedInput = mFL;
        newValue = mFL == fl ?
                inputNV :
                mFL.getCurrentInputValue();
    }

    private Comparator<FieldLink> getComparator() {
        return switch (mode) {
            case MAX -> Comparator.comparingDouble(in -> in.getCurrentInputValue());
            case MIN -> Comparator.comparingDouble(in -> -in.getCurrentInputValue());
        };
    }

    private boolean compare(double inputNV, double outputOV) {
        return switch (mode) {
            case MAX -> inputNV >= outputOV;
            case MIN -> inputNV < outputOV;
        };
    }
}
