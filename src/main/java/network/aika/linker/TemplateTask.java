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
import network.aika.steps.activation.Induction;
import network.aika.steps.link.*;

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
    public boolean checkPropagate(Activation act) {
        return act.getNeuron().allowTemplatePropagate(act);
    }

    public Neuron getPropagateTargetNeuron(Synapse targetSynapse, Activation act) {
        return targetSynapse.getTemplatePropagateTargetNeuron(act);
    }

    @Override
    protected boolean exists(Activation act, Direction dir, Synapse s) {
        return act.templateLinkExists(dir, s);
    }


    @Override
    public void addNextStepsToQueue(Activation act) {
        Induction.add(act);
    }

    @Override
    public void addNextStepsToQueue(Link l) {
        PropagateBindingSignal.add(l);
        LinkInduction.add(l);
        InformationGainGradient.add(l);
    }
}
