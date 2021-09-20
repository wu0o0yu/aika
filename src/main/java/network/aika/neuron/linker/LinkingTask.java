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
package network.aika.neuron.linker;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.steps.link.InformationGainGradient;
import network.aika.neuron.steps.link.Linking;
import network.aika.neuron.steps.link.PropagateGradientAndUpdateWeight;

import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;

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
    public boolean checkPropagate(Activation act, Direction dir, Synapse targetSynapse) {
        return true;
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

    public static void addNextLinkerSteps(Link l) {
        Linking.add(l);

        if(!l.getConfig().isEnableTraining())
            return;

        InformationGainGradient.add(l);
        PropagateGradientAndUpdateWeight.add(l, l.getOutput().getOutputGradientSum());
    }

    @Override
    public void linkIntern(Activation iAct, Activation oAct, Synapse targetSynapse) {
        if (!iAct.isFired())
            return;

 //           return dir.getSynapses(act.getNeuron(), invertRecurrent);

        if(Link.linkExists(targetSynapse, iAct, oAct))
            return;

        oAct = targetSynapse.branchIfNecessary(iAct, oAct);

        if(oAct != null)
            targetSynapse.createLink(iAct, oAct, oAct.isSelfRef(iAct));
    }
}
