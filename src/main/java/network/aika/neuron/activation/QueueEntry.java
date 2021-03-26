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

import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.neuron.phase.link.LinkPhase;

/**
 *
 * @author Lukas Molzberger
 */
public class QueueEntry<P extends Phase, E extends Element> implements Comparable<QueueEntry> {

    private int round;
    private P phase;
    private E element;
    private long addedTimestamp;
    private long currentTimestamp;

    public QueueEntry(int round, P phase, E element) {
        this.round = round;
        this.phase = phase;
        this.element = element;
    }

    public static void add(Activation act, int round, ActivationPhase p) {
        addIntern(act, round, p);
    }

    public static void add(Link l, int round, LinkPhase p) {
        addIntern(l, round, p);
    }

    private static <P extends Phase, E extends Element> void addIntern(E e, int round, P p) {
        QueueEntry qe = new QueueEntry(round, p, e);
        e.addQueuedPhase(qe);
        e.getThought().addQueueEntry(qe);
    }

    public int getRound() {
        return round;
    }

    public P getPhase() {
        return phase;
    }

    public long getAddedTimestamp() {
        return addedTimestamp;
    }

    public void setAddedTimestamp(long addedTimestamp) {
        this.addedTimestamp = addedTimestamp;
    }

    public long getCurrentTimestamp() {
        return currentTimestamp;
    }

    public void setCurrentTimestamp(long currentTimestamp) {
        this.currentTimestamp = currentTimestamp;
    }

    public String toString() {
        return Phase.toString(getPhase()) + " : " + element.toString();
    }

    public String pendingPhasesToString() {
        StringBuilder sb = new StringBuilder();
 //       pendingPhases.forEach(p -> sb.append(p.toString() + ", "));

        return sb.substring(0, Math.max(0, sb.length() - 2));
    }

    public Element getElement() {
        return element;
    }

    public void process() {
        phase.process(element);
    }

    @Override
    public int compareTo(QueueEntry qe) {
        int r = Integer.compare(round, qe.getRound());
        if(r != 0) return r;
        r = Integer.compare(getPhase().getRank(), qe.getPhase().getRank());
        if(r != 0) return r;
        r = getPhase().compare(element, qe.getElement());
        if(r != 0) return r;
        return element.compareTo(qe.getElement());
    }
}
