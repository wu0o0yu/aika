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
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Scope;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.phase.RankedImpl;
import network.aika.neuron.phase.VisitorPhase;
import network.aika.neuron.phase.link.LinkPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public class Linking extends RankedImpl implements VisitorPhase, ActivationPhase {
    private static final Logger log = LoggerFactory.getLogger(Linking.class);

    public Linking(int rank) {
        super(rank);
    }

    @Override
    public ActivationPhase[] getNextActivationPhases(Config c) {
        return ActivationPhase.getInitialPhases(c);
    }

    @Override
    public LinkPhase[] getNextLinkPhases(Config c) {
        return LinkPhase.getInitialPhases(c);
    }

    @Override
    public void process(Activation act) {
        act.getThought().linkInputRelations(act);
        boolean hasChanged = act.updateValue();

        if(hasChanged) {
            Visitor v = new Visitor(
                    this,
                    act,
                    OUTPUT,
                    Scope.SAME,
                    Scope.INPUT
            );

//            act.getNeuron().transition(v, act, false);
            act.followLinks(v);

            act.getModel().linkInputRelations(act, OUTPUT);

            act.propagate(v);
        }
    }

    public boolean isFinal() {
        return false;
    }

    @Override
    public void tryToLink(Activation act, Visitor v) {
        Activation iAct = v.startDir.getInput(v.origin, act);
        Activation oAct = v.startDir.getOutput(v.origin, act);

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

        s.transition(v, act, v.origin, true);
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return act1.getFired().compareTo(act2.getFired());
    }
}
