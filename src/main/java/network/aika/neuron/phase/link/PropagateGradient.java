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

import network.aika.Utils;
import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.RankedImpl;

/**
 *
 * @author Lukas Molzberger
 */
public class PropagateGradient extends RankedImpl implements LinkPhase {

    private double gradient;

    public PropagateGradient(int rank, double gradient) {
        super(rank);
        this.gradient = gradient;
    }

    @Override
    public void process(Link l) {
        l.propagateGradient(gradient);
    }

    public String toString() {
        return "Link-Propagate Gradient (" + Utils.round(gradient) + ")";
    }

    @Override
    public int compare(Link l1, Link l2) {
        return 0;
    }
}
