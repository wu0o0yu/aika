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
package network.aika.linker;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.direction.Direction;
import network.aika.steps.link.InformationGainGradient;
import network.aika.steps.link.PropagateBindingSignal;

import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class LinkingTask extends AbstractLinker {


    @Override
    public Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir) {
        return dir.getSynapses(act.getNeuron());
    }

    @Override
    public boolean checkPropagate(Activation act, Synapse targetSynapse) {
        return !targetSynapse.isNegative();
    }

    public Neuron getPropagateTargetNeuron(Synapse targetSynapse, Activation act) {
        return targetSynapse.getOutput();
    }

    @Override
    protected boolean exists(Activation act, Direction dir, Synapse s) {
        return !act.getOutputLinks(s).isEmpty();
    }

    @Override
    public void getNextSteps(Activation act) {
    }

    @Override
    public void getNextSteps(Link l) {
        addNextLinkerSteps(l);
    }

    @Override
    protected boolean neuronMatches(Neuron<?, ?> currentN, Neuron<?, ?> targetN) {
        return currentN.getId().intValue() == targetN.getId().intValue();
    }

    public static void addNextLinkerSteps(Link l) {
        PropagateBindingSignal.add(l);

        if(!l.getConfig().isEnableTraining())
            return;

        InformationGainGradient.add(l);
    }

    @Override
    public Link createLink(Activation iAct, Activation oAct, Synapse targetSynapse) {
        if (!iAct.isFired())
            return null;

        oAct = targetSynapse.branchIfNecessary(iAct, oAct);
        if(oAct == null)
            return null;

        return oAct.addLink(targetSynapse, iAct);
    }
}
