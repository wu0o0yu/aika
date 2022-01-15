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

import network.aika.direction.Direction;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.PatternBindingSignal;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class BindingNeuronSynapse<I extends Neuron, IA extends Activation> extends ExcitatorySynapse<I, BindingNeuron, IA, BindingActivation> {

    @Override
    public boolean checkBindingSignal(BindingSignal fromBS, Direction dir) {
        return fromBS instanceof PatternBindingSignal;
    }

    @Override
    public boolean propagateValue(Link<IA, BindingActivation> l) {
        return !isRecurrent() ||
                l.isForward();
    }

    @Override
    public void updateSynapse(Link l, double delta) {
        super.updateSynapse(l, delta);

        if(isRecurrent() && !l.getInput().isFired()) {
            getOutput().getFinalBias().addAndTriggerUpdate(delta);
        }
    }

    @Override
    public boolean checkCausalityAndBranchConsistency(Activation<?> iAct, Activation<?> oAct) {
        return Link.isForward(iAct, oAct);
    }

    @Override
    public BindingActivation branchIfNecessary(Activation iAct, BindingActivation oAct) {
        if (getOutput().isInputNeuron())
            return null;

        if(isRecurrent() && !oAct.isSelfRef(iAct))
            return null;

        return oAct;
    }
}
