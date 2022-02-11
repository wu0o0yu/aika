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

    Comparator<QueueKey> COMPARATOR = Comparator
            .<QueueKey>comparingInt(k -> k.getPhase().ordinal())
            .thenComparing(k -> k.getFired())
            .thenComparing(k -> k.getLinkingOrder());

    Comparator<QueueKey> THOUGHT_COMPARATOR = COMPARATOR
            .thenComparing(k -> k.getTimestamp());

    Comparator<QueueKey> ELEMENT_COMPARATOR = COMPARATOR
            .thenComparing(k -> k.getStepName())  // Needed to check if the entry is already on the queue.
            .thenComparing(k -> k.getCheckIfQueuedTimestamp());

    default Timestamp getCheckIfQueuedTimestamp() {
        return checkIfQueued() ? NOT_SET : getTimestamp();
    }

    Phase getPhase();

    Timestamp getFired();

    LinkingOrder getLinkingOrder();

    String getStepName();

    Timestamp getTimestamp();

    boolean checkIfQueued();

    class Key implements QueueKey {
        private final Phase p;

        public Key(Phase p) {
            this.p = p;
        }

        @Override
        public Phase getPhase() {
            return p;
        }

        @Override
        public Timestamp getFired() {
            return Timestamp.MAX;
        }

        @Override
        public LinkingOrder getLinkingOrder() {
            return LinkingOrder.NOT_SET;
        }

        @Override
        public String getStepName() {
            return "";
        }

        @Override
        public Timestamp getTimestamp() {
            return NOT_SET;
        }

        @Override
        public boolean checkIfQueued() {
            return true;
        }
    }
}
