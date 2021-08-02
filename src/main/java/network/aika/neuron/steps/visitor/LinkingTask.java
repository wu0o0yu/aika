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
package network.aika.neuron.steps.visitor;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.steps.VisitorStep;

import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.steps.link.LinkStep.LINKING;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class LinkingTask extends VisitorStep {

    public LinkingTask(Direction dir) {
        super(dir);
    }

    @Override
    public Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir, boolean invertRecurrent) {
         return dir.getSynapses(act.getNeuron(), invertRecurrent);
    }

    @Override
    public boolean checkPropagate(Activation act, Synapse targetSynapse) {
        return true;
    }

    @Override
    protected boolean exists(Activation act, Synapse s) {
        return act.outputLinkExists(s);
    }

    @Override
    protected boolean opposingNeuronMatches(Neuron<?> currentN, Neuron<?> targetN) {
        return currentN.getId() == targetN.getId();
    }

    @Override
    public void getNextSteps(Activation act) {
    }

    @Override
    public void getNextSteps(Link l) {
        QueueEntry.add(l, LINKING);
    }

    @Override
    public void closeLoopIntern(ActVisitor v, Activation iAct, Activation oAct) {
        if (!iAct.isActive(false))
            return;

        if(!(v.getCurrentDir() == OUTPUT || targetSynapse.isRecurrent()))
            return;

        if (Link.linkExists(targetSynapse, iAct, oAct))
            return;

        oAct = targetSynapse.branchIfNecessary(oAct, v);

        if(oAct != null)
            targetSynapse.closeLoop(this, v, iAct, oAct);
    }
}
