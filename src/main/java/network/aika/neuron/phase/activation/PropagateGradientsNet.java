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
import network.aika.neuron.activation.Fired;
import network.aika.neuron.phase.RankedImpl;

import java.util.Comparator;

import static network.aika.neuron.activation.Activation.FIRED_COMPARATOR_REVERSED;

/**
 * Propagates the gradient of this activation backwards to all its input-links.
 *
 * @author Lukas Molzberger
 */
public class PropagateGradientsNet extends RankedImpl implements ActivationPhase {

    public PropagateGradientsNet() {
        super(PROPAGATE_GRADIENTS_SUM);
    }

    @Override
    public void process(Activation act) {
        act.propagateGradientsFromNetUpdate();
    }

    @Override
    public Comparator<Activation> getElementComparator() {
        return FIRED_COMPARATOR_REVERSED;
    }

    public String toString() {
        return "Act-Phase: Propagate Gradients from Net Update";
    }
}
