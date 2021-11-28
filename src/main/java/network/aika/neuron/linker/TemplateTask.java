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

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.steps.activation.Induction;
import network.aika.neuron.steps.link.*;

import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class TemplateTask extends AbstractLinker {

    @Override
    public Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir) {
        Stream<Neuron<?, ?>> s = act.getNeuron().getTemplateGroup().stream();
        return s.flatMap(dir::getSynapses);
    }

    @Override
    public boolean checkPropagate(Activation act, Synapse targetSynapse) {
        return targetSynapse.checkTemplatePropagate(act);
    }

    public Neuron getPropagateTargetNeuron(Synapse targetSynapse, Activation act) {
        return targetSynapse.getTemplatePropagateTargetNeuron(act);
    }

    @Override
    protected boolean exists(Activation act, Direction dir, Synapse s) {
        return act.templateLinkExists(dir, s);
    }

    @Override
    protected boolean neuronMatches(Neuron<?, ?> currentN, Neuron<?, ?> targetN) {
        return currentN.getTemplateGroup().stream()
                .anyMatch(tn ->
                        tn.getId().intValue() == targetN.getId().intValue()
                );
    }

    @Override
    public void getNextSteps(Activation act) {
        Induction.add(act);
    }

    @Override
    public void getNextSteps(Link l) {
        PropagateBindingSignal.add(l);
        LinkInduction.add(l);

        if(!l.getConfig().isEnableTraining())
            return;

        InformationGainGradient.add(l);
//        PropagateGradientAndUpdateWeight.add(l, l.getOutput().getOutputGradientSum());
    }

    @Override
    public Link createLink(Activation iAct, Activation oAct, Synapse targetSynapse) {
        if(oAct.getNeuron().isInputNeuron())
            return null;

        if(!targetSynapse.checkTemplateLink(iAct, oAct))
            return null;

        if(!iAct.isFired())
            return null;

        if(Link.templateLinkExists(targetSynapse, iAct, oAct))
            return null;

        return targetSynapse.createLink(iAct, oAct, oAct.isSelfRef(iAct));
    }
}
