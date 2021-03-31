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

import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.RoundType;
import network.aika.utils.Utils;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.RankedImpl;
import network.aika.neuron.phase.activation.ActivationPhase;

import java.util.Comparator;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.neuron.activation.RoundType.ACT;
import static network.aika.neuron.activation.RoundType.WEIGHT;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.phase.activation.ActivationPhase.*;

/**
 * Uses the input activation value, and the synapse weight to update the net value of the output activation.
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
    public void process(Link l, int round) {
        l.sumUpLink(delta);

        Activation oAct = l.getOutput();

        if(l.getSynapse().isRecurrent() && !oAct.getNeuron().isInputNeuron())
            round++;

        QueueEntry.add(oAct, round, PROPAGATE_GRADIENTS_NET);

        if(oAct.checkIfFired()) {
            QueueEntry.add(oAct, round, LINK_AND_PROPAGATE);
            QueueEntry.add(oAct, round, USE_FINAL_BIAS);

            if(oAct.hasBranches())
                    QueueEntry.add(oAct, round, DETERMINE_BRANCH_PROBABILITY);

            QueueEntry.add(oAct, MAX_VALUE, ActivationPhase.COUNTING);
            oAct.addLinksToQueue(INPUT, MAX_VALUE, COUNTING);
        }
    }

    @Override
    public Comparator<Link> getElementComparator() {
        return Comparator.naturalOrder();
    }

    public String toString() {
        return "Link-Phase: Sum up Link (" + Utils.round(delta) + ")";
    }
}
