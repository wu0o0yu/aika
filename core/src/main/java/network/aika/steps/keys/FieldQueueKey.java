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


/**
 * @author Lukas Molzberger
 */
public class FieldQueueKey implements QueueKey {

    public static final double SORT_VALUE_PRECISION = 1000.0;


    private Phase phase;

    private int sortValue;
    private Timestamp currentTimestamp;

    public FieldQueueKey(Phase phase, int sortValue, Timestamp currentTimestamp) {
        this.phase = phase;
        this.sortValue = sortValue;
        this.currentTimestamp = currentTimestamp;
    }

    public Phase getPhase() {
        return phase;
    }

    public int getSortValue() {
        return sortValue;
    }

    public Timestamp getCurrentTimestamp() {
        return currentTimestamp;
    }


    @Override
    public int compareTo(QueueKey qk) {
        return Integer.compare(
                ((FieldQueueKey) qk).sortValue,
                sortValue
        );
    }

    public String toString() {
        String svStr = getSortValue() == Integer.MAX_VALUE ?
                "MAX" :
                "" + getSortValue();

        return "[p:" + getPhase() + "-" + getPhase().ordinal() +
                ",sv:" + svStr +
                ",ts:" + getCurrentTimestamp() +
                "]";
    }
}
