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

/**
 *
 * @author Lukas Molzberger
 */
public class PropagateOutputGradient implements LinkPhase {

    @Override
    public void process(Link l) {
        l.getOutput().addInputGradient(l.getOutputGradient());
    }

    @Override
    public int getRank() {
        return 11;
    }

    @Override
    public int compare(Link o1, Link o2) {
        return 0;
    }
}
