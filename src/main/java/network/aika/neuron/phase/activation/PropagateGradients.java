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
package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.phase.Ranked;
import network.aika.neuron.phase.RankedImpl;
import network.aika.neuron.phase.link.LinkPhase;
import network.aika.neuron.phase.link.PropagateGradient;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.phase.link.LinkPhase.PROPAGATE_GRADIENT_RANK;

/**
 *
 * @author Lukas Molzberger
 */
public class PropagateGradients extends RankedImpl implements ActivationPhase {

    @Override
    public Ranked getPreviousRank() {
        return PROPAGATE_GRADIENT_RANK;
    }

    @Override
    public void process(Activation act) {
        if(act.gradientIsZero())
            return;

        double gradient = act.getAndResetGradient();

        act.addLinksToQueue(
                INPUT,
                !act.getNeuron().isInputNeuron() ? new PropagateGradient(gradient) : null,
                LinkPhase.TEMPLATE
        );

        act.getThought().addToQueue(
                act,
                TEMPLATE_INPUT,
                TEMPLATE_OUTPUT
        );
    }

    public String toString() {
        return "Propagate Gradients";
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return act2.getFired().compareTo(act1.getFired());
    }
}
