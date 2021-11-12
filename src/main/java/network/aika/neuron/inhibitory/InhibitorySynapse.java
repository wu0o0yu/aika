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
package network.aika.neuron.inhibitory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import static network.aika.neuron.sign.Sign.POS;


/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse<I extends Neuron> extends Synapse<I, InhibitoryNeuron, Activation> {

    @Override
    public boolean checkTemplatePropagate(Activation iAct) {
        return true;
    }

    @Override
    public boolean allowLinking(Activation bindingSignal) {
        return false;
    }

    @Override
    public boolean isWeak() {
        return weight.getOldValue() + getOutput().getBias().getOldValue() < 0.0;
    }

    public Neuron getTemplatePropagateTargetNeuron(Activation<?> act) {
/*
        List<Activation<?>> candidates = act.getPatternBindingSignals().entrySet().stream()
                .map(e -> e.getKey())
                .flatMap(bAct -> bAct.getReverseBindingSignals().entrySet().stream())
                .map(e -> e.getKey())
                .filter(relAct -> relAct.getNeuron() instanceof InhibitoryNeuron)
                .collect(Collectors.toList());
*/
        return getOutput();
    }

    public void updateSynapse(Link l, double delta) {
        if(!l.getInput().isFired())
            return;

        addWeight(delta);
        l.getOutput().getNet().addAndTriggerUpdate(delta * l.getInputValue(POS));
    }

    @Override
    public Activation branchIfNecessary(Activation iAct, Activation oAct) {
        return oAct;
    }

    @Override
    public boolean checkCausality(Activation<?> iAct, Activation<?> oAct) {
        return true;
    }

    @Override
    public void setModified() {
        getInput().setModified();
    }

    @Override
    public void linkOutput() {
        if(!isTemplate())
            return;

        super.linkOutput();
    }
}
