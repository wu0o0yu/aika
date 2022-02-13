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
package network.aika.steps.activation;

import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.steps.*;

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.steps.LinkingOrder.PROPAGATE;


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
public class Propagate extends Step<Activation> {

    public static void add(Activation act, boolean template) {
        if(template && !act.getConfig().isTemplatesEnabled())
            return;

        Step.add(new Propagate(act, template));
    }

    private boolean template;

    private Propagate(Activation act, boolean template) {
        super(act);

        this.template = template;
    }

    @Override
    public Phase getPhase() {
        return Phase.PROCESSING;
    }

    @Override
    public LinkingOrder getLinkingOrder() {
        return PROPAGATE;
    }

    @Override
    public void process() {
        Activation act = getElement();

        if(!act.checkAllowPropagate())
            return;

        Neuron<?, ?> n = act.getNeuron();
        n.getTargetSynapses(true, template)
                .filter(s ->
                        s.allowPropagate() &&
                                !act.linkExists(OUTPUT, s, template)
                )
                .forEach(s ->
                        propagate(act, s)
                );
    }

    public static void propagate(Activation fromAct, Synapse targetSynapse) {
        Thought t = fromAct.getThought();

        Activation toAct = targetSynapse.getOutput().createActivation(t);
        toAct.init(targetSynapse, fromAct);

        targetSynapse.createLink(fromAct, toAct);
    }

    public String toString() {
        return PROPAGATE + " " + (template ? "Template " : "") + getElement();
    }
}
