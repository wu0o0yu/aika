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
package network.aika.neuron.phase;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lukas Molzberger
 */
public class LinkingPhase extends Phase {
    private static final Logger log = LoggerFactory.getLogger(LinkingPhase.class);

    public LinkingPhase(boolean isFinal) {
        super(isFinal);
    }

    @Override
    public void tryToLink(Activation iAct, Activation oAct, Visitor c) {
        if(!iAct.isActive()) return;

        Synapse s = oAct.getNeuron()
                .getInputSynapse(
                        iAct.getNeuronProvider()
                );

        if (s == null ||
                s.getOutput().isInputNeuron() ||
                (s.isRecurrent() && !c.getSelfRef()) ||
                iAct.outputLinkExists(oAct)
        ) return;

        if (s.isNegative() && !c.getSelfRef()) {
            oAct = oAct.createBranch(s);
        }

        Link ol = oAct.getInputLink(s);
        if (ol != null) {
//                    oAct = oAct.cloneToReplaceLink(s);
            log.warn("Link already exists!  " + oAct.getThought());
            return;
        }

        Link.link(s, iAct, oAct, c.getSelfRef());
    }

    @Override
    public void propagate(Activation act) {
        act.getNeuron().getOutputSynapses()
                .filter(s -> !act.outputLinkExists(s))
                .forEach(s ->
                        Link.link(
                                s,
                                act,
                                act.createActivation(s.getOutput()),
                                false
                        )
                );
    }

}
