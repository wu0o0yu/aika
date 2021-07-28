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
import network.aika.neuron.steps.link.LinkStep;

import java.util.Set;
import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.steps.activation.ActivationStep.INDUCTION;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class TemplateTask extends VisitorStep {

    public TemplateTask(Direction dir) {
        super(dir);
    }

    @Override
    public Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir, boolean invertRecurrent) {
        return act.getNeuron()
                .getTemplateGroup().stream()
                .flatMap(tn ->
                        dir.getSynapses(tn, invertRecurrent)
                );
    }

    @Override
    public boolean checkPropagate(Activation act, Synapse targetSynapse) {
        return targetSynapse.checkTemplatePropagate(direction, act);
    }

    @Override
    protected boolean opposingNeuronMatches(Neuron<?> currentN, Neuron<?> targetN) {
        return currentN.getTemplateGroup()
                .stream()
                .anyMatch(tn -> tn.getId() == targetN.getId());
    }

    @Override
    public void getNextSteps(Activation act) {
        QueueEntry.add(act, INDUCTION);
    }

    @Override
    public void getNextSteps(Link l) {
        QueueEntry.add(l, LinkStep.TEMPLATE);
        QueueEntry.add(l, LinkStep.INDUCTION);
    }

    @Override
    public void closeLoopIntern(ActVisitor v, Activation iAct, Activation oAct) {
        if(oAct.getNeuron().isInputNeuron())
            return;

        if(v.getCurrentDir() != OUTPUT && !targetSynapse.isRecurrent())
            return;

        if(!iAct.isActive(targetSynapse.isRecurrent()))
            return;

        if(oAct.templateInputLinkExists(targetSynapse))
            return;

        targetSynapse.closeLoop(this, v, iAct, oAct);
    }
}
