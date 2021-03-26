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

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.phase.Ranked;
import network.aika.neuron.phase.RankedImpl;

import java.util.Comparator;

import static network.aika.neuron.activation.RoundType.*;
import static network.aika.neuron.phase.activation.ActivationPhase.*;
import static network.aika.neuron.sign.Sign.POS;

/**
 * Use the link gradient to update the synapse weight.
 *
 * @author Lukas Molzberger
 */
public class UpdateWeight extends RankedImpl implements LinkPhase {

    @Override
    public Ranked getPreviousRank() {
        return PROPAGATE_GRADIENTS_NET;
    }

    @Override
    public void process(Link l) {
        Synapse s = l.getSynapse();

        double weightDelta = s.updateSynapse(l);

        QueueEntry.add(l.getOutput(), l.getRound(WEIGHT), UPDATE_SYNAPSE_INPUT_LINKS);
        QueueEntry.add(l, l.getRound(WEIGHT), new SumUpLink(l.getInputValue(POS) * weightDelta));
    }

    public String toString() {
        return "Link-Phase: Update Weight";
    }

    @Override
    public Comparator<Link> getElementComparator() {
        return Comparator.naturalOrder();
    }
}
