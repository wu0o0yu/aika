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

/**
 * During the initial linking process all positive recurrent synapses are assumed to be
 * active. If that is not the case, updates of the affected activations are required.
 *
 * @author Lukas Molzberger
 */
public class PropagateChange  implements ActivationPhase {

    private double delta;

    public PropagateChange(double delta) {
        this.delta = delta;
    }

    @Override
    public void process(Activation act) {
        act.updateOutgoingLinks(delta);
    }

    public String toString() {
        return "Act-Phase: Propagate Change";
    }
}
