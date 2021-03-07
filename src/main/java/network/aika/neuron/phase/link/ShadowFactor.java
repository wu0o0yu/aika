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
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.Ranked;
import network.aika.neuron.phase.RankedImpl;

import static network.aika.neuron.activation.Element.RoundType.ACT;
import static network.aika.neuron.phase.activation.ActivationPhase.DETERMINE_BRANCH_PROBABILITY;

/**
 * Avoid that synapses which access the same source information generate twice the gradient.
 *
 * @author Lukas Molzberger
 */
public class ShadowFactor extends RankedImpl implements LinkPhase {

    @Override
    public Ranked getPreviousRank() {
        return DETERMINE_BRANCH_PROBABILITY;
    }

    @Override
    public void process(Link l) {
    //    l.removeGradientDependencies();
    }

    public String toString() {
        return "Link-Phase: Shadow Factor";
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
