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
import network.aika.neuron.activation.Link;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.PBSType;
import network.aika.neuron.bindingsignal.PatternBindingSignal;

/**
 *
 * @author Lukas Molzberger
 */
public class SameBNSynapse<I extends Neuron> extends BindingNeuronSynapse<I> {

    public PatternBindingSignal propagatePatternBindingSignal(Link l, PatternBindingSignal iBS) {
        if(iBS.getScope() > 0)
            return null;

        return new PatternBindingSignal(iBS, l.getOutput(), null, iBS.getScope());
    }

    public boolean checkRelatedPatternBindingSignal(PatternBindingSignal iBS, PatternBindingSignal oBS) {
        return iBS.getScope() == 1 && iBS.getType() == PBSType.PRIMARY &&
                oBS.getScope() == 2 && oBS.getType() == PBSType.SECONDARY;
    }
}
