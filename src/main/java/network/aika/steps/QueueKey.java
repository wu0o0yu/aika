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
            .thenComparingInt(k -> k.getFired() == NOT_SET ? 0 : 1)
            .thenComparing(k -> k.getFired())
            .thenComparing(k -> k.getStepName())
            .thenComparing(k -> k.getTimestamp());

    Phase getPhase();

    Timestamp getFired();

    String getStepName();

    Timestamp getTimestamp();

    class Key implements QueueKey {
        private final Phase p;
        private final Timestamp fired;
        private final String stepName;
        private final Timestamp timestamp;

        public Key(Phase p, Timestamp fired) {
            this.p = p;
            this.fired = fired;
            this.stepName = "";
            this.timestamp = NOT_SET;
        }

        public Key(Step s, Timestamp timestamp) {
            this.p = s.getPhase();
            this.fired = s.getFired();
            this.stepName = s.getStepName();
            this.timestamp = timestamp;
        }

        @Override
        public Phase getPhase() {
            return p;
        }

        @Override
        public Timestamp getFired() {
            return fired;
        }

        @Override
        public String getStepName() {
            return stepName;
        }

        @Override
        public Timestamp getTimestamp() {
            return timestamp;
        }
    }
}
