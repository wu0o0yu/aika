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
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.link.LinkPhase;
import network.aika.neuron.phase.link.PropagateGradient;

import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.activation.direction.Direction.INPUT;

/**
 * An Element is either a node (Activation) or an edge (Link) in the Activation graph.
 *
 *  @author Lukas Molzberger
 */
public abstract class Element implements Comparable<Element> {

    private int round;

    private Set<QueueEntry> queuedPhases = new TreeSet<>();

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

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
        queuedPhases.stream()
                .map(qe ->
                        new QueueEntry(qe.getRound(), qe.getPhase(), newElement)
                )
                .forEach(qe ->
                        getThought().addQueueEntry(qe)
                );
    }

    private void removeFromQueue() {
        getThought().removeQueueEntries(queuedPhases);
    }

    public abstract void onProcessEvent(Phase p);

    public abstract void afterProcessEvent(Phase p);

    public abstract Thought getThought();

    public abstract String toShortString();

}
