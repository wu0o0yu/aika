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
package network.aika.neuron.steps;

import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.Timestamp;

import java.util.Comparator;

import static network.aika.neuron.activation.Timestamp.NOT_SET;


/**
 * @author Lukas Molzberger
 */
public abstract class Step<E extends Element> implements QueueKey, Cloneable {

    public static final Comparator<Step> COMPARATOR = Comparator
            .<Step>comparingInt(s -> s.getPhase().ordinal())
            .thenComparingInt(s -> s.fired == NOT_SET ? 0 : 1)
            .thenComparing(s -> s.fired)
            .thenComparing(s -> s.timestamp);

    private E element;

    protected Timestamp fired;
    private Timestamp timestamp;

    public Step(E element) {
        this.element = element;
        this.fired = element.getFired();
        this.timestamp = NOT_SET;
    }

    public Step(E element, Timestamp timestamp) {
        this.element = element;
        this.fired = element.getFired();
        this.timestamp = timestamp;
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

    public abstract StepType getStepType();

    public abstract boolean checkIfQueued();

    public static void add(Step s) {
        Element e = s.getElement();
        if(s.checkIfQueued() && e.isQueued(s))
            return;

        e.addQueuedStep(s);
        e.getThought().addStep(s);
    }

    public Timestamp getFired() {
        return fired;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimeStamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public E getElement() {
        return element;
    }
}
