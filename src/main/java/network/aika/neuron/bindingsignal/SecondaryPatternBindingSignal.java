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
package network.aika.neuron.bindingsignal;

import network.aika.direction.Direction;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;

/**
 * @author Lukas Molzberger
 */
public class SecondaryPatternBindingSignal extends PatternBindingSignal {

    public SecondaryPatternBindingSignal(PatternActivation act) {
        super(act);
    }

    protected SecondaryPatternBindingSignal(PatternBindingSignal parent, Activation act, boolean scopeTransition) {
        super(parent, act, scopeTransition);
    }

    public SecondaryPatternBindingSignal next(Activation act, boolean scopeTransition) {
        return nextSecondary(act, scopeTransition);
    }

    public String toString() {
        return "[SECONDARY-PATTERN:" + getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ",s:" + scope + ",d:" + getDepth() + "]";
    }
}
