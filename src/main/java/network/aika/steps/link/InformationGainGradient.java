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
 * Computes the gradient of the information gain function for this activation.
 *
 * @see <a href="https://aika.network/training.html">Aika Training</a>
 *
 * @author Lukas Molzberger
 */
public class InformationGainGradient extends Step<Link> {

    public static void add(Link l) {
        if(!l.getConfig().isTrainingEnabled())
            return;

        if(l.isNegative())
            return; // TODO: Check under which conditions negative synapses could contribute to the cost function.

        Step.add(new InformationGainGradient(l));
    }

    private InformationGainGradient(Link l) {
        super(l);
    }

    @Override
    public Phase getPhase() {
        return Phase.LATE;
    }

    @Override
    public void process() {
        getElement().updateInformationGainGradient();
    }

    public String toString() {
        return "Link-Step: Information-Gain Gradient " + getElement().toShortString();
    }
}
