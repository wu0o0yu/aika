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
package network.aika.neuron.steps.tasks;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.steps.Linker;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.link.Linking;

import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class LinkingTask extends Linker {

    public LinkingTask(Direction dir) {
        super(dir);
    }

    @Override
    public Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir) {
        return dir.getSynapses(act.getNeuron());
    }

    @Override
    public Stream<? extends Synapse> getTemplateTargetSynapses(Activation act, Direction dir) {
         return super.getTemplateTargetSynapses(act, dir); // TODO: filter non existing non-template synapses
    }

    @Override
    public boolean checkPropagate(Activation act, Synapse targetSynapse) {
        return true;
    }

    @Override
    protected boolean exists(Activation act, Synapse s) {
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
    }

    @Override
    public void closeLoopIntern(ActVisitor v, Activation iAct, Activation oAct) {
        if (!iAct.isActive(false))
            return;

        if(!(v.getCurrentDir() == OUTPUT || targetSynapse.isRecurrent()))
            return;

 //           return dir.getSynapses(act.getNeuron(), invertRecurrent);
        Synapse cs = targetSynapse.getConcreteSynapse(iAct.getNeuron(), oAct.getNeuron());
        if(cs == null)
            return;

        if(Link.linkExists(cs, iAct, oAct))
            return;

        oAct = cs.branchIfNecessary(oAct, v);

        if(oAct != null)
            cs.closeLoop(this, v, iAct, oAct);
    }

    public String toString() {
        return "Linking Task: (Target-Synapse:" + targetSynapse + ")";
    }
}
