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
package network.aika.steps.thought;

import network.aika.Thought;
import network.aika.elements.activations.Timestamp;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.steps.keys.DocQueueKey;
import network.aika.text.Document;

import static network.aika.steps.Phase.CLOSE;

/**
 *
 * @author Lukas Molzberger
 */
public class CloseStep extends Step<Thought> {

    public static void add(Thought t) {
        add(new CloseStep(t));
    }

    public CloseStep(Thought element) {
        super(element);
    }

    @Override
    public void createQueueKey(Timestamp timestamp) {
        queueKey = new DocQueueKey(getPhase(), timestamp);
    }

    @Override
    public void process() {
        getElement().setIsOpen(0.0);
    }

    @Override
    public Phase getPhase() {
        return CLOSE;
    }

    @Override
    public String toString() {
        return ((Document)getElement()).getContent();
    }
}
