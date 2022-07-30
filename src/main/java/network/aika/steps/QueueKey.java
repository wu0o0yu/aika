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

import network.aika.neuron.activation.Timestamp;

import java.util.Comparator;

import static network.aika.neuron.activation.Timestamp.NOT_SET;

/**
 * @author Lukas Molzberger
 */
public interface QueueKey {

    Comparator<QueueKey> COMPARATOR = (k1, k2) -> {
        int r = Integer.compare(k1.getPhase().ordinal(), k2.getPhase().ordinal());
        if(r != 0)
            return r;

        r = compareBothFired(k1, k2);
        if(r != 0)
            return r;

        if(firedBeforeCreated(k1, k2))
            return 1;

        if(firedBeforeCreated(k2, k1))
            return -1;

        r = Boolean.compare(
                k1.getFired() != NOT_SET,
                k2.getFired() != NOT_SET
        );
        if(r != 0)
            return r;

        r = compareBothNotFired(k1, k2);
        if(r != 0)
            return r;

        return k1.getCurrentTimestamp().compareTo(k2.getCurrentTimestamp());
    };

    private static int compareBothFired(QueueKey k1, QueueKey k2) {
        if(k1.getFired() == NOT_SET || k2.getFired() == NOT_SET)
            return 0;

        return k1.getFired().compareTo(k2.getFired());
    }

    private static boolean firedBeforeCreated(QueueKey k1, QueueKey k2) {
        return k1.getFired() == NOT_SET &&
                k2.getFired() != NOT_SET &&
                k1.getCreated().compareTo(k2.getFired()) > 0;
    }

    private static int compareBothNotFired(QueueKey k1, QueueKey k2) {
        if(k1.getFired() != NOT_SET || k2.getFired() != NOT_SET)
            return 0;

        return Integer.compare(k2.getSortValue(), k1.getSortValue());
    }

    String getStepName();

    Phase getPhase();

    Timestamp getFired();

    Timestamp getCreated();

    int getSortValue();

    Timestamp getCurrentTimestamp();

    default String qkToString() {
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
