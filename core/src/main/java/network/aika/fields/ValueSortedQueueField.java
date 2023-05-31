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
import network.aika.Thought;
import network.aika.elements.Element;
import network.aika.steps.Phase;
import network.aika.utils.Utils;

import static network.aika.steps.keys.FieldQueueKey.SORT_VALUE_PRECISION;
import static network.aika.utils.Utils.TOLERANCE;

/**
 * @author Lukas Molzberger
 */
public class ValueSortedQueueField extends QueueSumField {

    public ValueSortedQueueField(FieldObject e, Phase p, String label, Double tolerance) {
        super(e, p, label, tolerance);
    }

    @Override
    public void triggerUpdate() {
        if(Utils.belowTolerance(tolerance, newValue - currentValue))
            return;

        updateSortValue(Math.abs(newValue - currentValue));
        super.triggerUpdate();
    }

    public void updateSortValue(double newSortValue) {
        if(Utils.belowTolerance(TOLERANCE, step.getSortValue() - newSortValue))
            return;

        if(isQueued()) {
            Element ref = (Element) getReference();
            Thought t = ref.getThought();
            t.removeStep(step);
            step.setSortValue(convertSortValue(newSortValue));
            t.addStep(step);
        } else
            step.setSortValue(convertSortValue(newSortValue));
    }

    private int convertSortValue(double newSortValue) {
        return (int) (SORT_VALUE_PRECISION * newSortValue);
    }
}
