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

import static network.aika.neuron.activation.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public class Linking implements Phase {
    private static final Logger log = LoggerFactory.getLogger(Linking.class);

    @Override
    public void process(Activation act) {
        act.getThought().linkInputRelations(act);
        act.updateValueAndPropagate();
    }

    @Override
    public Phase nextPhase() {
        return FINAL_LINKING;
    }

    public boolean isFinal() {
        return false;
    }

    @Override
    public void tryToLink(Activation iAct, Activation oAct, Visitor v) {
        if(!iAct.isActive()) {
            return;
        }

        Synapse s = oAct.getNeuron()
                .getInputSynapse(
                        iAct.getNeuronProvider()
                );

        if(s == null || iAct.outputLinkExists(oAct)) {
            return;
        }

        oAct = s.getOutputActivationToLink(oAct, v);
        if (oAct == null) {
            return;
        }

        Link ol = oAct.getInputLink(s);
        if (ol != null) {
//                    oAct = oAct.cloneToReplaceLink(s);
            log.warn("Link already exists!  " + oAct.getThought());
            return;
        }

        Link.link(s, iAct, oAct, v.getSelfRef());
    }


    @Override
    public void propagate(Activation act) {
        act.getModel().linkInputRelations(act, OUTPUT);
        act.getThought().processLinks();

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

    @Override
    public int getRank() {
        return 0;
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return act1.getFired().compareTo(act2.getFired());
    }
}
