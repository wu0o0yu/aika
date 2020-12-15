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

import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.phase.RankedImpl;
import network.aika.neuron.phase.VisitorPhase;
import network.aika.neuron.phase.link.LinkPhase;

import java.util.Set;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Template extends RankedImpl implements VisitorPhase, ActivationPhase {

    public Template(int rank) {
        super(rank);
    }

    @Override
    public ActivationPhase[] getNextActivationPhases(Config c) {
        return new ActivationPhase[] {
                PREPARE_FINAL_LINKING,
                SOFTMAX,
                COUNTING,
//                TEMPLATE,
                INDUCTION,
                PROPAGATE_GRADIENT,
                UPDATE_SYNAPSE_INPUT_LINKS,
                FINAL
        };
    }

    @Override
    public LinkPhase[] getNextLinkPhases(Config c) {
        return new LinkPhase[] {
                LinkPhase.SELF_GRADIENT,
                LinkPhase.SHADOW_FACTOR,
                LinkPhase.INDUCTION,
                LinkPhase.UPDATE_WEIGHTS
        };
    }

    @Override
    public void process(Activation act) {
        act.followLinks(
                new Visitor(this, act, INPUT)
        );

        propagate(act,
                new Visitor(this, act, INPUT)
        );
        propagate(act,
                new Visitor(this, act, OUTPUT)
        );
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public void tryToLink(Activation act, Visitor v) {
        Activation iAct = v.startDir == INPUT ? act : v.origin;
        Activation oAct = v.startDir == OUTPUT ? act : v.origin;

        Neuron n = oAct.getNeuron();

        if(!iAct.isActive() || n.isInputNeuron())
            return;

        if (n.getInputSynapse(iAct.getNeuronProvider()) != null)
            return;

        oAct.getNeuron()
                .getTemplates()
                .stream()
                .flatMap(tn -> tn.getInputSynapses())
                .filter(ts -> ts.checkTemplate(iAct, oAct, v))
                .filter(s -> iAct.getNeuron().getTemplates().contains(s.getInput()))
                .forEach(s ->
                        s.transition(v, act, v.origin, true)
                );
    }

    @Override
    public void propagate(Activation act, Visitor v) {
        if (act.gradientSumIsZero())
            return;

        if (!act.getNeuron().checkTemplate(act)) {
            return;
        }

        Set<Synapse> templateSynapses = act
                .getNeuron()
                .getTemplates()
                .stream()
                .flatMap(tn -> v.startDir.getSynapses(tn))
                .filter(ts -> ts.checkTemplatePropagate(v, act))
                .collect(Collectors.toSet());

        v.startDir.getLinks(act)
                .forEach(l ->
                        templateSynapses.remove(l.getSynapse().getTemplate())
                );

        templateSynapses.forEach(s ->
                s.transition(v, act, null, true)
        );
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return 0;
    }
}
