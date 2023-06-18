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

import static network.aika.utils.Utils.roundToString;


/**
 * @author Lukas Molzberger
 */
public class FieldQueueKey extends QueueKey {

    public static final double SORT_VALUE_PRECISION = 1000.0;

    Comparator<FieldQueueKey> COMPARATOR = Comparator
            .comparingInt(k -> -k.sortValue);

    private int sortValue;


    public FieldQueueKey(int round, Phase phase, int sortValue, Timestamp currentTimestamp) {
        super(round, phase, currentTimestamp);
        this.sortValue = sortValue;
    }

    public int getSortValue() {
        return sortValue;
    }

    private String getSortValueAsString() {
        return getSortValue() == Integer.MAX_VALUE ?
                "MAX" :
                "" + getSortValue();
    }

    @Override
    public int compareTo(QueueKey qk) {
        return COMPARATOR.compare(this, (FieldQueueKey) qk);
    }

    @Override
    public String toString() {
        return "[r:" + getRoundStr() +
                ",p:" + getPhraseStr() +
                ",sv:" + getSortValueAsString() +
                ",ts:" + getCurrentTimestamp() +
                "]";
    }
}
