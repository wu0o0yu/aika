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
package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.neuron.phase.activation.ActivationPhase.LINK_AND_PROPAGATE;


/**
 * Uses the visitor to link neighbouring links to the same output activation.
 *
 * @author Lukas Molzberger
 */
public class Linking implements LinkPhase {

    @Override
    public void process(Link l) {
        l.follow(LINK_AND_PROPAGATE);

        QueueEntry.add(l, MAX_VALUE, COUNTING);
    }

    public String toString() {
        return "Link-Phase: Linking";
    }
}
