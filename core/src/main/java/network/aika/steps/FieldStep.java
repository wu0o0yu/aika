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
package network.aika.steps;

import network.aika.Thought;
import network.aika.elements.activations.Timestamp;
import network.aika.fields.IQueueField;
import network.aika.elements.Element;
import network.aika.steps.keys.FieldQueueKey;
import network.aika.utils.Utils;

import static network.aika.steps.keys.FieldQueueKey.SORT_VALUE_PRECISION;
import static network.aika.utils.Utils.TOLERANCE;
import static network.aika.utils.Utils.doubleToString;

/**
 *
 * @author Lukas Molzberger
 */
public class FieldStep<E extends Element> extends Step<E> {

    private IQueueField field;

    private Phase phase;

    private int round;

    private int sortValue = Integer.MAX_VALUE;

    private double delta = 0.0;


    public FieldStep(E e, Phase p, int round, IQueueField qf) {
        super(e);
        this.phase = p;
        this.round = round;

        this.field = qf;
    }

    private void updateSortValue(double newSortValue) {
        if(Utils.belowTolerance(TOLERANCE, sortValue - newSortValue))
            return;

        if(isQueued()) {
            Element ref = (Element) field.getReference();
            Thought t = ref.getThought();
            t.removeStep(this);
            sortValue = convertSortValue(newSortValue);
            t.addStep(this);
        } else
            sortValue = convertSortValue(newSortValue);
    }

    private int convertSortValue(double newSortValue) {
        return (int) (SORT_VALUE_PRECISION * newSortValue);
    }

    public int getSortValue() {
        return sortValue;
    }

    public void updateDelta(double delta) {
        this.delta += delta;

        updateSortValue(Math.abs(delta));
    }

    public void reset() {
        delta = 0.0;
    }

    public void createQueueKey(Timestamp timestamp) {
        queueKey = new FieldQueueKey(getPhase(), round, sortValue, timestamp);
    }

    @Override
    public void process() {
        field.process(this);
    }

    public int getRound() {
        return round;
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

    public double getDelta() {
        return delta;
    }

    public IQueueField getField() {
        return field;
    }

    public String toShortString() {
        return " Round:" + round +
                " Delta:" + doubleToString(delta);
    }

    public String toString() {
        return "Phase:" + phase +
                " Round:" + round +
                " Delta:" + doubleToString(delta) +
                " Field: " + field +
                " Ref:" + field.getReference();
    }
}
