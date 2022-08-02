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


/**
 * @author Lukas Molzberger
 */
public abstract class Step<E extends Element> implements Cloneable {

    private E element;
    private QueueKey queueKey;
    private int sortValue = Integer.MAX_VALUE;


    public Step(E element) {
        this.element = element;
    }

    public boolean isQueued() {
        return queueKey != null;
    }

    public QueueKey getQueueKey() {
        return queueKey;
    }

    public void createQueueKey(Timestamp timestamp) {
        queueKey = new QueueKey(getPhase(), element, sortValue, timestamp);
    }

    public void removeQueueKey() {
        queueKey = null;
    }

    public void updateSortValue(double newSortValue) {
        if(Utils.belowTolerance(sortValue - newSortValue))
            return;

        if(isQueued()) {
            Thought t = getElement().getThought();
            t.removeStep(this);
            sortValue = convertSortValue(newSortValue);
            t.addStep(this);
        } else
            sortValue = convertSortValue(newSortValue);
    }

    private int convertSortValue(double newSortValue) {
        return (int) (1000.0 * newSortValue);
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

    public static boolean add(Step s) {
        Thought t = s.getElement().getThought();
        if(t == null)
            return false;

        t.addStep(s);
        return true;
    }

    public E getElement() {
        return element;
    }

    public String toString() {
        return "" + getElement();
    }
}
