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
package network.aika.neuron.activation;

import network.aika.neuron.steps.Step;

import java.util.Comparator;

/**
 *
 * @author Lukas Molzberger
 */
public class QueueEntry<S extends Step, E extends Element> {

    public static final Comparator<QueueEntry> COMPARATOR = Comparator
            .<QueueEntry>comparingInt(qe -> qe.step.getPhase().ordinal())
            .thenComparing(qe -> qe.fired)
            .thenComparing(qe -> qe.timestamp);

    private S step;
    private E element;

    private Fired fired;
    private long timestamp;

    public QueueEntry(S step, E element) {
        this.step = step;
        this.element = element;
        this.fired = element.getFired();
    }

    public QueueEntry(S step, E element, long timestamp) {
        this.step = step;
        this.element = element;
        this.fired = element.getFired();
        this.timestamp = timestamp;
    }

    public static <S extends Step, E extends Element> void add(E e, S s) {
        if(s.checkIfQueued() && e.isQueued(s))
            return;

        QueueEntry qe = new QueueEntry(s, e);
        e.addQueuedStep(qe);
        e.getThought().addQueueEntry(qe);
    }

    public S getStep() {
        return step;
    }

    public Fired getFired() {
        return fired;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String toString() {
        return Step.toString(getStep()) + " : " + element.toString();
    }

    public Element getElement() {
        return element;
    }

    public void process() {
        step.process(element);
    }
}
