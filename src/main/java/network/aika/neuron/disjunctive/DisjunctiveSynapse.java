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
package network.aika.neuron.disjunctive;

import network.aika.direction.Direction;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.DisjunctiveActivation;
import network.aika.neuron.activation.InhibitoryActivation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.axons.Axon;
import network.aika.neuron.axons.BindingAxon;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.sign.Sign;
import network.aika.utils.Bound;


/**
 *
 * @author Lukas Molzberger
 */
public class DisjunctiveSynapse<I extends Neuron & Axon, O extends DisjunctiveNeuron<?, OA>, IA extends Activation, OA extends DisjunctiveActivation> extends Synapse<I, O, IA, OA> {

    @Override
    public boolean checkBindingSignal(BindingSignal fromBS, Direction dir) {
        return false;
    }

    @Override
    public boolean allowPropagate() {
        return true;
    }

    @Override
    public boolean isWeak() {
        return weight.getCurrentValue() + getOutput().getBias().getCurrentValue() < 0.0;
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

    @Override
    public void updateSynapse(Link l, double delta) {
        if(!l.getInput().isFired())
            return;

        weight.addAndTriggerUpdate(delta);
    }

    @Override
    public boolean checkCausalityAndBranchConsistency(Activation<?> iAct, Activation<?> oAct) {
        return true;
    }

    @Override
    protected Bound getProbabilityBound(Sign si, Sign so) {
        return so == Sign.POS ? Bound.LOWER : Bound.UPPER;
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