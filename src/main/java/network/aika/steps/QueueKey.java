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

import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.Timestamp;

import java.util.Comparator;

import static network.aika.neuron.activation.Timestamp.FIRED_COMPARATOR;
import static network.aika.neuron.activation.Timestamp.NOT_SET;

/**
 * @author Lukas Molzberger
 */
public class QueueKey {

    public static Comparator<QueueKey> COMPARATOR = (k1, k2) -> {
        int r = Integer.compare(k1.getPhase().ordinal(), k2.getPhase().ordinal());
        if(r != 0)
            return r;

        r = FIRED_COMPARATOR.compare(k1.fired, k2.fired);
        if (r != 0)
            return r;

//        if(k1.getFired() == NOT_SET || k2.getFired() == NOT_SET) {
        r = Integer.compare(k2.getSortValue(), k1.getSortValue());
        if (r != 0)
            return r;
//        }

        r = k1.created.compareTo(k2.created);
        if (r != 0)
            return r;

        return k1.getCurrentTimestamp().compareTo(k2.getCurrentTimestamp());
    };

    private Phase phase;
    private Timestamp created;
    private Timestamp fired;
    private int sortValue;
    private Timestamp currentTimestamp;

    public QueueKey(Phase phase, Element element, int sortValue, Timestamp currentTimestamp) {
        this.phase = phase;
        this.created = element.getCreated();
        this.fired = element.getFired();
        this.sortValue = sortValue;
        this.currentTimestamp = currentTimestamp;
    }

    public QueueKey(Phase phase, Long fired, long created, int sortValue, long currentTimestamp) {
        this.phase = phase;
        this.fired = fired != null ? new Timestamp(fired) : NOT_SET;
        this.created = new Timestamp(created);
        this.sortValue = sortValue;
        this.currentTimestamp = new Timestamp(currentTimestamp);
    }

    public Phase getPhase() {
        return phase;
    }

    public Timestamp getFired() {
        return fired;
    }

    public Timestamp getCreated() {
        return created;
    }

    public int getSortValue() {
        return sortValue;
    }

    public Timestamp getCurrentTimestamp() {
        return currentTimestamp;
    }

    public String toString() {
        String firedStr = getFired() == NOT_SET ?
                "NOT_FIRED" : "" +
                getFired();

        String svStr = getSortValue() == Integer.MAX_VALUE ?
                "MAX" :
                "" + getSortValue();

        return "[p:" + getPhase() + "-" + getPhase().ordinal() +
                ",f:" + firedStr +
                ",c:" + getCreated() +
                ",sv:" + svStr +
                ",ts:" + getCurrentTimestamp() +
                "]";
    }
}
