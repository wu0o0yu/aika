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

import network.aika.Thought;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * An Element is either a node (Activation) or an edge (Link) in the Activation graph.
 *
 *  @author Lukas Molzberger
 */
public abstract class Element<E extends Element> implements Comparable<E> {

    Comparator<Element> COMPARE = Comparator.
            <Element>comparingInt(e -> e.getElementType())
            .thenComparing(e -> e);

    private Set<QueueEntry> queuedPhases = new TreeSet<>(QueueEntry.COMPARATOR);

    protected abstract int getElementType();

    public abstract Fired getFired();

    public void addQueuedPhase(QueueEntry qe) {
        queuedPhases.add(qe);
    }

    public void removeQueuedPhase(QueueEntry qe) {
        queuedPhases.remove(qe);
    }

    public void replaceElement(Element newElement) {
        removeFromQueue();
        copyPhases(newElement);
        queuedPhases.clear();
    }

    public void copyPhases(Element newElement) {
        queuedPhases.stream().forEach(qe ->
                QueueEntry.add(newElement, qe.getPhase())
        );
    }

    private void removeFromQueue() {
        getThought().removeQueueEntries(queuedPhases);
    }

    public abstract Thought getThought();

    public abstract String toShortString();
}
