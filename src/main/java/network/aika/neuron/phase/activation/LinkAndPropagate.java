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

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.phase.Ranked;
import network.aika.neuron.phase.RankedImpl;
import network.aika.neuron.phase.VisitorPhase;
import network.aika.neuron.phase.link.LinkPhase;

import static network.aika.neuron.activation.Visitor.Transition.ACT;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.phase.link.LinkPhase.LINKING;
import static network.aika.neuron.phase.link.LinkPhase.SUM_UP_LINK_RANK;


/**
 * The job of the linking phase is to propagate information through the network by creating the required activations and links.
 * Each activation and each link have an corresponding neuron or synapse respectively. Depending on the data set in the
 * document, a neuron might have several activations associated with it. During propagation an input activation
 * causes the creating of a link in one or more output synapses and the creation of an output activation. Initially the value
 * of the input activation and the weight of the synapse might not suffice to activate the output activation. But that might
 * change later on as more input links are added to the activation. New input links are added by the closeCycle method. This
 * method is called by the visitor which follows the links in the activation network to check that both input and output
 * activation of a new link refer to the same object in the input data set.
 *
 * @author Lukas Molzberger
 */
public class LinkAndPropagate extends RankedImpl implements VisitorPhase, ActivationPhase {

    @Override
    public Ranked getPreviousRank() {
        return SUM_UP_LINK_RANK;
    }

    @Override
    public ActivationPhase[] getNextActivationPhases() {
        return new ActivationPhase[] {};
    }

    @Override
    public LinkPhase[] getNextLinkPhases() {
        return new LinkPhase[] {
                LINKING
        };
    }

    @Override
    public void process(Activation act) {
        act.getThought().linkInputRelations(act);

        if(!act.updateValue(false))
            return;

        act.followLinks(
                new Visitor(
                        this,
                        act,
                        OUTPUT,
                        INPUT,
                        ACT
                )
        );

        act.getModel().linkInputRelations(act, OUTPUT);

        propagate(act,
                new Visitor(
                        this,
                        act,
                        OUTPUT,
                        OUTPUT,
                        ACT
                )
        );
    }

    @Override
    public void closeCycle(Activation fromAct, Visitor v) {
        Direction dir = v.startDir;
        Activation iAct = dir.getCycleInput(fromAct, v.getOriginAct());
        Activation oAct = dir.getCycleOutput(fromAct, v.getOriginAct());

        if(!iAct.isActive(false))
            return;

        Synapse s = Link.getSynapse(iAct, oAct);
        if(s == null)
            return;

        if (Link.linkExists(iAct, oAct))
            return;

        oAct = s.branchIfNecessary(oAct, v);

        if(oAct != null)
            s.closeCycle(v, iAct, oAct);
    }

    public void propagate(Activation act, Visitor v) {
        act.getNeuron()
                .getOutputSynapses()
                .filter(s -> !act.outputLinkExists(s))
                .forEach(s ->
                        s.propagate(act, v)
                );
    }

    public String toString() {
        return "Act-Phase: Link and Propagate";
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return act1.getFired().compareTo(act2.getFired());
    }
}
