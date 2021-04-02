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
package network.aika.neuron.phase.activation;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.phase.VisitorPhase;
import network.aika.neuron.phase.link.LinkPhase;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Visitor.Transition.ACT;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 * Uses the Template Network defined in the {@link network.aika.neuron.Templates} to induce new template
 * activations and links.
 *
 * @author Lukas Molzberger
 */
public class Template implements VisitorPhase, ActivationPhase {

    private Direction direction;

    public Template(Direction dir) {
        direction = dir;
    }

    @Override
    public void getNextPhases(Activation act) {
        QueueEntry.add(act, INDUCTION);
    }

    @Override
    public void getNextPhases(Link l) {
        QueueEntry.add(l, LinkPhase.TEMPLATE);
        QueueEntry.add(l, LinkPhase.INDUCTION);
    }

    @Override
    public void process(Activation act) {
        if(direction == OUTPUT) {
            act.followLinks(
                    new Visitor(
                            this,
                            act,
                            direction,
                            INPUT,
                            ACT
                    )
            );
        }

        propagate(act,
                new Visitor(
                        this,
                        act,
                        direction,
                        direction,
                        ACT
                )
        );
    }

    @Override
    public void closeCycle(Activation fromAct, Visitor v) {
        Direction dir = v.startDir;

        Activation iAct = dir.getCycleInput(fromAct, v.getOriginAct());
        Activation oAct = dir.getCycleOutput(fromAct, v.getOriginAct());

        if(oAct.getNeuron().isInputNeuron())
            return;

        if(!iAct.isActive(true))
            return;

        if (Link.synapseExists(iAct, oAct))
            return;

        Set<Neuron<?>> inputTemplates = iAct.getNeuron().getTemplates();

        oAct.getNeuron()
                .getTemplates()
                .stream()
                .flatMap(tn -> tn.getInputSynapses()) // TODO!
                .filter(ts -> inputTemplates.contains(ts.getInput()))
                .forEach(ts ->
                        ts.closeCycle(v, iAct, oAct)
                );
    }

    private void propagate(Activation act, Visitor v) {
        if (!act.getNeuron().checkGradientThreshold(act))
            return;

        Collection<Synapse> templateSynapses = act
                .getNeuron()
                .getTemplates()
                .stream()
                .flatMap(tn -> v.startDir.getSynapses(tn))
                .filter(ts -> ts.checkTemplatePropagate(v, act))
                .collect(Collectors.toList());

        v.startDir.getLinks(act)
                .forEach(l ->
                        templateSynapses.remove(l.getSynapse().getTemplate())
                );

        templateSynapses.forEach(s ->
                s.propagate(act, v)
        );
    }

    public String toString() {
        return "Act-Phase: Template-" + direction;
    }
}
