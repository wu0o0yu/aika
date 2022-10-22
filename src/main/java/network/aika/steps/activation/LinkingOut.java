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
package network.aika.steps.activation;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import static network.aika.steps.Phase.OUTPUT_LINKING;

/**
 *
 * @author Lukas Molzberger
 */
public class LinkingOut extends Step<Activation> {

    private Synapse synapse;

    public static void add(Activation act, Synapse s) {
        Step.add(new LinkingOut(act, s));
    }

    public LinkingOut(Activation act, Synapse s) {
        super(act);
        this.synapse = s;
    }

    @Override
    public void process() {
       synapse.linkAndPropagateOut(getElement());
    }

    @Override
    public Phase getPhase() {
        return OUTPUT_LINKING;
    }
}
