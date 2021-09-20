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
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.steps.activation.SetFinalMode;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class BindingNeuronSynapse<I extends Neuron> extends ExcitatorySynapse<I, BindingNeuron, BindingActivation> {

    @Override
    public void updateOutputNet(Link<BindingActivation> l, double delta) {
        if(
                isRecurrent() &&
                !isNegative() &&
                !l.getOutput().isFinalMode() &&
                !l.isForward()
        )
            return;

        super.updateOutputNet(l, delta);
        SetFinalMode.add(l.getOutput());
    }

    @Override
    public boolean checkCausality(Activation<?> iAct, Activation<?> oAct) {
        return Link.isForward(iAct, oAct);
    }

    @Override
    public void updateReference(Link l) {
        if(l.getInput() == null)
            return;

        Reference iRef = l.getInput().getReference();
        if(iRef == null)
            return;

        // TODO: find a better solution.
        Synapse ts = l.getSynapse().getTemplate();
        Templates t = getModel().getTemplates();
        if(ts != t.RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE && ts != t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE) {
            l.getOutput().propagateReference(iRef);
        }
    }

    @Override
    public BindingActivation branchIfNecessary(Activation iAct, BindingActivation oAct) {
        if (getOutput().isInputNeuron())
            return null;

        if(isRecurrent() && !oAct.isSelfRef(iAct))
            return null;

        return oAct;
    }

    @Override
    public Reference getReference(Link l) {
        return (
                isRecurrent() ?
                        l.getOutput() :
                        l.getInput()
        ).getReference();
    }

    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);

        getOutput().setModified(true);
    }
}
