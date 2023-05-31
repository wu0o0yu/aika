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
public class MaxField extends SumField {

    private AbstractFieldLink selectedInput;

    public MaxField(FieldObject ref, String label) {
        super(ref, label, null);
    }

    public AbstractFieldLink getSelectedInput() {
        return selectedInput;
    }

    @Override
    public void receiveUpdate(AbstractFieldLink fl, double u) {
        computeUpdate(fl);
        triggerUpdate();
    }

    private void computeUpdate(AbstractFieldLink fl) {
        if(selectedInput == null) {
            selectedInput = fl;
            newValue = fl.getNewInputValue();
            return;
        }

        selectedInput = getInputs().stream()
                .max(getComparator(fl))
                .orElse(null);

        newValue = getInput(selectedInput, fl);
    }

    private Comparator<FieldLink> getComparator(AbstractFieldLink fl) {
        return Comparator.comparingDouble(in -> getInput(in, fl));
    }

    private double getInput(AbstractFieldLink fl, AbstractFieldLink updateFL) {
        return fl == updateFL ?
                fl.getNewInputValue() :
                fl.getCurrentInputValue();
    }
}
