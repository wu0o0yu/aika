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
package network.aika.steps.link;

import network.aika.neuron.activation.Link;
import network.aika.steps.Phase;
import network.aika.steps.Step;

/**
 *
 * @author Lukas Molzberger
 */
public class AddLink extends Step<Link> {

    public static void add(Link l) {
        add(new AddLink(l));
    }

    private AddLink(Link l) {
        super(l);
    }

    @Override
    public Phase getPhase() {
        return Phase.INIT;
    }

    @Override
    public boolean checkIfQueued() {
        return false;
    }

    @Override
    public void process() {
        Link l = getElement();

        if(l.getInput() != null) {
            l.linkInput();
            PropagateBindingSignal.add(l);
        }

        if(l.getOutput() != null)
            l.linkOutput();

        l.getOutput().getNet().addAndTriggerUpdate(
                l.getWeightedInput().getCurrentValue()
        );
    }

    public String toString() {
        return "Link-Step: Add Link " + getElement().toShortString();
    }
}
