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

import network.aika.Thought;
import network.aika.Utils;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.RankedImpl;
import network.aika.neuron.phase.activation.ActivationPhase;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.phase.activation.ActivationPhase.*;

/**
 *
 * @author Lukas Molzberger
 */
public class SumUpLink extends RankedImpl implements LinkPhase {

    private double delta;

    public SumUpLink(double delta) {
        super(LINKING);
        this.delta = delta;
    }

    @Override
    public void process(Link l) {
        Thought t = l.getThought();
        l.sumUpLink(delta);

        t.addToQueue(l, INFORMATION_GAIN_GRADIENT);
        t.addToQueue(
                l.getOutput(),
                ActivationPhase.ENTROPY_GRADIENT
        );

        Activation oAct = l.getOutput();
        if(oAct.checkIfFired()) {
            t.addToQueue(
                    oAct,
                    LINK_AND_PROPAGATE,
                    PREPARE_POSITIVE_FEEDBACK_LOOP_UPDATE,
                    oAct.hasBranches() ? SOFTMAX : null,
                    ActivationPhase.COUNTING
            );
            oAct.addLinksToQueue(
                    INPUT,
                    COUNTING
            );
        }
    }

    public String toString() {
        return "Sum up Link (" + Utils.round(delta) + ")";
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
