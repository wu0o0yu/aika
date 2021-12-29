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
package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.direction.Direction;
import network.aika.neuron.activation.BindingSignal;
import network.aika.neuron.activation.Link;

/**
 * @author Lukas Molzberger
 */
public abstract class InputBNSynapse<I extends Neuron> extends BindingNeuronSynapse<I> {

    public boolean checkRelatedBindingSignal(BindingSignal iBS, BindingSignal oBS) {
        return (byte) (iBS.getScope() + 1) == oBS.getScope();
    }

    public BindingSignal propagateBindingSignal(Link l, BindingSignal iBS) {
        if(iBS.getActivation() != l.getInput() &&
                iBS.getOriginActivation().getType() == l.getInput().getType())
            return null;

        if(iBS.getScope() >= 2)
            return null;

        return new BindingSignal(iBS, l.getOutput(), (byte) (iBS.getScope() + 1));
    }
}
