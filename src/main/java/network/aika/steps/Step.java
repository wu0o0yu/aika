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
import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.Timestamp;
import network.aika.utils.Utils;

import static network.aika.neuron.activation.Timestamp.NOT_SET;


/**
 * @author Lukas Molzberger
 */
public abstract class Step<E extends Element> implements QueueKey, Cloneable {

    private E element;

    protected Timestamp created;
    protected Timestamp fired;

    protected double sortValue;

    private Timestamp currentTimestamp;

    public Step(E element) {
        this.element = element;
        this.created = element.getCreated();
        this.fired = element.getFired();
    }

    public void updateSortValue(double newSortValue, boolean isQueued) {
        if(Utils.belowTolerance(sortValue - newSortValue))
            return;

        if(isQueued) {
            Thought t = getElement().getThought();
            t.removeStep(this);
            sortValue = newSortValue;
            t.addStep(this);
        } else
            sortValue = newSortValue;
    }

    public Step copy(Element newElement) {
        Step newStep = null;
        try {
            newStep = (Step) clone();
        } catch (CloneNotSupportedException e) {
        }
        newStep.element = newElement;
        return newStep;
    }

    public String getStepName() {
        return getClass().getSimpleName();
    }

    public abstract void process();

    public abstract Phase getPhase();

    public static void add(Step s) {
        Thought t = s.getElement().getThought();
        if(t == null)
            return;

        t.addStep(s);
    }

    public double getSortValue() {
        return sortValue;
    }

    public Timestamp getCurrentTimestamp() {
        return currentTimestamp;
    }

    public void setCurrentTimestamp(Timestamp timestamp) {
        this.currentTimestamp = timestamp;
    }

    public E getElement() {
        return element;
    }

    @Override
    public Timestamp getFired() {
        return element.getFired();
    }

    @Override
    public Timestamp getCreated() {
        return element.getCreated();
    }

    public String toString() {
        return "" + getElement();
    }
}
