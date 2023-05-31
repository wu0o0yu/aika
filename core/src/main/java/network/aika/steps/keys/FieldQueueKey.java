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
package network.aika.steps.keys;

import network.aika.elements.activations.Timestamp;
import network.aika.steps.Phase;

import java.util.Comparator;
import java.util.function.Function;


/**
 * @author Lukas Molzberger
 */
public class FieldQueueKey implements QueueKey {

    public static final double SORT_VALUE_PRECISION = 1000.0;

    Comparator<FieldQueueKey> COMPARATOR = Comparator
            .<FieldQueueKey>comparingInt(k -> k.round)
            .thenComparingInt(k -> -k.sortValue);

    private Phase phase;

    private int round;

    private int sortValue;

    private Timestamp currentTimestamp;

    public FieldQueueKey(Phase phase, int round, int sortValue, Timestamp currentTimestamp) {
        this.phase = phase;
        this.round = round;
        this.sortValue = sortValue;
        this.currentTimestamp = currentTimestamp;
    }

    public Phase getPhase() {
        return phase;
    }

    public int getRound() {
        return round;
    }

    public int getSortValue() {
        return sortValue;
    }

    private String getSortValueAsString() {
        return getSortValue() == Integer.MAX_VALUE ?
                "MAX" :
                "" + getSortValue();
    }

    public Timestamp getCurrentTimestamp() {
        return currentTimestamp;
    }


    @Override
    public int compareTo(QueueKey qk) {
        return COMPARATOR.compare(this, (FieldQueueKey) qk);
    }

    public String toString() {
        return "[p:" + getPhase() + "-" + getPhase().ordinal() +
                ",r:" + round +
                ",sv:" + getSortValueAsString() +
                ",ts:" + getCurrentTimestamp() +
                "]";
    }
}