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

import network.aika.FieldObject;

import java.util.Comparator;

/**
 * @author Lukas Molzberger
 */
public class MinMaxField extends SumField {

    MinMax mode;
    private AbstractFieldLink selectedInput;

    public MinMaxField(FieldObject ref, MinMax m, String label) {
        super(ref, label, null);
        mode = m;
    }

    public AbstractFieldLink getSelectedInput() {
        return selectedInput;
    }

    public MinMax getMode() {
        return mode;
    }

    @Override
    public void receiveUpdate(AbstractFieldLink fl, double u) {
        computeUpdate(fl);
        triggerUpdate();
    }

    private void computeUpdate(AbstractFieldLink fl) {
        if(selectedInput == null) {
            selectedInput = fl;
            newValue = fl.getInput().getNewValue();
            return;
        }

        selectedInput = getInputs().stream()
                .max(getComparator())
                .orElse(null);

        newValue = selectedInput.getCurrentInputValue();
    }

    private Comparator<FieldLink> getComparator() {
        return switch (mode) {
            case MAX -> Comparator.comparingDouble(in -> in.getCurrentInputValue());
            case MIN -> Comparator.comparingDouble(in -> -in.getCurrentInputValue());
        };
    }
}
