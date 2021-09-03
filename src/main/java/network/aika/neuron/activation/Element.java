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

import network.aika.Config;
import network.aika.Thought;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.QueueKey;

import java.util.*;
import java.util.stream.Stream;

/**
 * An Element is either a node (Activation) or an edge (Link) in the Activation graph.
 *
 *  @author Lukas Molzberger
 */
public abstract class Element<E extends Element> implements Comparable<E> {

    private NavigableMap<QueueKey, Step> queuedPhases = new TreeMap<>(
            Comparator
                    .<QueueKey, String>comparing(s -> s.getStepName())
                    .thenComparing(s -> s.getTimeStamp())
    );

    public abstract Fired getFired();

    public void addQueuedStep(Step s) {
        queuedPhases.put(s, s);
    }

    public boolean isQueued(Step s) {
        return !queuedPhases.subMap(
                new QueueKey.DummyStep(s, Long.MIN_VALUE),
                new QueueKey.DummyStep(s, Long.MAX_VALUE)
        ).isEmpty();
    }

    public void removeQueuedPhase(Step s) {
        queuedPhases.remove(s);
    }

    public void replaceElement(Element newElement) {
        removeFromQueue();
        copyPhases(newElement);
        queuedPhases.clear();
    }

    public void copyPhases(Element newElement) {
        queuedPhases.values().stream().forEach(s ->
                Step.add(s)
        );
    }

    public Stream<Step> getQueuedEntries() {
        return queuedPhases.values().stream();
    }

    private void removeFromQueue() {
        getThought().removeQueueEntries(queuedPhases.values());
    }

    public abstract Thought getThought();

    public abstract Config getConfig();

    public abstract String toShortString();
}
